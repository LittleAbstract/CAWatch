package com.security.careactivator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.security.careactivator.R
import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.CaInfo
import com.security.careactivator.db.CertDatabase
import com.security.careactivator.db.DetectedCaEntity
import com.security.careactivator.prefs.AppPrefs
import com.security.careactivator.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory

/**
 * Detects newly-installed user CA certificates.
 *
 * Model:
 *  1. First run: capture trusted baseline of user-installed CAs.
 *  2. Later scans: report any CA present now but not in the baseline.
 *
 * Monitoring modes (AppPrefs):
 *  - ALWAYS_ON: stay as foreground service after scan.
 *  - BOOT_AND_MANUAL: scan then stop (Play-friendlier default).
 */
class CertScannerService : Service() {

    companion object {
        private const val TAG = "CertScannerService"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1
        private const val FOREGROUND_CHANNEL_ID = "foreground_channel"
        private const val ALERT_CHANNEL_ID = "ca_detection_channel"

        const val EXTRA_ONE_SHOT = "one_shot"

        fun startScan(context: Context, oneShot: Boolean = false) {
            val intent = Intent(context, CertScannerService::class.java).apply {
                putExtra(EXTRA_ONE_SHOT, oneShot)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var db: CertDatabase
    private lateinit var prefs: AppPrefs
    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    /** When true, stop after the current scan finishes. */
    private var oneShotRequest = false

    inner class LocalBinder : Binder() {
        fun getService(): CertScannerService = this@CertScannerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        db = CertDatabase.getDatabase(this)
        prefs = AppPrefs.get(this)
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        oneShotRequest = intent?.getBooleanExtra(EXTRA_ONE_SHOT, false) == true ||
            !prefs.isAlwaysOn

        Log.d(TAG, "onStartCommand oneShot=$oneShotRequest mode=${prefs.monitoringMode}")
        scanNow()
        return if (prefs.isAlwaysOn && !oneShotRequest) START_STICKY else START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    fun scanNow() {
        serviceScope.launch { runScan() }
    }

    private suspend fun runScan() {
        Log.d(TAG, "Starting CA scan...")

        try {
            val currentCerts = getUserInstalledCertificates()
            Log.d(TAG, "Found ${currentCerts.size} user-installed certificates")

            val baseline = db.caDao().getAllBaseline()

            if (baseline.isEmpty()) {
                Log.i(TAG, "No baseline — capturing trusted baseline of ${currentCerts.size} CAs")
                db.caDao().insertBaselineAll(
                    currentCerts.map {
                        BaselineCaEntity(
                            fingerprint = it.fingerprint,
                            issuer = it.issuer,
                            subject = it.subject
                        )
                    }
                )
                Log.i(TAG, "Baseline captured.")
            } else {
                // Only alert / insert for certs that are new vs baseline AND not already logged.
                val newCerts = CaDetection.findNewCerts(
                    currentCerts = currentCerts,
                    baseline = baseline,
                    alreadyDetected = db.caDao().getAllDetected()
                )

                Log.d(TAG, "Found ${newCerts.size} newly installed certificates")

                if (newCerts.isNotEmpty()) {
                    newCerts.forEach { cert ->
                        db.caDao().insertDetected(
                            DetectedCaEntity(
                                fingerprint = cert.fingerprint,
                                issuer = cert.issuer,
                                subject = cert.subject,
                                detectedAt = System.currentTimeMillis()
                            )
                        )
                        Log.i(TAG, "New user CA detected: ${cert.issuer}")
                    }
                    showDetectionNotification(newCerts)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for user CAs", e)
        } finally {
            if (oneShotRequest || !prefs.isAlwaysOn) {
                Log.d(TAG, "One-shot / boot-and-manual mode — stopping service")
                stopForegroundCompat()
                stopSelf()
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun getUserInstalledCertificates(): List<CaInfo> {
        val certificates = mutableListOf<CaInfo>()

        try {
            val ks = KeyStore.getInstance("AndroidCAStore")
            ks.load(null)

            val aliases = try {
                ks.aliases().toList()
            } catch (e: Exception) {
                Log.w(TAG, "alias() unsupported, falling back to TrustManagerFactory", e)
                emptyList()
            }

            if (aliases.isNotEmpty()) {
                for (alias in aliases) {
                    try {
                        val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                        certificates.add(cert.toCaInfo())
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to process alias $alias", e)
                    }
                }
            } else {
                certificates.addAll(getSystemCertificates())
            }

            Log.d(TAG, "Total user certificates processed: ${certificates.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user certificates", e)
        }

        return certificates
    }

    private fun getSystemCertificates(): List<CaInfo> {
        val certificates = mutableListOf<CaInfo>()

        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)

            for (manager in trustManagerFactory.trustManagers
                .filterIsInstance<javax.net.ssl.X509TrustManager>()) {
                for (cert in manager.acceptedIssuers) {
                    try {
                        certificates.add(cert.toCaInfo())
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to process certificate", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system certificates", e)
        }

        return certificates
    }

    private fun X509Certificate.toCaInfo(): CaInfo = CaDetection.toCaInfo(this)

    private fun createForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                getString(R.string.notification_channel_foreground),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_foreground_title))
            .setContentText(getString(R.string.notification_foreground_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showDetectionNotification(certs: List<CaInfo>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val message = if (certs.size == 1) {
            getString(R.string.notification_alert_single, certs[0].issuer)
        } else {
            getString(R.string.notification_alert_multiple, certs.size)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.notification_alert_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ALERT_NOTIFICATION_ID, notification)
    }
}

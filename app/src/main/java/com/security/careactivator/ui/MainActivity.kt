package com.security.careactivator.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.security.careactivator.BuildConfig
import com.security.careactivator.R
import com.security.careactivator.billing.BillingManager
import com.security.careactivator.billing.BillingProducts
import com.security.careactivator.databinding.ActivityMainBinding
import com.security.careactivator.db.CertDatabase
import com.security.careactivator.prefs.AppPrefs
import com.security.careactivator.service.CertScannerService
import com.security.careactivator.util.ExportUtil
import com.security.careactivator.util.SettingsIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Primary UI: status, detection list, scan, export, monitoring mode.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: CertDatabase
    private lateinit var prefs: AppPrefs
    private lateinit var billing: BillingManager
    private lateinit var adapter: DetectedCaAdapter
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        prefs = AppPrefs.get(this)
        billing = BillingManager.get()
        if (!prefs.onboardingCompleted) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setTitleTextColor(getColorCompat(R.color.colorOnPrimary))

        db = CertDatabase.getDatabase(this)

        setupRecyclerView()
        loadData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            triggerManualScan()
        }

        binding.btnScanNow.setOnClickListener {
            triggerManualScan()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshEntitlement()
            loadData()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // Surface the upgrade entry even for entitiled users (manage subscription).
        menu.findItem(R.id.action_go_pro)?.isVisible = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_go_pro -> {
                showProDialog()
                true
            }
            R.id.action_export -> {
                exportReport()
                true
            }
            R.id.action_open_settings -> {
                SettingsIntents.openUserCredentials(this)
                true
            }
            R.id.action_monitoring_mode -> {
                showMonitoringModeDialog()
                true
            }
            R.id.action_clear_history -> {
                confirmClearHistory()
                true
            }
            R.id.action_reset_baseline -> {
                confirmResetBaseline()
                true
            }
            R.id.action_about -> {
                showAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = DetectedCaAdapter(emptyList()) { event ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_ID, event.id)
                putExtra(DetailActivity.EXTRA_FINGERPRINT, event.fingerprint)
                putExtra(DetailActivity.EXTRA_ISSUER, event.issuer)
                putExtra(DetailActivity.EXTRA_SUBJECT, event.subject)
                putExtra(DetailActivity.EXTRA_DETECTED_AT, event.detectedAt)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = db.caDao().getAllDetected()
                val baselineCount = db.caDao().countBaseline()
                val modeLabel = if (prefs.isAlwaysOn) {
                    if (billing.isUnlocked.value) {
                        getString(R.string.label_status_mode_always)
                    } else {
                        getString(R.string.label_status_mode_always_locked)
                    }
                } else {
                    getString(R.string.label_status_mode_boot)
                }
                val status = if (baselineCount == 0) {
                    getString(R.string.label_status_none) + "\n" + modeLabel
                } else {
                    getString(R.string.label_status_baseline, baselineCount) + "\n" + modeLabel
                }

                withContext(Dispatchers.Main) {
                    adapter.updateData(events)
                    binding.textStatus.text = status
                    val empty = events.isEmpty()
                    binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.toast_error, e.message ?: "unknown"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun triggerManualScan() {
        CertScannerService.startScan(this, oneShot = !prefs.isAlwaysOn)
        Toast.makeText(this, R.string.toast_scan_started, Toast.LENGTH_SHORT).show()
        // Refresh after the service has had time to write results.
        mainHandler.postDelayed({ loadData() }, 1200)
        mainHandler.postDelayed({ loadData() }, 3000)
    }

    private fun exportReport() {
        CoroutineScope(Dispatchers.IO).launch {
            val baseline = db.caDao().getAllBaseline()
            val detected = db.caDao().getAllDetected()
            if (baseline.isEmpty() && detected.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.toast_nothing_to_export,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            val json = ExportUtil.toJson(baseline, detected)
            withContext(Dispatchers.Main) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_export_title))
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                startActivity(Intent.createChooser(send, getString(R.string.menu_export)))
            }
        }
    }

    private fun showMonitoringModeDialog() {
        // Always-on is a Pro feature. If not entitled, route through the paywall.
        if (!billing.isUnlocked.value && prefs.isAlwaysOn) {
            // Currently in Always-on but entitlement lapsed — demote to boot+manual.
            prefs.monitoringMode = AppPrefs.MonitoringMode.BOOT_AND_MANUAL
            prefs.pendingProAlwaysOn = false
            stopService(Intent(this, CertScannerService::class.java))
        }

        val options = if (billing.isUnlocked.value) {
            arrayOf(
                getString(R.string.menu_mode_boot),
                getString(R.string.menu_mode_always)
            )
        } else {
            arrayOf(
                getString(R.string.menu_mode_boot),
                getString(R.string.menu_mode_always_locked)
            )
        }
        val checked = if (prefs.isAlwaysOn) 1 else 0
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_monitoring_mode)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val newMode = if (which == 1) {
                    AppPrefs.MonitoringMode.ALWAYS_ON
                } else {
                    AppPrefs.MonitoringMode.BOOT_AND_MANUAL
                }
                if (newMode == AppPrefs.MonitoringMode.ALWAYS_ON && !billing.isUnlocked.value) {
                    // Remember intent, then open the paywall. If they buy, Always-on starts.
                    prefs.pendingProAlwaysOn = true
                    prefs.monitoringMode = AppPrefs.MonitoringMode.BOOT_AND_MANUAL
                    dialog.dismiss()
                    showProDialog()
                    return@setSingleChoiceItems
                }
                prefs.monitoringMode = newMode
                if (newMode == AppPrefs.MonitoringMode.ALWAYS_ON) {
                    CertScannerService.startScan(this, oneShot = false)
                } else {
                    // Stop any always-on instance by requesting a one-shot that exits.
                    stopService(Intent(this, CertScannerService::class.java))
                }
                Toast.makeText(this, R.string.toast_mode_updated, Toast.LENGTH_SHORT).show()
                loadData()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * Offers the subscription (7-day free trial) and the one-time lifetime unlock,
     * or — if already entitled — a "Manage subscription" pointer (handled by Play).
     */
    private fun showProDialog() {
        val entitled = billing.isUnlocked.value
        val items = if (entitled) {
            arrayOf(getString(R.string.pro_manage))
        } else {
            arrayOf(
                getString(R.string.pro_option_subscription),
                getString(R.string.pro_option_lifetime)
            )
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.pro_dialog_title)
            .setMessage(if (entitled) getString(R.string.pro_active) else getString(R.string.pro_dialog_body))
            .setItems(items) { dialog, which ->
                when {
                    entitled -> {
                        // Open Play Store subscription management.
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(
                                "https://play.google.com/store/account/subscriptions"
                            )
                        }
                        startActivity(intent)
                    }
                    which == 0 -> billing.launchSubscription(this)
                    which == 1 -> billing.launchLifetime(this)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /** Called after returning from the Play billing flow (and from onResume). */
    private fun refreshEntitlement() {
        if (billing.isUnlocked.value) {
            if (prefs.pendingProAlwaysOn || prefs.isAlwaysOn) {
                prefs.pendingProAlwaysOn = false
                prefs.monitoringMode = AppPrefs.MonitoringMode.ALWAYS_ON
                CertScannerService.startScan(this, oneShot = false)
                loadData()
            }
        } else if (prefs.isAlwaysOn) {
            // Entitlement missing but mode still set — demote so free tier works.
            prefs.monitoringMode = AppPrefs.MonitoringMode.BOOT_AND_MANUAL
            stopService(Intent(this, CertScannerService::class.java))
        }
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_clear_title)
            .setMessage(R.string.dialog_clear_body)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.caDao().deleteAllDetected()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_history_cleared,
                            Toast.LENGTH_SHORT
                        ).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun confirmResetBaseline() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_reset_title)
            .setMessage(R.string.dialog_reset_body)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.caDao().deleteAllDetected()
                    db.caDao().deleteAllBaseline()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.toast_baseline_reset,
                            Toast.LENGTH_LONG
                        ).show()
                        triggerManualScan()
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun getColorCompat(resId: Int): Int {
        return androidx.core.content.ContextCompat.getColor(this, resId)
    }
}

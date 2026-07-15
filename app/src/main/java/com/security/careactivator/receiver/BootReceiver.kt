package com.security.careactivator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.security.careactivator.prefs.AppPrefs
import com.security.careactivator.service.CertScannerService

/**
 * Starts a CA scan after boot. In BOOT_AND_MANUAL mode the service exits after
 * one scan; in ALWAYS_ON it stays as a foreground monitor.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Received broadcast: ${intent?.action}")

        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val prefs = AppPrefs.get(context)
        if (!prefs.onboardingCompleted) {
            Log.i(TAG, "Onboarding not completed — skipping boot scan")
            return
        }

        Log.i(TAG, "Boot detected — starting CA scan (mode=${prefs.monitoringMode})")
        // oneShot is derived from monitoring mode inside the service
        CertScannerService.startScan(context, oneShot = !prefs.isAlwaysOn)
    }
}

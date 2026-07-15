package com.security.careactivator

import android.app.Application
import android.util.Log
import com.security.careactivator.billing.BillingManager

/**
 * Application entry. Initializes Play Billing; the Always-on foreground monitor is
 * only (re)started once entitlement is confirmed in MainActivity, never blindly at boot.
 */
class CAWatchApp : Application() {

    companion object {
        private const val TAG = "CaDetectorApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")

        // Initialize Play Billing; entitlement is verified before Always-on starts.
        BillingManager.create(this).start()
    }
}

package com.security.careactivator.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight preferences for onboarding and monitoring mode.
 * Path A product: free local utility — no accounts, no cloud.
 */
class AppPrefs(context: Context) {

    enum class MonitoringMode {
        /** Foreground service stays running (persistent notification). */
        ALWAYS_ON,

        /** Scan on boot + manual only; service stops after each scan. */
        BOOT_AND_MANUAL
    }

    private val prefs: SharedPreferences =
        // Use device-protected storage so the app can read monitoring mode /
        // onboarding state BEFORE the user unlocks credential-encrypted storage
        // (e.g. on boot, when BootReceiver is directBootAware). Reading
        // credential-encrypted SharedPreferences at that point throws
        // IllegalStateException and crashes the app on launch (Android 15).
        context.applicationContext
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var monitoringMode: MonitoringMode
        get() {
            val raw = prefs.getString(KEY_MONITORING_MODE, MonitoringMode.BOOT_AND_MANUAL.name)
            return runCatching { MonitoringMode.valueOf(raw!!) }
                .getOrDefault(MonitoringMode.BOOT_AND_MANUAL)
        }
        set(value) = prefs.edit().putString(KEY_MONITORING_MODE, value.name).apply()

    val isAlwaysOn: Boolean
        get() = monitoringMode == MonitoringMode.ALWAYS_ON

    /** Cached entitlement for the Pro (Always-on) feature. Source of truth is Play. */
    var isProUnlocked: Boolean
        get() = prefs.getBoolean(KEY_PRO_UNLOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRO_UNLOCKED, value).apply()

    /** User picked Always-on during onboarding but wasn't subscribed yet. */
    var pendingProAlwaysOn: Boolean
        get() = prefs.getBoolean(KEY_PENDING_PRO, false)
        set(value) = prefs.edit().putBoolean(KEY_PENDING_PRO, value).apply()

    companion object {
        private const val PREFS_NAME = "ca_detector_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
        private const val KEY_MONITORING_MODE = "monitoring_mode"
        private const val KEY_PRO_UNLOCKED = "pro_unlocked"
        private const val KEY_PENDING_PRO = "pending_pro_always_on"

        @Volatile
        private var instance: AppPrefs? = null

        fun get(context: Context): AppPrefs {
            return instance ?: synchronized(this) {
                instance ?: AppPrefs(context).also { instance = it }
            }
        }
    }
}

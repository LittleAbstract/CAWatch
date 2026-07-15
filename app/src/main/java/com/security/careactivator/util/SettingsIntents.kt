package com.security.careactivator.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/**
 * Best-effort deep links into Android credential / security settings.
 * OEM skins differ; we fall back to generic security settings.
 */
object SettingsIntents {

    fun openUserCredentials(context: Context) {
        val candidates = listOf(
            Intent("com.android.settings.TRUSTED_CREDENTIALS_USER"),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    // try next
                }
            }
        }
        Toast.makeText(
            context,
            "Open Settings → Security → Encryption & credentials to manage CAs",
            Toast.LENGTH_LONG
        ).show()
    }
}

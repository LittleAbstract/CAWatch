package com.security.careactivator.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.security.careactivator.databinding.ActivityOnboardingBinding
import com.security.careactivator.prefs.AppPrefs
import com.security.careactivator.service.CertScannerService

/**
 * First-run: explain product, pick monitoring mode, request notifications,
 * capture baseline via an initial scan.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: AppPrefs

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            finishOnboarding()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AppPrefs.get(this)

        if (prefs.onboardingCompleted) {
            goMain()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnGetStarted.setOnClickListener {
            val wantAlwaysOn = binding.radioAlways.isChecked
            prefs.monitoringMode = if (wantAlwaysOn) {
                AppPrefs.MonitoringMode.ALWAYS_ON
            } else {
                AppPrefs.MonitoringMode.BOOT_AND_MANUAL
            }
            // Always-on is a Pro feature: remember the intent; MainActivity verifies
            // entitlement (and opens the paywall if needed) before starting the service.
            prefs.pendingProAlwaysOn = wantAlwaysOn
            requestNotificationsThenFinish()
        }
    }

    private fun requestNotificationsThenFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        finishOnboarding()
    }

    private fun finishOnboarding() {
        prefs.onboardingCompleted = true
        // Capture baseline (one-shot). Always-on, if chosen, starts from MainActivity
        // once Play Billing entitlement is confirmed.
        CertScannerService.startScan(this, oneShot = true)
        goMain()
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

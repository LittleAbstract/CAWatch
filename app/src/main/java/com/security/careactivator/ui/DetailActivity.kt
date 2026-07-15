package com.security.careactivator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.security.careactivator.R
import com.security.careactivator.databinding.ActivityDetailBinding
import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.CertDatabase
import com.security.careactivator.util.ExportUtil
import com.security.careactivator.util.SettingsIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detail view for a detected CA: remediate, trust, share, or dismiss.
 */
class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_FINGERPRINT = "extra_fingerprint"
        const val EXTRA_ISSUER = "extra_issuer"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_DETECTED_AT = "extra_detected_at"

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private lateinit var binding: ActivityDetailBinding
    private lateinit var db: CertDatabase

    private var eventId: Long = -1
    private var fingerprint: String = ""
    private var issuer: String = ""
    private var subject: String = ""
    private var detectedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = CertDatabase.getDatabase(this)

        eventId = intent.getLongExtra(EXTRA_ID, -1)
        fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT).orEmpty()
        issuer = intent.getStringExtra(EXTRA_ISSUER).orEmpty()
        subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty()
        detectedAt = intent.getLongExtra(EXTRA_DETECTED_AT, 0L)

        binding.toolbar.setNavigationOnClickListener { finish() }
        // Ensure back arrow tint works even if theme attribute is missing.
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        binding.textIssuer.text = issuer.ifBlank { "—" }
        binding.textSubject.text = subject.ifBlank { "—" }
        binding.textFingerprint.text = fingerprint.ifBlank { "—" }
        binding.textDetectedAt.text = if (detectedAt > 0) {
            DATE_FORMAT.format(Date(detectedAt))
        } else {
            "—"
        }

        binding.btnOpenSettings.setOnClickListener {
            SettingsIntents.openUserCredentials(this)
        }

        binding.btnAcceptBaseline.setOnClickListener { confirmAcceptBaseline() }
        binding.btnShare.setOnClickListener { shareDetails() }
        binding.btnDismiss.setOnClickListener { dismissFromList() }
    }

    private fun confirmAcceptBaseline() {
        AlertDialog.Builder(this)
            .setTitle(R.string.detail_accept_confirm_title)
            .setMessage(R.string.detail_accept_confirm_body)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> acceptBaseline() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun acceptBaseline() {
        CoroutineScope(Dispatchers.IO).launch {
            db.caDao().insertBaseline(
                BaselineCaEntity(
                    fingerprint = fingerprint,
                    issuer = issuer,
                    subject = subject
                )
            )
            if (fingerprint.isNotEmpty()) {
                db.caDao().deleteDetectedByFingerprint(fingerprint)
            } else if (eventId >= 0) {
                db.caDao().deleteDetectedById(eventId)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetailActivity, R.string.detail_accepted, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun dismissFromList() {
        CoroutineScope(Dispatchers.IO).launch {
            if (eventId >= 0) {
                db.caDao().deleteDetectedById(eventId)
            } else if (fingerprint.isNotEmpty()) {
                db.caDao().deleteDetectedByFingerprint(fingerprint)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetailActivity, R.string.detail_dismissed, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun shareDetails() {
        val text = ExportUtil.singleCaText(issuer, subject, fingerprint, detectedAt)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_export_title))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.detail_share)))
    }
}

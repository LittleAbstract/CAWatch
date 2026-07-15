package com.security.careactivator.util

import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.DetectedCaEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds a local JSON export of baseline + detected CAs for sharing/evidence.
 * Nothing is uploaded — caller uses ACTION_SEND.
 */
object ExportUtil {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun toJson(
        baseline: List<BaselineCaEntity>,
        detected: List<DetectedCaEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "CAWatch")
        root.put("version", 1)
        root.put("exportedAt", isoFormat.format(Date()))
        root.put("baselineCount", baseline.size)
        root.put("detectedCount", detected.size)

        val baselineArr = JSONArray()
        baseline.forEach { ca ->
            baselineArr.put(
                JSONObject()
                    .put("fingerprint", ca.fingerprint)
                    .put("issuer", ca.issuer)
                    .put("subject", ca.subject)
            )
        }
        root.put("baseline", baselineArr)

        val detectedArr = JSONArray()
        detected.forEach { ca ->
            detectedArr.put(
                JSONObject()
                    .put("fingerprint", ca.fingerprint)
                    .put("issuer", ca.issuer)
                    .put("subject", ca.subject)
                    .put("detectedAt", ca.detectedAt)
                    .put("detectedAtIso", isoFormat.format(Date(ca.detectedAt)))
            )
        }
        root.put("detected", detectedArr)

        return root.toString(2)
    }

    fun singleCaText(issuer: String, subject: String, fingerprint: String, detectedAt: Long): String {
        val whenStr = isoFormat.format(Date(detectedAt))
        return buildString {
            appendLine("CAWatch — detected certificate")
            appendLine("Detected: $whenStr")
            appendLine("Issuer: $issuer")
            appendLine("Subject: $subject")
            appendLine("SHA-256: $fingerprint")
            appendLine()
            appendLine("This CA was not in the trusted baseline at first run.")
            appendLine("Review under Settings → Security → Encryption & credentials.")
        }
    }
}

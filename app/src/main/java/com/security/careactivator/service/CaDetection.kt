package com.security.careactivator.service

import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.CaInfo
import com.security.careactivator.db.DetectedCaEntity
import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * Pure detection core for user CA monitoring.
 *
 * This class contains the algorithm previously inlined in [CertScannerService]
 * (now moved out so it can be unit-tested on the JVM without an Android device):
 *
 *  1. Each certificate is reduced to a stable [CaInfo] (SHA-256 fingerprint,
 *     issuer, subject).
 *  2. On the first scan the trusted baseline is whatever CAs are present.
 *  3. On later scans, any CA whose fingerprint is absent from the baseline AND
 *     absent from the already-logged detections is reported as newly installed.
 *
 * Nothing in here touches Android APIs, so it runs under plain JUnit.
 */
object CaDetection {

    /**
     * Reduce an X.509 certificate to its [CaInfo] (SHA-256 fingerprint + names).
     */
    fun toCaInfo(cert: X509Certificate): CaInfo = CaInfo(
        fingerprint = fingerprint(cert, "SHA-256"),
        issuer = cert.issuerX500Principal.name,
        subject = cert.subjectX500Principal.name
    )

    /**
     * Compute the hex SHA-256 fingerprint of a certificate's DER encoding.
     */
    fun fingerprint(cert: X509Certificate, algorithm: String = "SHA-256"): String {
        val md = MessageDigest.getInstance(algorithm)
        md.update(cert.encoded)
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Diff the current trust store against the trusted baseline and the
     * already-logged detections.
     *
     * @return the certs that are genuinely *new* (not in baseline, not yet
     *         reported). These are the ones the service should persist + alert on.
     */
    fun findNewCerts(
        currentCerts: List<CaInfo>,
        baseline: List<BaselineCaEntity>,
        alreadyDetected: List<DetectedCaEntity>
    ): List<CaInfo> {
        val baselineFps = baseline.map { it.fingerprint }.toSet()
        val detectedFps = alreadyDetected.map { it.fingerprint }.toSet()
        return currentCerts.filter { cert ->
            !baselineFps.contains(cert.fingerprint) && !detectedFps.contains(cert.fingerprint)
        }
    }
}

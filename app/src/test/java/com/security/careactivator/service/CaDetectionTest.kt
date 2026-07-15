package com.security.careactivator.service

import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.DetectedCaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Validates the user-CA detection core against REAL X.509 certificates
 * (generated with `keytool`, PEM in src/test/resources/certs).
 *
 * These tests prove the algorithm that runs on-device in CertScannerService
 * can, in fact, fingerprint an installed cert and flag it as newly installed
 * when it is absent from the trusted baseline.
 */
class CaDetectionTest {

    private val cf = CertificateFactory.getInstance("X.509")

    private fun load(name: String): X509Certificate {
        val stream: InputStream = javaClass.getResourceAsStream("/certs/$name")
            ?: throw IllegalStateException("missing test cert /certs/$name")
        return cf.generateCertificate(stream) as X509Certificate
    }

    @Test
    fun `fingerprint is stable SHA-256 hex of DER encoding`() {
        val cert = load("baseline.crt")
        val fp = CaDetection.fingerprint(cert)
        // 256-bit SHA-256 => 64 hex chars
        assertEquals(64, fp.length)
        // deterministic
        assertEquals(fp, CaDetection.fingerprint(cert))
        // lowercase hex only
        assertTrue(fp.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `toCaInfo captures issuer and subject from the cert`() {
        val cert = load("baseline.crt")
        val info = CaDetection.toCaInfo(cert)
        assertTrue(info.issuer.contains("Test CA Baseline"))
        assertTrue(info.subject.contains("Test CA Baseline"))
        assertEquals(CaDetection.fingerprint(cert), info.fingerprint)
    }

    @Test
    fun `pure diff with empty baseline returns all current certs (service guards first-run capture)`() {
        // findNewCerts is a PURE diff: with no baseline and nothing already
        // detected, every current cert is "not in baseline", so it returns all.
        // The decision to treat an empty baseline as "capture, don't alert"
        // lives in CertScannerService.runScan()'s `if (baseline.isEmpty())`
        // branch, which calls insertBaselineAll() instead of findNewCerts().
        // This test pins the pure-contract so that guard remains intentional.
        val current = listOf(
            CaDetection.toCaInfo(load("baseline.crt")),
            CaDetection.toCaInfo(load("new.crt"))
        )
        val new = CaDetection.findNewCerts(
            currentCerts = current,
            baseline = emptyList(),
            alreadyDetected = emptyList()
        )
        assertEquals(2, new.size)
    }

    @Test
    fun `installing a new user CA is detected against the baseline`() {
        val baseline = listOf(
            CaDetection.toCaInfo(load("baseline.crt"))
        ).map { BaselineCaEntity(it.fingerprint, it.issuer, it.subject) }

        val current = listOf(
            CaDetection.toCaInfo(load("baseline.crt")),
            CaDetection.toCaInfo(load("new.crt"))
        )

        val new = CaDetection.findNewCerts(
            currentCerts = current,
            baseline = baseline,
            alreadyDetected = emptyList()
        )

        assertEquals(1, new.size)
        assertTrue(new[0].issuer.contains("Test CA Newly Installed"))
    }

    @Test
    fun `multiple new CAs are all reported`() {
        val baseline = listOf(
            CaDetection.toCaInfo(load("baseline.crt"))
        ).map { BaselineCaEntity(it.fingerprint, it.issuer, it.subject) }

        val current = listOf(
            CaDetection.toCaInfo(load("baseline.crt")),
            CaDetection.toCaInfo(load("new.crt")),
            CaDetection.toCaInfo(load("another.crt"))
        )

        val new = CaDetection.findNewCerts(
            currentCerts = current,
            baseline = baseline,
            alreadyDetected = emptyList()
        )

        assertEquals(2, new.size)
        val issuers = new.map { it.issuer }.toSet()
        assertTrue(issuers.any { it.contains("Test CA Newly Installed") })
        assertTrue(issuers.any { it.contains("Test CA Another New") })
    }

    @Test
    fun `already-detected CA is not reported again on re-scan (dedup)`() {
        val baseline = listOf(
            CaDetection.toCaInfo(load("baseline.crt"))
        ).map { BaselineCaEntity(it.fingerprint, it.issuer, it.subject) }

        val newInfo = CaDetection.toCaInfo(load("new.crt"))
        val current = listOf(
            CaDetection.toCaInfo(load("baseline.crt")),
            newInfo
        )

        // First scan reports it.
        val first = CaDetection.findNewCerts(current, baseline, emptyList())
        assertEquals(1, first.size)

        // Second scan: it's now in `alreadyDetected`, so report nothing new.
        val alreadyDetected = listOf(
            DetectedCaEntity(
                fingerprint = newInfo.fingerprint,
                issuer = newInfo.issuer,
                subject = newInfo.subject,
                detectedAt = 0L
            )
        )
        val second = CaDetection.findNewCerts(current, baseline, alreadyDetected)
        assertTrue(second.isEmpty())
    }

    @Test
    fun `cert present in baseline is never flagged even if re-scanned`() {
        val baselineInfo = CaDetection.toCaInfo(load("baseline.crt"))
        val baseline = listOf(
            BaselineCaEntity(baselineInfo.fingerprint, baselineInfo.issuer, baselineInfo.subject)
        )
        val current = listOf(baselineInfo)

        val new = CaDetection.findNewCerts(current, baseline, emptyList())
        assertTrue(new.isEmpty())
    }
}

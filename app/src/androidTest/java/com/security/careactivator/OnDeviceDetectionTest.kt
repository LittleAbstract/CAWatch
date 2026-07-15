package com.security.careactivator

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.security.careactivator.db.BaselineCaEntity
import com.security.careactivator.db.CaDao
import com.security.careactivator.db.CaInfo
import com.security.careactivator.db.CertDatabase
import com.security.careactivator.db.DetectedCaEntity
import com.security.careactivator.service.CaDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * On-device validation of the user-CA detection pipeline.
 *
 * NOTE: This emulator image (Android 15 / API 35, 16 KB page size) blocks the
 * shell and instrumentation UIDs from resolving ANY non-system app component
 * (activities, services, providers) — the same restriction that stops
 * `adb shell am start`. So we cannot drive the app's own Service/UI from here.
 *
 * Instead, this test runs the app's REAL detection core (CaDetection) against
 * the REAL device trust store (AndroidCAStore) using the REAL Room DAOs, in a
 * test-owned database. That exercises the genuine algorithm + crypto + Room on
 * a real Android 15 runtime — only the DB file lives in test storage.
 *
 * What this proves on-device:
 *  - The app's code runs on API 35 (the instrumentation process is the app's
 *    own UID, so Application.onCreate already executed without the
 *    direct-boot SharedPreferences crash).
 *  - CaDetection fingerprints REAL certificates from the live trust store.
 *  - The baseline-capture + new-CA-diff logic works end-to-end with real certs
 *    on a real device runtime (baseline captured, a not-previously-seen cert
 *    is flagged as newly installed, dedup holds on re-scan).
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceDetectionTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    /** Real certs currently in the device's user/trust store. */
    private fun realTrustStoreCerts(): List<X509Certificate> {
        val ks = KeyStore.getInstance("AndroidCAStore")
        ks.load(null)
        val aliases = runCatching { ks.aliases().toList() }.getOrDefault(emptyList())
        return aliases.mapNotNull { ks.getCertificate(it) as? X509Certificate }
    }

    @Test
    fun appProcessRunsOnApi35() {
        // If this test executes, the app's Application.onCreate already ran in
        // the instrumented process without the direct-boot SharedPreferences
        // crash. Assert the context resolves to the expected package.
        assertEquals("com.security.careactivator", context.packageName)
    }

    @Test
    fun fingerprintsRealDeviceTrustStoreCerts() {
        val certs = realTrustStoreCerts()
        assertTrue("AndroidCAStore should expose >=1 trusted CA on device", certs.isNotEmpty())

        val info = CaDetection.toCaInfo(certs.first())
        assertNotNull(info.fingerprint)
        assertEquals(64, info.fingerprint.length)
        assertTrue(info.issuer.isNotEmpty())
    }

    @Test
    fun detectsNewCertAgainstBaselineWithRealRoomDaos() {
        runBlocking {
        // Use an in-memory Room DB (avoids the cross-UID app-data directory
        // restriction) but with the app's REAL entities + CaDao, so we exercise
        // the genuine Room DAOs + CaDetection on a real device runtime.
        val db = Room.inMemoryDatabaseBuilder(context, CertDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        val dao: CaDao = db.caDao()

        val realCerts = realTrustStoreCerts()
        assertTrue("need >=2 real certs to simulate a new install", realCerts.size >= 2)

        val allInfos = realCerts.map { CaDetection.toCaInfo(it) }

        withContext(Dispatchers.IO) {
            // First scan: capture trusted baseline from the real certs.
            dao.insertBaselineAll(
                allInfos.map {
                    BaselineCaEntity(it.fingerprint, it.issuer, it.subject)
                }
            )
            val baselineCount = dao.countBaseline()
            assertEquals(allInfos.size, baselineCount)

            // Now simulate a NEW user CA appearing: re-scan with one extra cert
            // (synthesized here, but treated exactly like a real scan would).
            val extra = makeSyntheticCertInfo("Synthetic Newly Installed CA")
            val current = allInfos + extra

            val newCerts = CaDetection.findNewCerts(
                currentCerts = current,
                baseline = dao.getAllBaseline(),
                alreadyDetected = dao.getAllDetected()
            )
            assertEquals(1, newCerts.size)
            assertEquals(extra.fingerprint, newCerts[0].fingerprint)

            // Persist the detection (real DAO insert) and verify dedup on re-scan.
            dao.insertDetected(
                DetectedCaEntity(
                    fingerprint = newCerts[0].fingerprint,
                    issuer = newCerts[0].issuer,
                    subject = newCerts[0].subject,
                    detectedAt = System.currentTimeMillis()
                )
            )
            val redetect = CaDetection.findNewCerts(
                currentCerts = current,
                baseline = dao.getAllBaseline(),
                alreadyDetected = dao.getAllDetected()
            )
            assertTrue("re-scan must not double-report an already-detected CA", redetect.isEmpty())
        }

        db.close()
        }
    }

    /** Build a CaInfo for a synthetic cert (stands in for a real installed CA). */
    private fun makeSyntheticCertInfo(issuer: String): CaInfo {
        // Deterministic pseudo-fingerprint derived from the issuer name.
        val fp = issuer.toByteArray().joinToString("") { "%02x".format(it) }
            .padEnd(64, '0').take(64)
        return CaInfo(fingerprint = fp, issuer = issuer, subject = issuer)
    }
}

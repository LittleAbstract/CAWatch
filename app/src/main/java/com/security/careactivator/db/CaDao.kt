package com.security.careactivator.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for the CAWatch database.
 */
@Dao
interface CaDao {

    // =========================================================================
    // Detected (newly installed) CAs
    // =========================================================================

    @Query("SELECT * FROM detected_ca ORDER BY detectedAt DESC")
    suspend fun getAllDetected(): List<DetectedCaEntity>

    @Query("SELECT * FROM detected_ca WHERE id = :id LIMIT 1")
    suspend fun getDetectedById(id: Long): DetectedCaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetected(event: DetectedCaEntity)

    @Query("SELECT * FROM detected_ca WHERE fingerprint = :fingerprint ORDER BY detectedAt DESC")
    suspend fun getDetectedByFingerprint(fingerprint: String): List<DetectedCaEntity>

    @Query("DELETE FROM detected_ca WHERE id = :id")
    suspend fun deleteDetectedById(id: Long)

    @Query("DELETE FROM detected_ca WHERE fingerprint = :fingerprint")
    suspend fun deleteDetectedByFingerprint(fingerprint: String)

    @Query("DELETE FROM detected_ca")
    suspend fun deleteAllDetected()

    @Query("DELETE FROM detected_ca WHERE detectedAt < :timestamp")
    suspend fun deleteDetectedOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM detected_ca")
    suspend fun countDetected(): Int

    // =========================================================================
    // Trusted baseline
    // =========================================================================

    @Query("SELECT * FROM baseline_ca")
    suspend fun getAllBaseline(): List<BaselineCaEntity>

    @Query("SELECT * FROM baseline_ca WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getBaselineByFingerprint(fingerprint: String): BaselineCaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaselineAll(certs: List<BaselineCaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseline(cert: BaselineCaEntity)

    @Query("SELECT COUNT(*) FROM baseline_ca")
    suspend fun countBaseline(): Int

    @Query("DELETE FROM baseline_ca")
    suspend fun deleteAllBaseline()
}


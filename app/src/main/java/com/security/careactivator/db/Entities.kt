package com.security.careactivator.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Newly-detected user-installed CA (appeared after trusted baseline).
 */
@Entity(tableName = "detected_ca")
data class DetectedCaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fingerprint: String,
    val issuer: String,
    val subject: String,
    val detectedAt: Long
)

/**
 * Trusted baseline of user-installed CAs (captured on first run).
 * Fingerprint is the primary key so "accept into baseline" is idempotent.
 */
@Entity(tableName = "baseline_ca")
data class BaselineCaEntity(
    @PrimaryKey
    val fingerprint: String,

    val issuer: String,
    val subject: String
)

/**
 * DTO for certificate info while scanning.
 */
data class CaInfo(
    val fingerprint: String,
    val issuer: String,
    val subject: String
)

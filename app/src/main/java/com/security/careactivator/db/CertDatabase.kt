package com.security.careactivator.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for CAWatch.
 *
 * Stores the trusted baseline of user-installed CAs and any newly
 * detected CA certificates.
 */
@Database(
    entities = [BaselineCaEntity::class, DetectedCaEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CertDatabase : RoomDatabase() {

    abstract fun caDao(): CaDao

    companion object {
        @Volatile
        private var INSTANCE: CertDatabase? = null
        private const val DATABASE_NAME = "ca_detector.db"

        fun getDatabase(context: Context): CertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}

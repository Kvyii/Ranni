package com.ranni.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ranni.app.BuildConfig
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog

// When bumping the version, add a new @AutoMigration entry for additive changes (new columns/tables).
// For renames or deletes, supply a spec class — see Room docs.
@Database(
    entities = [Exercise::class, SessionLog::class, ClimbLog::class, MetricsConfig::class],
    version = 10,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 9, to = 10)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun climbLogDao(): ClimbLogDao
    abstract fun metricsConfigDao(): MetricsConfigDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ranni_db"
                ).apply {
                    // Debug: wipe DB on failed migration for fast iteration
                    // Release: crash on failed migration to protect user data
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    }
                }.build().also { INSTANCE = it }
            }
    }
}

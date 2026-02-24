package com.ranni.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog

// No migrations kept — all users are on the current schema.
// fallbackToDestructiveMigration() handles any stale installs.
@Database(
    entities = [Exercise::class, SessionLog::class, ClimbLog::class, MetricsConfig::class, InjuryLog::class],
    version = 18,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),  // Adds injury_logs table
        AutoMigration(from = 11, to = 12),  // Adds orderIndex column to exercises
        AutoMigration(from = 12, to = 13),  // Adds gymName column to climb_logs
        AutoMigration(from = 13, to = 14),  // Adds uiTheme column to metrics_config
        // 14→15: table recreate, handled by MIGRATION_14_15 below (default value rename fix)
        AutoMigration(from = 15, to = 16),  // Adds filterRepeats column to metrics_config
        AutoMigration(from = 16, to = 17),  // Adds showPeriodComparison column to metrics_config
        AutoMigration(from = 17, to = 18)   // Adds showAboveMedianOnly column to metrics_config
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun climbLogDao(): ClimbLogDao
    abstract fun metricsConfigDao(): MetricsConfigDao
    abstract fun injuryLogDao(): InjuryLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ranni_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

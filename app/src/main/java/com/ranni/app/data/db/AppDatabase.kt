package com.ranni.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ranni.app.BuildConfig
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog

// When bumping the version, add a new @AutoMigration entry for additive changes (new columns/tables).
// For renames or deletes, supply a spec class — see Room docs.
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

        // Recreate metrics_config to fix the column default value.
        // SQLite does not support ALTER COLUMN, so we must use the copy-and-replace pattern.
        // v14 was originally shipped with DEFAULT 'ORIGINAL'; this corrects it to 'RANNI_DARK'
        // so that Room's schema verification passes on v15.
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a new table with the correct default value
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `metrics_config_new` " +
                    "(`id` INTEGER NOT NULL, `months` INTEGER NOT NULL, `topK` INTEGER NOT NULL, " +
                    "`timelineMonths` INTEGER NOT NULL, `showClimbDots` INTEGER NOT NULL, " +
                    "`showExerciseDots` INTEGER NOT NULL, " +
                    "`uiTheme` TEXT NOT NULL DEFAULT 'RANNI_DARK', " +
                    "PRIMARY KEY(`id`))"
                )
                // 2. Copy existing data; rows with 'ORIGINAL' keep their stored value as-is
                db.execSQL(
                    "INSERT INTO `metrics_config_new` " +
                    "SELECT `id`, `months`, `topK`, `timelineMonths`, `showClimbDots`, `showExerciseDots`, `uiTheme` " +
                    "FROM `metrics_config`"
                )
                // 3. Drop the old table
                db.execSQL("DROP TABLE `metrics_config`")
                // 4. Rename the new table to the canonical name
                db.execSQL("ALTER TABLE `metrics_config_new` RENAME TO `metrics_config`")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ranni_db"
                ).apply {
                    addMigrations(MIGRATION_14_15)
                    // Debug: wipe DB on failed migration for fast iteration
                    // Release: crash on failed migration to protect user data
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    }
                }.build().also { INSTANCE = it }
            }
    }
}

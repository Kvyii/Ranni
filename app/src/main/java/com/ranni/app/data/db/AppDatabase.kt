package com.ranni.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog

@Database(entities = [Exercise::class, SessionLog::class, ClimbLog::class, MetricsConfig::class], version = 9, exportSchema = false)
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
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

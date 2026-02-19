package com.ranni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ranni.app.data.model.MetricsConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricsConfigDao {
    @Query("SELECT * FROM metrics_config WHERE id = 1")
    fun getConfig(): Flow<MetricsConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: MetricsConfig)
}

package com.ranni.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ranni.app.data.model.ClimbLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimbLogDao {
    @Insert
    suspend fun insert(log: ClimbLog)

    @Delete
    suspend fun delete(log: ClimbLog)

    @Query("SELECT * FROM climb_logs ORDER BY loggedAt DESC")
    fun getAllLogs(): Flow<List<ClimbLog>>

    // Backfill gymName for legacy rows (gymName = '') that match a known route name.
    // Safe to run repeatedly — the WHERE clause means it's a no-op once all rows are populated.
    @Query("UPDATE climb_logs SET gymName = :gymName WHERE gymName = '' AND color = :routeName")
    suspend fun backfillGymName(routeName: String, gymName: String)
}

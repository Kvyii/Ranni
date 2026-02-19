package com.ranni.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ranni.app.data.model.ClimbLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimbLogDao {
    @Insert
    suspend fun insert(log: ClimbLog)

    @Query("SELECT * FROM climb_logs ORDER BY loggedAt DESC")
    fun getAllLogs(): Flow<List<ClimbLog>>
}

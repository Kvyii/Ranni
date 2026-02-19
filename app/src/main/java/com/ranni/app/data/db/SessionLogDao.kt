package com.ranni.app.data.db

import androidx.room.*
import com.ranni.app.data.model.SessionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Insert
    suspend fun insert(log: SessionLog)

    @Query("SELECT * FROM session_logs ORDER BY completedAt DESC")
    fun getAllLogs(): Flow<List<SessionLog>>

    @Delete
    suspend fun delete(log: SessionLog)
}

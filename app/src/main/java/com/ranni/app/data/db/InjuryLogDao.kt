package com.ranni.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ranni.app.data.model.InjuryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InjuryLogDao {
    @Insert
    suspend fun insert(log: InjuryLog)

    @Delete
    suspend fun delete(log: InjuryLog)

    @Query("SELECT * FROM injury_logs ORDER BY loggedAt DESC")
    fun getAllLogs(): Flow<List<InjuryLog>>
}

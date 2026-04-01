package com.ranni.app.data.repository

import com.ranni.app.data.db.ClimbLogDao
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.ClimbType
import kotlinx.coroutines.flow.Flow

class ClimbRepository(private val dao: ClimbLogDao) {
    fun getAllLogs(): Flow<List<ClimbLog>> = dao.getAllLogs()
    // Log a climb with the given type (New/Flash/Repeat) — score is already multiplied by caller.
    // Optional loggedAt overrides the default timestamp (used by the amend flow for past-day entries).
    suspend fun logClimb(
        color: String,
        gymName: String,
        score: Int,
        climbType: ClimbType = ClimbType.NEW,
        loggedAt: Long = System.currentTimeMillis()
    ) = dao.insert(ClimbLog(color = color, gymName = gymName, score = score, climbType = climbType.name, loggedAt = loggedAt))
    suspend fun deleteLog(log: ClimbLog) = dao.delete(log)
}

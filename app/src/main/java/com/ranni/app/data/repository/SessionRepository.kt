package com.ranni.app.data.repository

import com.ranni.app.data.db.SessionLogDao
import com.ranni.app.data.model.SessionLog
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionLogDao) {
    fun getAllLogs(): Flow<List<SessionLog>> = dao.getAllLogs()
    suspend fun logSession(exerciseName: String) = dao.insert(SessionLog(exerciseName = exerciseName))
}

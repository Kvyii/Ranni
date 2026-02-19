package com.ranni.app.data.repository

import com.ranni.app.data.db.ClimbLogDao
import com.ranni.app.data.model.ClimbLog
import kotlinx.coroutines.flow.Flow

class ClimbRepository(private val dao: ClimbLogDao) {
    fun getAllLogs(): Flow<List<ClimbLog>> = dao.getAllLogs()
    suspend fun logClimb(color: String, score: Int) = dao.insert(ClimbLog(color = color, score = score))
}

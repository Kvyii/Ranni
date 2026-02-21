package com.ranni.app.data.repository

import com.ranni.app.data.db.InjuryLogDao
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import kotlinx.coroutines.flow.Flow

class InjuryRepository(private val dao: InjuryLogDao) {
    fun getAllLogs(): Flow<List<InjuryLog>> = dao.getAllLogs()

    /** Insert a new injury marker for the current date/time with the given severity. */
    suspend fun logInjury(severity: InjurySeverity) =
        dao.insert(InjuryLog(severity = severity.name))

    suspend fun deleteLog(log: InjuryLog) = dao.delete(log)
}

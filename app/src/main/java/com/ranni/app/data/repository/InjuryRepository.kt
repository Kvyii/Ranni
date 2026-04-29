package com.ranni.app.data.repository

import com.ranni.app.data.db.InjuryLogDao
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import kotlinx.coroutines.flow.Flow

class InjuryRepository(private val dao: InjuryLogDao) {
    fun getAllLogs(): Flow<List<InjuryLog>> = dao.getAllLogs()

    /** Insert a new injury marker with the given severity.
     *  Optional [loggedAt] overrides the default timestamp (used by the amend flow for past-day entries). */
    suspend fun logInjury(severity: InjurySeverity, loggedAt: Long = System.currentTimeMillis()) =
        dao.insert(InjuryLog(severity = severity.name, loggedAt = loggedAt))

    suspend fun deleteLog(log: InjuryLog) = dao.delete(log)
}

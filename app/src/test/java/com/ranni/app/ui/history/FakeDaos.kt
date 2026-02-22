package com.ranni.app.ui.history

import com.ranni.app.data.GymOrderPreferences
import com.ranni.app.data.db.ClimbLogDao
import com.ranni.app.data.db.InjuryLogDao
import com.ranni.app.data.db.MetricsConfigDao
import com.ranni.app.data.db.SessionLogDao
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake of [ClimbLogDao] for unit tests.
 * Backed by a MutableStateFlow so the ViewModel reacts to changes.
 */
class FakeClimbLogDao : ClimbLogDao {
    private val climbs = MutableStateFlow<List<ClimbLog>>(emptyList())

    override suspend fun insert(log: ClimbLog) {
        climbs.value = climbs.value + log
    }

    override suspend fun delete(log: ClimbLog) {
        climbs.value = climbs.value.filter { it.id != log.id }
    }

    override fun getAllLogs(): Flow<List<ClimbLog>> =
        climbs.map { list -> list.sortedByDescending { it.loggedAt } }

    override suspend fun backfillGymName(routeName: String, gymName: String) {
        // Update all climbs with matching color and empty gymName
        climbs.value = climbs.value.map { log ->
            if (log.color == routeName && log.gymName.isEmpty()) log.copy(gymName = gymName)
            else log
        }
    }

    override suspend fun renameGym(oldName: String, newName: String) {
        // Replace all climbs whose gymName matches oldName with newName
        climbs.value = climbs.value.map { log ->
            if (log.gymName == oldName) log.copy(gymName = newName) else log
        }
    }

    /** Bulk-set climbs for test setup (bypasses insert one-by-one). */
    fun setClimbs(logs: List<ClimbLog>) {
        climbs.value = logs
    }
}

/**
 * In-memory fake of [SessionLogDao] for unit tests.
 */
class FakeSessionLogDao : SessionLogDao {
    private val sessions = MutableStateFlow<List<SessionLog>>(emptyList())

    override suspend fun insert(log: SessionLog) {
        sessions.value = sessions.value + log
    }

    override fun getAllLogs(): Flow<List<SessionLog>> =
        sessions.map { list -> list.sortedByDescending { it.completedAt } }

    override suspend fun delete(log: SessionLog) {
        sessions.value = sessions.value.filter { it.id != log.id }
    }

    /** Bulk-set sessions for test setup. */
    fun setSessions(logs: List<SessionLog>) {
        sessions.value = logs
    }
}

/**
 * In-memory fake of [InjuryLogDao] for unit tests.
 */
class FakeInjuryLogDao : InjuryLogDao {
    private val injuries = MutableStateFlow<List<InjuryLog>>(emptyList())

    override suspend fun insert(log: InjuryLog) {
        injuries.value = injuries.value + log
    }

    override suspend fun delete(log: InjuryLog) {
        injuries.value = injuries.value.filter { it.id != log.id }
    }

    override fun getAllLogs(): Flow<List<InjuryLog>> =
        injuries.map { list -> list.sortedByDescending { it.loggedAt } }

    /** Bulk-set injuries for test setup. */
    fun setInjuries(logs: List<InjuryLog>) {
        injuries.value = logs
    }
}

/**
 * In-memory fake of [MetricsConfigDao] for unit tests.
 */
class FakeMetricsConfigDao : MetricsConfigDao {
    private val config = MutableStateFlow<MetricsConfig?>(MetricsConfig())

    override fun getConfig(): Flow<MetricsConfig?> = config

    override suspend fun upsert(config: MetricsConfig) {
        this.config.value = config
    }

    /** Directly set config for test setup. */
    fun setConfig(cfg: MetricsConfig) {
        config.value = cfg
    }
}

/**
 * In-memory fake of [GymOrderPreferences] for unit tests.
 * No SharedPreferences dependency — stores values in plain fields.
 */
class FakeGymOrderPreferences : GymOrderPreferences {
    private var order: List<String> = emptyList()
    private var lastStatsGym: String? = null
    private var favouriteGym: String? = null
    private var lastStatsPeriod: Int? = null

    override fun getOrder(): List<String> = order
    override fun setOrder(names: List<String>) { order = names }
    override fun getLastStatsGym(): String? = lastStatsGym
    override fun setLastStatsGym(gymName: String?) { lastStatsGym = gymName }
    override fun getFavouriteGym(): String? = favouriteGym
    override fun setFavouriteGym(gymName: String?) { favouriteGym = gymName }
    override fun getLastStatsPeriod(): Int? = lastStatsPeriod
    override fun setLastStatsPeriod(months: Int?) { lastStatsPeriod = months }
}

package com.ranni.app.data.repository

import com.ranni.app.data.db.MetricsConfigDao
import com.ranni.app.data.model.MetricsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MetricsRepository(private val dao: MetricsConfigDao) {
    fun getConfig(): Flow<MetricsConfig> = dao.getConfig().map { it ?: MetricsConfig() }
    suspend fun updateConfig(config: MetricsConfig) = dao.upsert(config)
}

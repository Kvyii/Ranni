package com.ranni.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metrics_config")
data class MetricsConfig(
    @PrimaryKey val id: Int = 1,
    val months: Int = 2,
    val topK: Int = 10,
    val timelineMonths: Int = 3,
    val showClimbDots: Boolean = true,
    val showExerciseDots: Boolean = false
)

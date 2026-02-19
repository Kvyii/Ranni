package com.ranni.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "climb_logs")
data class ClimbLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val color: String,
    val score: Int,
    val loggedAt: Long = System.currentTimeMillis()
)

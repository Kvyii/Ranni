package com.ranni.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Climb type determines the score multiplier applied at log time
enum class ClimbType {
    NEW,    // 1x base score
    FLASH,  // 1.25x base score (first-try send)
    REPEAT  // 0.75x base score (re-climbed route)
}

@Entity(tableName = "climb_logs")
data class ClimbLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val color: String,                                    // Route name (e.g. "Green", "V3")
    @ColumnInfo(defaultValue = "") val gymName: String,  // Gym name — disambiguates colliding route names across gyms
    val score: Int,
    val climbType: String = ClimbType.NEW.name,
    val loggedAt: Long = System.currentTimeMillis()
)

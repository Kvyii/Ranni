package com.ranni.app.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ranni.app.R

/** Severity levels for an injury, ordered from least to most severe. */
enum class InjurySeverity {
    MILD,
    MODERATE,
    SEVERE,
    DEATH;

    /** Tint color used when rendering the skull icon for this severity. */
    val color: Color get() = when (this) {
        MILD     -> Color(0xFFAAAAAA)   // gray
        MODERATE -> Color(0xFFFFC107)   // amber
        SEVERE   -> Color(0xFFF44336)   // red
        DEATH    -> Color(0xFF9C27B0)   // purple (regal, for Valhalla)
    }

    /** Pre-colored skull drawable for this severity (no runtime tinting needed). */
    @get:DrawableRes
    val skullRes: Int get() = when (this) {
        MILD     -> R.drawable.skull_mild
        MODERATE -> R.drawable.skull_moderate
        SEVERE   -> R.drawable.skull_severe
        DEATH    -> R.drawable.skull_death
    }
}

/** A date-only injury marker. Severity is stored as its name string for Room compatibility. */
@Entity(tableName = "injury_logs")
data class InjuryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val severity: String = InjurySeverity.MILD.name,
    val loggedAt: Long = System.currentTimeMillis()
) {
    /** Convenience accessor to get the typed severity enum. */
    val severityEnum: InjurySeverity
        get() = InjurySeverity.valueOf(severity)
}

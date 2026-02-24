package com.ranni.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ranni.app.ui.theme.AppTheme

@Entity(tableName = "metrics_config")
data class MetricsConfig(
    @PrimaryKey val id: Int = 1,
    val months: Int = 2,
    val topK: Int = 10,
    val timelineMonths: Int = 3,
    val showClimbDots: Boolean = true,
    val showExerciseDots: Boolean = false,
    // Stored as the enum name string to avoid a Room TypeConverter.
    // Resolved to AppTheme via AppTheme.valueOf(uiTheme) at the call site.
    // @ColumnInfo defaultValue is required by Room's AutoMigration to generate the ALTER TABLE SQL.
    @ColumnInfo(defaultValue = "RANNI_DARK")
    val uiTheme: String = AppTheme.RANNI_DARK.name,
    // When true, REPEAT climbs are excluded from dots, graph, and stats; detail/list views are unaffected.
    // @ColumnInfo defaultValue "0" (false) is required for AutoMigration to generate ALTER TABLE SQL.
    @ColumnInfo(defaultValue = "0")
    val filterRepeats: Boolean = false,
    // When true, the Stats tab shows +/- deltas vs the preceding period of equal length.
    // Only shown when there is sufficient data (2× the selected period) and period is not Lifetime.
    @ColumnInfo(defaultValue = "0")
    val showPeriodComparison: Boolean = false
)

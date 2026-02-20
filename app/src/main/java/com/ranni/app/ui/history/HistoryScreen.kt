package com.ranni.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.climbColorMap
import com.ranni.app.data.model.climbGradeMap
import com.ranni.app.data.model.climbGymMap
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Calendar", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Progress", modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        when (selectedTab) {
            0 -> CalendarTab(viewModel)
            1 -> ProgressTab(viewModel)
        }
    }
}

@Composable
private fun CalendarTab(viewModel: HistoryViewModel) {
    val logs by viewModel.logs.collectAsState()
    val climbLogs by viewModel.climbLogs.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var logToDelete by remember { mutableStateOf<SessionLog?>(null) }
    var climbToDelete by remember { mutableStateOf<ClimbLog?>(null) }

    val sessionsByDate: Map<LocalDate, List<SessionLog>> = remember(logs) {
        logs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.completedAt),
                ZoneId.systemDefault()
            )
        }
    }

    val climbsByDate: Map<LocalDate, List<ClimbLog>> = remember(climbLogs) {
        climbLogs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.loggedAt),
                ZoneId.systemDefault()
            )
        }
    }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(1) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove \"${logToDelete!!.exerciseName}\" from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(logToDelete!!)
                    logToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (climbToDelete != null) {
        val grade = climbGradeMap[climbToDelete!!.color] ?: climbToDelete!!.color
        AlertDialog(
            onDismissRequest = { climbToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove $grade climb from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteClimb(climbToDelete!!)
                    climbToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { climbToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                Day(
                    day = day,
                    sessionLogs = sessionsByDate[day.date].orEmpty(),
                    climbLogs = climbsByDate[day.date].orEmpty(),
                    isSelected = day.date == selectedDate
                ) {
                    selectedDate = if (selectedDate == day.date) null else day.date
                }
            },
            monthHeader = { month ->
                MonthHeader(month.yearMonth, calendarState)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Box(modifier = Modifier.weight(1f)) { selectedDate?.let { date ->
            val dayLogs = sessionsByDate[date].orEmpty()
            val dayClimbs = climbsByDate[date].orEmpty()
                .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            if (dayLogs.isEmpty() && dayClimbs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing logged on this day", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dayClimbs.isNotEmpty()) {
                        item {
                            Text(
                                "Climbs",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(dayClimbs, key = { "climb-${it.id}" }) { climb ->
                            val time = Instant.ofEpochMilli(climb.loggedAt)
                                .atZone(ZoneId.systemDefault())
                                .format(timeFormatter)
                            val dotColor = climbColorMap[climb.color] ?: Color.White
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { climbToDelete = climb }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Text(climbGradeMap[climb.color] ?: climb.color, style = MaterialTheme.typography.bodyLarge)
                                        Text(climbGymMap[climb.color] ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (dayLogs.isNotEmpty()) {
                        item {
                            Text(
                                "Exercises",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(dayLogs, key = { it.id }) { log ->
                            val time = Instant.ofEpochMilli(log.completedAt)
                                .atZone(ZoneId.systemDefault())
                                .format(timeFormatter)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { logToDelete = log }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.exerciseName, style = MaterialTheme.typography.bodyLarge)
                                    Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Select a day to see activity", style = MaterialTheme.typography.bodyMedium)
            }
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTab(viewModel: HistoryViewModel) {
    val graphData by viewModel.graphData.collectAsState()
    val topClimbs by viewModel.topClimbs.collectAsState()
    val weeklyActivity by viewModel.weeklyActivity.collectAsState()
    val config by viewModel.metricsConfig.collectAsState()

    // Toggle state for showing/hiding climb and exercise dots on the graph
    var showClimbs by remember(config.showClimbDots) { mutableStateOf(config.showClimbDots) }
    var showExercises by remember(config.showExerciseDots) { mutableStateOf(config.showExerciseDots) }

    // Dots only shown on short timelines (3m/6m), too dense on 12m/24m
    val dotsEnabled = config.timelineMonths <= 6

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle chips for show/hide of dot overlays (only when dots are enabled)
        if (dotsEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                FilterChip(
                    selected = showClimbs,
                    onClick = { showClimbs = !showClimbs },
                    label = { Text("Climbs") }
                )
                FilterChip(
                    selected = showExercises,
                    onClick = { showExercises = !showExercises },
                    label = { Text("Exercises") }
                )
            }
        }

        // Graph with weekly dot overlay — top half
        MetricsGraph(
            data = graphData,
            title = "Average of Top ${config.topK} climbs over the Last ${config.months} months",
            weeklyActivity = if (dotsEnabled) weeklyActivity else emptyList(),
            showClimbs = showClimbs,
            showExercises = showExercises,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Top k climbs list — bottom half
        if (topClimbs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No climbs yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Top ${config.topK} Climbs",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                itemsIndexed(topClimbs, key = { _, climb -> climb.id }) { index, climb ->
                    // Climbs beyond topK are greyed out to show they aren't counted
                    val isCounted = index < config.topK
                    val alpha = if (isCounted) 1f else 0.4f
                    val dotColor = climbColorMap[climb.color] ?: Color.White
                    val climbDate = Instant.ofEpochMilli(climb.loggedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    // Days remaining until this climb falls out of the metric window (months * 30 days)
                    val expiryDate = climbDate.plusDays((config.months * 30).toLong())
                    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                        .coerceAtLeast(0)
                    val date = "${climbDate.format(dateFormatter)} (${daysRemaining}d)"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alpha)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Text(climbGradeMap[climb.color] ?: climb.color, style = MaterialTheme.typography.bodyLarge)
                                Text(climbGymMap[climb.color] ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    sessionLogs: List<SessionLog>,
    climbLogs: List<ClimbLog>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = isCurrentMonth, onClick = onClick)
            .padding(2.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cappedSessions = sessionLogs.take(4)
        val cappedClimbs = climbLogs
            .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            .take(4)
        if (cappedSessions.isNotEmpty() || cappedClimbs.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    cappedSessions.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    cappedClimbs.forEach { climb ->
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(climbColorMap[climb.color] ?: Color.White)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                !isCurrentMonth -> onSurface.copy(alpha = 0.3f)
                isSelected -> primaryColor
                isToday -> tertiaryColor
                else -> onSurface
            },
            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, calendarState: CalendarState) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            scope.launch { calendarState.animateScrollToMonth(yearMonth.minusMonths(1)) }
        }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = {
            scope.launch { calendarState.animateScrollToMonth(yearMonth.plusMonths(1)) }
        }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

package com.ranni.app.data.db

import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.Exercise
import com.ranni.app.data.model.SessionLog
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Seed data for development/testing.
 * Generates 3 months of realistic climb and exercise history
 * ending on Feb 18 2026, with 2-3 sessions per week.
 */

// Climb colour pool: (color name, score) matching GymData definitions
private val climbPool = listOf(
    "Green" to 75,
    "Blue" to 100,
    "Teal" to 150,
    "Pink" to 250,
    "Orange" to 450,
    "Black" to 700,
    "Purple" to 1000,
    "White" to 1300,
)

// Exercise definitions: (name, sets, setDurationSeconds or null, restDurationSeconds)
// These get inserted into the exercises table so they appear in the Exercises tab.
private val exerciseDefinitions = listOf(
    Exercise(name = "Hangboard", sets = 6, setDurationSeconds = 10, restDurationSeconds = 180),
    Exercise(name = "Pull-ups", sets = 5, setDurationSeconds = null, restDurationSeconds = 120),
    Exercise(name = "Core circuit", sets = 3, setDurationSeconds = 45, restDurationSeconds = 60),
    Exercise(name = "Deadlifts", sets = 5, setDurationSeconds = null, restDurationSeconds = 180),
)

// Exercise names to cycle through for session logs
private val exercisePool = exerciseDefinitions.map { it.name }

/**
 * Templates for how many exercises to do on a given climbing day.
 * Cycles through: 1 exercise, 2 exercises, 3 exercises, 1, 2, ...
 * This gives variety — some days are light, some are heavy training days.
 */
private val exerciseCountPattern = listOf(1, 2, 3, 1, 2)

/**
 * Day templates define the climb colours for different session types.
 * Each template represents a different training intensity.
 */
private val dayTemplates = listOf(
    // Warm-up / easy day
    listOf("Green", "Blue", "Teal", "Pink", "Orange"),
    // Push day — more mid-grade attempts
    listOf("Teal", "Pink", "Orange", "Orange", "Black"),
    // Hard day — higher grades
    listOf("Pink", "Orange", "Black", "Purple"),
    // Project day — fewer climbs, harder grades
    listOf("Orange", "Black", "Purple", "White"),
    // Volume day — lots of easier climbs
    listOf("Green", "Blue", "Teal", "Teal", "Pink", "Pink", "Orange"),
)

/**
 * Converts a LocalDate + LocalTime to epoch millis using the system timezone.
 */
private fun toEpochMs(date: LocalDate, time: LocalTime): Long {
    return date.atTime(time)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

/**
 * Seeds the database with ~3 months of climb and exercise data.
 * Covers Nov 18 2025 to Feb 18 2026, with 2-3 climbing days per week.
 * Each climbing day also has 1-3 exercise sessions logged.
 * Exercise definitions are inserted so they appear in the Exercises tab.
 */
suspend fun seedDatabase(db: AppDatabase) {
    val climbDao = db.climbLogDao()
    val sessionDao = db.sessionLogDao()
    val exerciseDao = db.exerciseDao()

    // Insert exercise definitions so they show up in the Exercises list
    exerciseDefinitions.forEach { exerciseDao.insert(it) }

    // End yesterday so seed data doesn't overlap with today's real entries
    val endDate = LocalDate.now().minusDays(1)
    val startDate = endDate.minusMonths(3)

    // Generate climbing days: iterate through the range picking 2-3 days per week.
    // We use a deterministic pattern based on day-of-week:
    // Monday (1), Wednesday (3), and Saturday (6) are climbing days.
    val climbingDays = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(endDate) }
        .filter { it.dayOfWeek.value in listOf(1, 3, 6) } // Mon, Wed, Sat
        .toList()

    climbingDays.forEachIndexed { dayIdx, date ->
        // Pick a template based on the day index for variety
        val template = dayTemplates[dayIdx % dayTemplates.size]
        val sessionStart = LocalTime.of(10, 0) // Start climbing at 10:00am

        // Insert climb logs, spaced 20 minutes apart
        template.forEachIndexed { climbIdx, color ->
            val score = climbPool.first { it.first == color }.second
            val time = sessionStart.plusMinutes((climbIdx * 20).toLong())

            climbDao.insert(
                ClimbLog(
                    color = color,
                    score = score,
                    loggedAt = toEpochMs(date, time)
                )
            )
        }

        // Determine how many exercises to log today (1, 2, or 3)
        val exerciseCount = exerciseCountPattern[dayIdx % exerciseCountPattern.size]

        // Insert exercise session logs after climbing, spaced 30 min apart.
        // Starts ~1 hour after the last climb.
        val exerciseStartTime = sessionStart
            .plusMinutes((template.size * 20 + 60).toLong())

        for (i in 0 until exerciseCount) {
            // Cycle through exercise pool so different exercises appear across days
            val exerciseName = exercisePool[(dayIdx + i) % exercisePool.size]
            val exerciseTime = exerciseStartTime.plusMinutes((i * 30).toLong())

            sessionDao.insert(
                SessionLog(
                    exerciseName = exerciseName,
                    completedAt = toEpochMs(date, exerciseTime)
                )
            )
        }
    }
}

/**
 * Clears all data from the database (climbs, sessions, exercises, metrics config).
 */
suspend fun clearDatabase(db: AppDatabase) {
    db.clearAllTables()
}

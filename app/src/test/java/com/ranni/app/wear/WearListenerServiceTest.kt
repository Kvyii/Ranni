package com.ranni.app.wear

import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.routeMap
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.ui.history.FakeClimbLogDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests the payload parsing and score computation logic that lives in WearListenerService,
 * without requiring a real device, Data Layer, or WearableListenerService lifecycle.
 */
class WearListenerServiceTest {

    // Simulates what WearListenerService.onMessageReceived() does with a payload string
    private suspend fun processPayload(payload: String, repo: ClimbRepository) {
        val parts = payload.split("|")
        if (parts.size != 3) return

        val gymName   = parts[0]
        val routeName = parts[1]
        val climbType = try { ClimbType.valueOf(parts[2]) } catch (_: IllegalArgumentException) { ClimbType.NEW }

        val baseScore = routeMap[gymName to routeName]?.score ?: return

        val score = when (climbType) {
            ClimbType.FLASH  -> (baseScore * 1.25).toInt()
            ClimbType.REPEAT -> (baseScore * 0.75).toInt()
            ClimbType.NEW    -> baseScore
        }

        repo.logClimb(color = routeName, gymName = gymName, score = score, climbType = climbType)
    }

    @Test
    fun `NEW climb logs correct base score`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("9 Degrees|Green|NEW", repo)

        val logs = dao.getAllLogs()
        var result: com.ranni.app.data.model.ClimbLog? = null
        kotlinx.coroutines.flow.first(logs) { result = it.firstOrNull(); true }

        assertNotNull("Expected a climb log to be inserted", result)
        assertEquals("Green", result!!.color)
        assertEquals("9 Degrees", result!!.gymName)
        assertEquals(75, result!!.score)           // base score for Green = 75
        assertEquals(ClimbType.NEW.name, result!!.climbType)
    }

    @Test
    fun `FLASH climb applies 1_25x multiplier`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("9 Degrees|Green|FLASH", repo)

        var result: com.ranni.app.data.model.ClimbLog? = null
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { result = it.firstOrNull(); true }

        assertEquals((75 * 1.25).toInt(), result!!.score)  // 93
        assertEquals(ClimbType.FLASH.name, result!!.climbType)
    }

    @Test
    fun `REPEAT climb applies 0_75x multiplier`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("9 Degrees|Green|REPEAT", repo)

        var result: com.ranni.app.data.model.ClimbLog? = null
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { result = it.firstOrNull(); true }

        assertEquals((75 * 0.75).toInt(), result!!.score)  // 56
        assertEquals(ClimbType.REPEAT.name, result!!.climbType)
    }

    @Test
    fun `unknown gym is silently dropped`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("Fake Gym|Green|NEW", repo)

        var count = 0
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { count = it.size; true }
        assertEquals("Unknown gym should produce no log entry", 0, count)
    }

    @Test
    fun `malformed payload is silently dropped`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("justonepart", repo)

        var count = 0
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { count = it.size; true }
        assertEquals("Malformed payload should produce no log entry", 0, count)
    }

    @Test
    fun `invalid climbType falls back to NEW`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("9 Degrees|Green|BOGUS", repo)

        var result: com.ranni.app.data.model.ClimbLog? = null
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { result = it.firstOrNull(); true }

        assertNotNull(result)
        assertEquals(75, result!!.score)                    // NEW = base score
        assertEquals(ClimbType.NEW.name, result!!.climbType)
    }

    @Test
    fun `Custom V3 NEW logs score 400`() = runTest {
        val dao  = FakeClimbLogDao()
        val repo = ClimbRepository(dao)

        processPayload("Custom|V3|NEW", repo)

        var result: com.ranni.app.data.model.ClimbLog? = null
        kotlinx.coroutines.flow.first(dao.getAllLogs()) { result = it.firstOrNull(); true }

        assertNotNull(result)
        assertEquals(400, result!!.score)
    }
}

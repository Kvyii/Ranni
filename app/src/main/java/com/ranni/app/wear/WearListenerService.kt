package com.ranni.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ranni.app.data.db.AppDatabase
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.routeMap
import com.ranni.app.data.repository.ClimbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Message path — must match MESSAGE_PATH_LOG_CLIMB in :wear ClimbMessageSender.kt
private const val MESSAGE_PATH_LOG_CLIMB = "/ranni/log_climb"

// Receives climb log messages from the paired Wear OS watch and inserts them into Room.
// Each message carries: "gymName|routeName|climbType" as a UTF-8 string.
// Score is computed here from the shared routeMap so the watch stays stateless.
class WearListenerService : WearableListenerService() {

    // Scoped to service lifetime — cancelled in onDestroy to avoid leaks
    private val serviceJob   = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Only handle climb-log messages; silently ignore any other paths
        if (messageEvent.path != MESSAGE_PATH_LOG_CLIMB) return

        // Decode pipe-delimited payload: "gymName|routeName|climbType"
        val payload = messageEvent.data.toString(Charsets.UTF_8)
        val parts   = payload.split("|")
        if (parts.size != 3) return

        val gymName   = parts[0]
        val routeName = parts[1]
        val climbType = try {
            ClimbType.valueOf(parts[2])
        } catch (_: IllegalArgumentException) {
            ClimbType.NEW
        }

        // Look up base score from the shared gym data — unknown route is silently dropped
        val baseScore = routeMap[gymName to routeName]?.score ?: return

        // Apply the same multipliers used in ClimbScreen
        val score = when (climbType) {
            ClimbType.FLASH  -> (baseScore * 1.25).toInt()
            ClimbType.REPEAT -> (baseScore * 0.75).toInt()
            ClimbType.NEW    -> baseScore
        }

        serviceScope.launch {
            val db   = AppDatabase.getInstance(applicationContext)
            val repo = ClimbRepository(db.climbLogDao())
            repo.logClimb(
                color     = routeName,
                gymName   = gymName,
                score     = score,
                climbType = climbType
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

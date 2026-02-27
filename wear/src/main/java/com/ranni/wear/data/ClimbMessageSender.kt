package com.ranni.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

// Must match the path filtered in WearListenerService.kt in the :app module
const val MESSAGE_PATH_LOG_CLIMB = "/ranni/log_climb"

// Sends a climb log message to all currently connected nodes (the paired phone).
// Payload format: "gymName|routeName|climbType" as UTF-8.
// Pipe delimiter is safe — no gym or route name contains a pipe character.
// Returns true if the message was delivered to at least one node; false on error or no connection.
suspend fun sendClimbToPhone(
    context: Context,
    gymName: String,
    routeName: String,
    climbType: String
): Boolean {
    return try {
        val nodeClient    = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)
        val nodes         = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) return false

        val payload = "$gymName|$routeName|$climbType".toByteArray(Charsets.UTF_8)
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, MESSAGE_PATH_LOG_CLIMB, payload).await()
        }
        true
    } catch (_: Exception) {
        false
    }
}

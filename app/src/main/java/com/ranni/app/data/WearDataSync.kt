package com.ranni.app.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

// Data Layer path + key — must match FavouriteGymReader.kt in the :wear module
internal const val DATA_PATH_FAVOURITE_GYM = "/ranni/favourite_gym"
internal const val DATA_KEY_GYM_NAME       = "gymName"

// Writes the favourite gym name to the Wearable Data Layer so the watch can read it.
// Pass null to delete the DataItem (watch will show the gym picker instead of routing directly).
// Uses setUrgent() to ensure near-immediate delivery rather than the default up-to-30-min batch window.
// Safe to call when watch is not connected — the item will be delivered on next connection.
suspend fun syncFavouriteGymToWatch(context: Context, gymName: String?) {
    try {
        val dataClient = Wearable.getDataClient(context)
        if (gymName == null) {
            // Delete the DataItem so the watch sees no favourite
            val uri = Uri.Builder()
                .scheme("wear")
                .path(DATA_PATH_FAVOURITE_GYM)
                .build()
            dataClient.deleteDataItems(uri).await()
        } else {
            // Write the new favourite gym name as an urgent DataItem
            val req = PutDataMapRequest.create(DATA_PATH_FAVOURITE_GYM).apply {
                dataMap.putString(DATA_KEY_GYM_NAME, gymName)
                setUrgent()
            }.asPutDataRequest()
            dataClient.putDataItem(req).await()
        }
    } catch (_: Exception) {
        // Watch not connected or Play Services unavailable — item will sync on next connection
    }
}

package com.ranni.wear.data

import android.content.Context
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

// Must match the path and key written by WearDataSync.kt in the :app module
private const val DATA_PATH_FAVOURITE_GYM = "/ranni/favourite_gym"
private const val DATA_KEY_GYM_NAME       = "gymName"

// Reads the favourite gym name from the phone's DataClient DataItem.
// Returns null if the item does not exist (no favourite set) or the watch is not connected.
// Called once on ViewModel init; the watch does not subscribe to live updates.
suspend fun readFavouriteGym(context: Context): String? {
    return try {
        val dataClient = Wearable.getDataClient(context)
        val uri = android.net.Uri.Builder()
            .scheme("wear")
            .path(DATA_PATH_FAVOURITE_GYM)
            .build()
        val buffer = dataClient.getDataItems(uri).await()
        val result = if (buffer.count > 0) {
            DataMapItem.fromDataItem(buffer.get(0)).dataMap.getString(DATA_KEY_GYM_NAME)
        } else null
        buffer.release()
        result
    } catch (_: Exception) {
        // Phone not connected or Play Services unavailable — treat as no favourite
        null
    }
}

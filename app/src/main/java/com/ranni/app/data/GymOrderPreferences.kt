package com.ranni.app.data

import android.content.Context

/**
 * SharedPreferences wrapper for the user-defined gym display order.
 * Stores a comma-separated ordered list of active gym names (e.g. "Custom,9 Degrees").
 * Coming-soon gyms are not stored here — they always appear after the active gyms.
 */
class GymOrderPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the saved ordered list of active gym names, or empty list if never set. */
    fun getOrder(): List<String> {
        val raw = prefs.getString(KEY_GYM_ORDER, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    /** Saves the ordered list of active gym names. */
    fun setOrder(names: List<String>) {
        prefs.edit().putString(KEY_GYM_ORDER, names.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_NAME = "gym_order_prefs"
        private const val KEY_GYM_ORDER = "gym_order"
    }
}

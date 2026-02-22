package com.ranni.app.data

import android.content.Context

/**
 * Contract for reading/writing the user-defined gym display order,
 * the last-selected Stats tab gym, the starred favourite gym,
 * and the last-selected Stats tab period filter.
 */
interface GymOrderPreferences {
    /** Returns the saved ordered list of active gym names, or empty list if never set. */
    fun getOrder(): List<String>

    /** Saves the ordered list of active gym names. */
    fun setOrder(names: List<String>)

    /** Returns the gym name last selected in the Stats tab, or null if never set. */
    fun getLastStatsGym(): String?

    /** Persists the gym name selected in the Stats tab; null clears the saved value. */
    fun setLastStatsGym(gymName: String?)

    /** Returns the starred favourite gym name for the Climb tab, or null if none is set. */
    fun getFavouriteGym(): String?

    /** Persists the favourite gym name; null clears the saved value. */
    fun setFavouriteGym(gymName: String?)

    /** Returns the Stats tab period (months) last selected by the user, or null if never set (= Lifetime). */
    fun getLastStatsPeriod(): Int?

    /** Persists the Stats tab period selection; null means Lifetime. */
    fun setLastStatsPeriod(months: Int?)
}

/**
 * SharedPreferences-backed implementation of [GymOrderPreferences].
 * Stores a comma-separated ordered list of active gym names (e.g. "Custom,9 Degrees").
 * Coming-soon gyms are not stored here — they always appear after the active gyms.
 */
class SharedPrefsGymOrderPreferences(context: Context) : GymOrderPreferences {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getOrder(): List<String> {
        val raw = prefs.getString(KEY_GYM_ORDER, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    override fun setOrder(names: List<String>) {
        prefs.edit().putString(KEY_GYM_ORDER, names.joinToString(",")).apply()
    }

    override fun getLastStatsGym(): String? = prefs.getString(KEY_STATS_GYM, null)

    override fun setLastStatsGym(gymName: String?) {
        val edit = prefs.edit()
        if (gymName != null) edit.putString(KEY_STATS_GYM, gymName)
        else edit.remove(KEY_STATS_GYM)
        edit.apply()
    }

    override fun getFavouriteGym(): String? = prefs.getString(KEY_FAVOURITE_GYM, null)

    override fun setFavouriteGym(gymName: String?) {
        val edit = prefs.edit()
        if (gymName != null) edit.putString(KEY_FAVOURITE_GYM, gymName)
        else edit.remove(KEY_FAVOURITE_GYM)
        edit.apply()
    }

    // Returns INT_MIN sentinel as "not set"; convert back to null for Lifetime.
    override fun getLastStatsPeriod(): Int? {
        val stored = prefs.getInt(KEY_STATS_PERIOD, Int.MIN_VALUE)
        return if (stored == Int.MIN_VALUE) null else stored
    }

    override fun setLastStatsPeriod(months: Int?) {
        val edit = prefs.edit()
        if (months != null) edit.putInt(KEY_STATS_PERIOD, months)
        else edit.remove(KEY_STATS_PERIOD)
        edit.apply()
    }

    companion object {
        private const val PREFS_NAME = "gym_order_prefs"
        private const val KEY_GYM_ORDER = "gym_order"
        private const val KEY_STATS_GYM = "stats_gym"
        private const val KEY_FAVOURITE_GYM = "favourite_gym"
        private const val KEY_STATS_PERIOD = "stats_period_months"
    }
}

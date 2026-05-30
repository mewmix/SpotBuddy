package com.mewmix.spotbuddy

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedWorkoutItem(
    val name: String,
    val selected: Boolean,
    val sets: Int,
    val reps: Int,
    val holdSeconds: Int,
    val completed: Int
)

data class SavedAppState(
    val items: List<SavedWorkoutItem>,
    val phase: String,
    val currentIndex: Int,
    val restSeconds: Int,
    val remainingSeconds: Int,
    val sessionStartedAt: Long,
    val actualCooldownSeconds: Int,
    val skippedCooldowns: Int,
    val skippedCooldownSeconds: Int,
    val sessionEndedEarly: Boolean
)

class SpotBuddyPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("spotbuddy_preferences", Context.MODE_PRIVATE)

    fun save(state: SavedAppState) {
        val root = JSONObject()
            .put("phase", state.phase)
            .put("currentIndex", state.currentIndex)
            .put("restSeconds", state.restSeconds)
            .put("remainingSeconds", state.remainingSeconds)
            .put("sessionStartedAt", state.sessionStartedAt)
            .put("actualCooldownSeconds", state.actualCooldownSeconds)
            .put("skippedCooldowns", state.skippedCooldowns)
            .put("skippedCooldownSeconds", state.skippedCooldownSeconds)
            .put("sessionEndedEarly", state.sessionEndedEarly)
            .put("items", JSONArray().apply {
                state.items.forEach { item ->
                    put(
                        JSONObject()
                            .put("name", item.name)
                            .put("selected", item.selected)
                            .put("sets", item.sets)
                            .put("reps", item.reps)
                            .put("holdSeconds", item.holdSeconds)
                            .put("completed", item.completed)
                    )
                }
            })

        prefs.edit().putString(KEY_STATE, root.toString()).apply()
    }

    fun load(): SavedAppState? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val items = root.getJSONArray("items")
            SavedAppState(
                items = List(items.length()) { index ->
                    val item = items.getJSONObject(index)
                    SavedWorkoutItem(
                        name = item.getString("name"),
                        selected = item.getBoolean("selected"),
                        sets = item.getInt("sets"),
                        reps = item.getInt("reps"),
                        holdSeconds = item.getInt("holdSeconds"),
                        completed = item.optInt("completed", 0)
                    )
                },
                phase = root.optString("phase", "Setup"),
                currentIndex = root.optInt("currentIndex", 0),
                restSeconds = root.optInt("restSeconds", 45),
                remainingSeconds = root.optInt("remainingSeconds", 45),
                sessionStartedAt = root.optLong("sessionStartedAt", 0L),
                actualCooldownSeconds = root.optInt("actualCooldownSeconds", 0),
                skippedCooldowns = root.optInt("skippedCooldowns", 0),
                skippedCooldownSeconds = root.optInt("skippedCooldownSeconds", 0),
                sessionEndedEarly = root.optBoolean("sessionEndedEarly", false)
            )
        }.getOrNull()
    }

    private companion object {
        const val KEY_STATE = "state"
    }
}

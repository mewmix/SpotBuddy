package com.mewmix.spotbuddy

import org.json.JSONArray
import org.json.JSONObject

data class SpotBuddyBackupData(
    val sessions: List<SessionRecord>,
    val savedState: SavedAppState?
)

object SpotBuddyBackup {
    private const val FORMAT_VERSION = 1

    fun encode(sessions: List<SessionRecord>, savedState: SavedAppState?): String {
        return JSONObject()
            .put("app", "SpotBuddy")
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("savedState", savedState?.let(::encodeState) ?: JSONObject.NULL)
            .put("sessions", JSONArray().apply {
                sessions.forEach { put(encodeSession(it)) }
            })
            .toString(2)
    }

    fun decode(raw: String): SpotBuddyBackupData {
        val root = JSONObject(raw)
        require(root.optString("app") == "SpotBuddy") { "Not a SpotBuddy backup" }
        require(root.optInt("formatVersion") == FORMAT_VERSION) { "Unsupported backup version" }
        val sessionsJson = root.getJSONArray("sessions")
        return SpotBuddyBackupData(
            sessions = List(sessionsJson.length()) { index -> decodeSession(sessionsJson.getJSONObject(index)) },
            savedState = if (root.isNull("savedState")) null else decodeState(root.getJSONObject("savedState"))
        )
    }

    private fun encodeSession(session: SessionRecord): JSONObject {
        return JSONObject()
            .put("startedAt", session.startedAt)
            .put("endedAt", session.endedAt)
            .put("durationSeconds", session.durationSeconds)
            .put("completedSets", session.completedSets)
            .put("plannedSets", session.plannedSets)
            .put("actualCooldownSeconds", session.actualCooldownSeconds)
            .put("skippedCooldowns", session.skippedCooldowns)
            .put("skippedCooldownSeconds", session.skippedCooldownSeconds)
            .put("endedEarly", session.endedEarly)
            .put("exercises", JSONArray().apply {
                session.exercises.forEach { put(encodeExercise(it)) }
            })
    }

    private fun decodeSession(item: JSONObject): SessionRecord {
        val exercisesJson = item.getJSONArray("exercises")
        return SessionRecord(
            id = 0L,
            startedAt = item.getLong("startedAt"),
            endedAt = item.getLong("endedAt"),
            durationSeconds = item.getInt("durationSeconds"),
            completedSets = item.getInt("completedSets"),
            plannedSets = item.getInt("plannedSets"),
            actualCooldownSeconds = item.getInt("actualCooldownSeconds"),
            skippedCooldowns = item.getInt("skippedCooldowns"),
            skippedCooldownSeconds = item.getInt("skippedCooldownSeconds"),
            endedEarly = item.optBoolean("endedEarly", false),
            exercises = List(exercisesJson.length()) { index -> decodeExercise(exercisesJson.getJSONObject(index)) }
        )
    }

    private fun encodeExercise(exercise: ExerciseSummary): JSONObject {
        return JSONObject()
            .put("name", exercise.name)
            .put("completedSets", exercise.completedSets)
            .put("plannedSets", exercise.plannedSets)
            .put("reps", exercise.reps)
            .put("holdSeconds", exercise.holdSeconds)
    }

    private fun decodeExercise(item: JSONObject): ExerciseSummary {
        return ExerciseSummary(
            name = item.getString("name"),
            completedSets = item.getInt("completedSets"),
            plannedSets = item.getInt("plannedSets"),
            reps = item.getInt("reps"),
            holdSeconds = item.getInt("holdSeconds")
        )
    }

    private fun encodeState(state: SavedAppState): JSONObject {
        return JSONObject()
            .put("phase", state.phase)
            .put("currentIndex", state.currentIndex)
            .put("restSeconds", state.restSeconds)
            .put("remainingSeconds", state.remainingSeconds)
            .put("activeTimerSeconds", state.activeTimerSeconds)
            .put("sessionStartedAt", state.sessionStartedAt)
            .put("actualCooldownSeconds", state.actualCooldownSeconds)
            .put("skippedCooldowns", state.skippedCooldowns)
            .put("skippedCooldownSeconds", state.skippedCooldownSeconds)
            .put("sessionEndedEarly", state.sessionEndedEarly)
            .put("savedSessionId", state.savedSessionId)
            .put("items", JSONArray().apply {
                state.items.forEach { put(encodeWorkoutItem(it)) }
            })
    }

    private fun decodeState(root: JSONObject): SavedAppState {
        val items = root.getJSONArray("items")
        return SavedAppState(
            items = List(items.length()) { index -> decodeWorkoutItem(items.getJSONObject(index)) },
            phase = root.optString("phase", "Setup"),
            currentIndex = root.optInt("currentIndex", 0),
            restSeconds = root.optInt("restSeconds", 45),
            remainingSeconds = root.optInt("remainingSeconds", 45),
            activeTimerSeconds = root.optInt("activeTimerSeconds", 0),
            sessionStartedAt = root.optLong("sessionStartedAt", 0L),
            actualCooldownSeconds = root.optInt("actualCooldownSeconds", 0),
            skippedCooldowns = root.optInt("skippedCooldowns", 0),
            skippedCooldownSeconds = root.optInt("skippedCooldownSeconds", 0),
            sessionEndedEarly = root.optBoolean("sessionEndedEarly", false),
            savedSessionId = root.optLong("savedSessionId", 0L)
        )
    }

    private fun encodeWorkoutItem(item: SavedWorkoutItem): JSONObject {
        return JSONObject()
            .put("name", item.name)
            .put("mode", item.mode)
            .put("selected", item.selected)
            .put("sets", item.sets)
            .put("reps", item.reps)
            .put("holdSeconds", item.holdSeconds)
            .put("completed", item.completed)
    }

    private fun decodeWorkoutItem(item: JSONObject): SavedWorkoutItem {
        return SavedWorkoutItem(
            name = item.getString("name"),
            mode = item.optString("mode", "Reps"),
            selected = item.getBoolean("selected"),
            sets = item.getInt("sets"),
            reps = item.getInt("reps"),
            holdSeconds = item.getInt("holdSeconds"),
            completed = item.optInt("completed", 0)
        )
    }
}

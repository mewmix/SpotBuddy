package com.mewmix.spotbuddy

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

data class ExerciseSummary(
    val name: String,
    val completedSets: Int,
    val plannedSets: Int,
    val reps: Int,
    val holdSeconds: Int
)

data class SessionRecord(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val completedSets: Int,
    val plannedSets: Int,
    val actualCooldownSeconds: Int,
    val skippedCooldowns: Int,
    val skippedCooldownSeconds: Int,
    val exercises: List<ExerciseSummary>
)

class SpotBuddyDatabase(context: Context) :
    SQLiteOpenHelper(context, "spotbuddy.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at INTEGER NOT NULL,
                ended_at INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                completed_sets INTEGER NOT NULL,
                planned_sets INTEGER NOT NULL,
                actual_cooldown_seconds INTEGER NOT NULL,
                skipped_cooldowns INTEGER NOT NULL,
                skipped_cooldown_seconds INTEGER NOT NULL,
                exercises_json TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sessions")
        onCreate(db)
    }

    fun insertSession(record: SessionRecord): Long {
        val values = ContentValues().apply {
            put("started_at", record.startedAt)
            put("ended_at", record.endedAt)
            put("duration_seconds", record.durationSeconds)
            put("completed_sets", record.completedSets)
            put("planned_sets", record.plannedSets)
            put("actual_cooldown_seconds", record.actualCooldownSeconds)
            put("skipped_cooldowns", record.skippedCooldowns)
            put("skipped_cooldown_seconds", record.skippedCooldownSeconds)
            put("exercises_json", encodeExercises(record.exercises))
        }
        return writableDatabase.insert("sessions", null, values)
    }

    fun getSessions(): List<SessionRecord> {
        val sessions = mutableListOf<SessionRecord>()
        readableDatabase.query(
            "sessions",
            null,
            null,
            null,
            null,
            null,
            "started_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                sessions += SessionRecord(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    startedAt = cursor.getLong(cursor.getColumnIndexOrThrow("started_at")),
                    endedAt = cursor.getLong(cursor.getColumnIndexOrThrow("ended_at")),
                    durationSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("duration_seconds")),
                    completedSets = cursor.getInt(cursor.getColumnIndexOrThrow("completed_sets")),
                    plannedSets = cursor.getInt(cursor.getColumnIndexOrThrow("planned_sets")),
                    actualCooldownSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("actual_cooldown_seconds")),
                    skippedCooldowns = cursor.getInt(cursor.getColumnIndexOrThrow("skipped_cooldowns")),
                    skippedCooldownSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("skipped_cooldown_seconds")),
                    exercises = decodeExercises(cursor.getString(cursor.getColumnIndexOrThrow("exercises_json")))
                )
            }
        }
        return sessions
    }

    fun deleteSession(id: Long) {
        writableDatabase.delete("sessions", "id = ?", arrayOf(id.toString()))
    }

    private fun encodeExercises(exercises: List<ExerciseSummary>): String {
        val array = JSONArray()
        exercises.forEach { exercise ->
            array.put(
                org.json.JSONObject()
                    .put("name", exercise.name)
                    .put("completedSets", exercise.completedSets)
                    .put("plannedSets", exercise.plannedSets)
                    .put("reps", exercise.reps)
                    .put("holdSeconds", exercise.holdSeconds)
            )
        }
        return array.toString()
    }

    private fun decodeExercises(json: String): List<ExerciseSummary> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ExerciseSummary(
                name = item.getString("name"),
                completedSets = item.getInt("completedSets"),
                plannedSets = item.getInt("plannedSets"),
                reps = item.getInt("reps"),
                holdSeconds = item.getInt("holdSeconds")
            )
        }
    }
}

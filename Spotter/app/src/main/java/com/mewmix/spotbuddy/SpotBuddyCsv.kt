package com.mewmix.spotbuddy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SpotBuddyCsv {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun encodeSessions(sessions: List<SessionRecord>): String {
        val rows = mutableListOf<List<String>>()
        rows += listOf(
            "session_id",
            "started_at",
            "started_at_ms",
            "ended_at",
            "ended_at_ms",
            "duration_seconds",
            "session_completed_sets",
            "session_planned_sets",
            "actual_cooldown_seconds",
            "skipped_cooldowns",
            "skipped_cooldown_seconds",
            "ended_early",
            "exercise_name",
            "exercise_completed_sets",
            "exercise_planned_sets",
            "reps",
            "hold_seconds"
        )
        sessions.forEach { session ->
            if (session.exercises.isEmpty()) {
                rows += sessionColumns(session) + listOf("", "", "", "", "")
            } else {
                session.exercises.forEach { exercise ->
                    rows += sessionColumns(session) + listOf(
                        exercise.name,
                        exercise.completedSets.toString(),
                        exercise.plannedSets.toString(),
                        exercise.reps.toString(),
                        exercise.holdSeconds.toString()
                    )
                }
            }
        }
        return rows.joinToString(separator = "\n", postfix = "\n") { row ->
            row.joinToString(separator = ",", transform = ::escapeCell)
        }
    }

    private fun sessionColumns(session: SessionRecord): List<String> {
        return listOf(
            session.id.toString(),
            dateFormat.format(Date(session.startedAt)),
            session.startedAt.toString(),
            dateFormat.format(Date(session.endedAt)),
            session.endedAt.toString(),
            session.durationSeconds.toString(),
            session.completedSets.toString(),
            session.plannedSets.toString(),
            session.actualCooldownSeconds.toString(),
            session.skippedCooldowns.toString(),
            session.skippedCooldownSeconds.toString(),
            session.endedEarly.toString()
        )
    }

    private fun escapeCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}

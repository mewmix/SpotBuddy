package com.mewmix.spotbuddy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SpotBuddyCsv {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val requiredHeaders = listOf(
        "session_id",
        "started_at_ms",
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

    fun decodeSessions(raw: String): List<SessionRecord> {
        val rows = parseRows(raw).filter { row -> row.any { it.isNotBlank() } }
        require(rows.isNotEmpty()) { "CSV is empty" }

        val headers = rows.first().map { it.trim() }
        val indexes = headers.withIndex().associate { it.value to it.index }
        requiredHeaders.forEach { header ->
            require(indexes.containsKey(header)) { "Missing CSV column: $header" }
        }

        return rows.drop(1)
            .filter { it.any(String::isNotBlank) }
            .groupBy { row ->
                cell(row, indexes, "session_id").ifBlank {
                    "${cell(row, indexes, "started_at_ms")}-${cell(row, indexes, "ended_at_ms")}"
                }
            }
            .values
            .map { group -> decodeSessionGroup(group, indexes) }
            .sortedByDescending { it.startedAt }
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

    private fun decodeSessionGroup(
        rows: List<List<String>>,
        indexes: Map<String, Int>
    ): SessionRecord {
        val first = rows.first()
        val exercises = rows.mapNotNull { row ->
            val name = cell(row, indexes, "exercise_name")
            if (name.isBlank()) {
                null
            } else {
                ExerciseSummary(
                    name = name,
                    completedSets = intCell(row, indexes, "exercise_completed_sets"),
                    plannedSets = intCell(row, indexes, "exercise_planned_sets"),
                    reps = intCell(row, indexes, "reps"),
                    holdSeconds = intCell(row, indexes, "hold_seconds")
                )
            }
        }

        return SessionRecord(
            id = 0L,
            startedAt = longCell(first, indexes, "started_at_ms"),
            endedAt = longCell(first, indexes, "ended_at_ms"),
            durationSeconds = intCell(first, indexes, "duration_seconds"),
            completedSets = intCell(first, indexes, "session_completed_sets"),
            plannedSets = intCell(first, indexes, "session_planned_sets"),
            actualCooldownSeconds = intCell(first, indexes, "actual_cooldown_seconds"),
            skippedCooldowns = intCell(first, indexes, "skipped_cooldowns"),
            skippedCooldownSeconds = intCell(first, indexes, "skipped_cooldown_seconds"),
            endedEarly = boolCell(first, indexes, "ended_early"),
            exercises = exercises
        )
    }

    private fun parseRows(raw: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < raw.length) {
            val char = raw[index]
            when {
                inQuotes && char == '"' && raw.getOrNull(index + 1) == '"' -> {
                    currentCell.append('"')
                    index += 1
                }
                char == '"' -> inQuotes = !inQuotes
                !inQuotes && char == ',' -> {
                    currentRow += currentCell.toString()
                    currentCell.clear()
                }
                !inQuotes && (char == '\n' || char == '\r') -> {
                    if (char == '\r' && raw.getOrNull(index + 1) == '\n') index += 1
                    currentRow += currentCell.toString()
                    currentCell.clear()
                    rows += currentRow.toList()
                    currentRow.clear()
                }
                else -> currentCell.append(char)
            }
            index += 1
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += currentCell.toString()
            rows += currentRow.toList()
        }
        require(!inQuotes) { "CSV contains an unterminated quoted value" }
        return rows
    }

    private fun cell(row: List<String>, indexes: Map<String, Int>, header: String): String {
        return row.getOrNull(indexes.getValue(header)).orEmpty()
    }

    private fun intCell(row: List<String>, indexes: Map<String, Int>, header: String): Int {
        return cell(row, indexes, header).toIntOrNull() ?: 0
    }

    private fun longCell(row: List<String>, indexes: Map<String, Int>, header: String): Long {
        return cell(row, indexes, header).toLongOrNull() ?: 0L
    }

    private fun boolCell(row: List<String>, indexes: Map<String, Int>, header: String): Boolean {
        return cell(row, indexes, header).equals("true", ignoreCase = true)
    }
}

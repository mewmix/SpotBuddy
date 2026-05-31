package com.mewmix.spotbuddy

internal fun normalizeWorkoutName(name: String): String {
    return name.trim().replace(Regex("\\s+"), " ")
}

internal fun shortWorkoutName(name: String): String {
    return name.filter { it.isLetterOrDigit() }
        .take(4)
        .uppercase()
        .ifBlank { "MOVE" }
}

internal fun clampManualDayOffset(offset: Int): Int = offset.coerceIn(-365, 0)

internal fun clampManualDurationMinutes(minutes: Int): Int = minutes.coerceIn(0, 600)

internal fun manualDurationSeconds(minutes: Int): Int = clampManualDurationMinutes(minutes) * 60


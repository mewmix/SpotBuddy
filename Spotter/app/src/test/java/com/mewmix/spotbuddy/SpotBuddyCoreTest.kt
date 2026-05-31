package com.mewmix.spotbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotBuddyCoreTest {
    @Test
    fun normalizeWorkoutNameTrimsAndCollapsesWhitespace() {
        assertEquals("Wall Sits", normalizeWorkoutName("  Wall   Sits  "))
    }

    @Test
    fun shortWorkoutNameUsesFirstFourLettersOrFallback() {
        assertEquals("PULL", shortWorkoutName("Pull-ups"))
        assertEquals("MOVE", shortWorkoutName(" !!! "))
    }

    @Test
    fun manualEntryClampsPastDateOffset() {
        assertEquals(0, clampManualDayOffset(7))
        assertEquals(-365, clampManualDayOffset(-500))
        assertEquals(-14, clampManualDayOffset(-14))
    }

    @Test
    fun manualDurationSecondsClampsToSupportedRange() {
        assertEquals(0, manualDurationSeconds(-5))
        assertEquals(1_800, manualDurationSeconds(30))
        assertEquals(36_000, manualDurationSeconds(900))
    }

    @Test
    fun csvExportFlattensSessionsAndEscapesCells() {
        val csv = SpotBuddyCsv.encodeSessions(
            listOf(
                SessionRecord(
                    id = 7L,
                    startedAt = 1_700_000_000_000L,
                    endedAt = 1_700_000_060_000L,
                    durationSeconds = 60,
                    completedSets = 2,
                    plannedSets = 3,
                    actualCooldownSeconds = 30,
                    skippedCooldowns = 1,
                    skippedCooldownSeconds = 15,
                    endedEarly = true,
                    exercises = listOf(
                        ExerciseSummary(
                            name = "Pushups, wide",
                            completedSets = 2,
                            plannedSets = 3,
                            reps = 12,
                            holdSeconds = 0
                        )
                    )
                )
            )
        )

        assert(csv.startsWith("session_id,started_at,started_at_ms"))
        assert(csv.contains("7,"))
        assert(csv.contains("\"Pushups, wide\",2,3,12,0"))
        assert(csv.endsWith("\n"))
    }
}

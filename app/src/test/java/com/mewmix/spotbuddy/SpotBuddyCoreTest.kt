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

    @Test
    fun csvImportRestoresExportedSessions() {
        val original = SessionRecord(
            id = 9L,
            startedAt = 1_700_010_000_000L,
            endedAt = 1_700_010_300_000L,
            durationSeconds = 300,
            completedSets = 5,
            plannedSets = 6,
            actualCooldownSeconds = 90,
            skippedCooldowns = 1,
            skippedCooldownSeconds = 45,
            endedEarly = false,
            exercises = listOf(
                ExerciseSummary("Pushups", 3, 3, 15, 0),
                ExerciseSummary("Planks", 2, 3, 0, 45)
            )
        )

        val imported = SpotBuddyCsv.decodeSessions(SpotBuddyCsv.encodeSessions(listOf(original)))

        assertEquals(1, imported.size)
        assertEquals(original.startedAt, imported.first().startedAt)
        assertEquals(original.endedAt, imported.first().endedAt)
        assertEquals(original.completedSets, imported.first().completedSets)
        assertEquals(original.skippedCooldownSeconds, imported.first().skippedCooldownSeconds)
        assertEquals(original.exercises, imported.first().exercises)
    }

    @Test
    fun csvImportParsesQuotedExerciseNames() {
        val csv = listOf(
            "session_id,started_at,started_at_ms,ended_at,ended_at_ms,duration_seconds,session_completed_sets,session_planned_sets,actual_cooldown_seconds,skipped_cooldowns,skipped_cooldown_seconds,ended_early,exercise_name,exercise_completed_sets,exercise_planned_sets,reps,hold_seconds",
            "1,2023-01-01 10:00:00,1000,2023-01-01 10:10:00,2000,60,1,1,0,0,0,false,\"Pullups, \"\"wide\"\"\",1,1,8,0"
        ).joinToString("\n")

        val imported = SpotBuddyCsv.decodeSessions(csv)

        assertEquals("Pullups, \"wide\"", imported.first().exercises.first().name)
        assertEquals(8, imported.first().exercises.first().reps)
    }
}

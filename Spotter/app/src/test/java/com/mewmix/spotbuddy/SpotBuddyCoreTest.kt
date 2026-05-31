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
}

package com.javaide.mobile.practice.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DifficultyTest {

    @Test
    fun `fromString parses a valid value case-insensitively`() {
        assertEquals(Difficulty.EASY, Difficulty.fromString("EASY"))
        assertEquals(Difficulty.MEDIUM, Difficulty.fromString("medium"))
        assertEquals(Difficulty.HARD, Difficulty.fromString("Hard"))
    }

    @Test
    fun `fromString returns null for an unsupported difficulty`() {
        assertNull(Difficulty.fromString("EXPERT"))
        assertNull(Difficulty.fromString(""))
        assertNull(Difficulty.fromString("mediumm"))
    }
}

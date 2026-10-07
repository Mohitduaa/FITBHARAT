package com.example

import com.example.data.model.StreakCalculator
import com.example.data.model.StreakType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakCalculatorTest {
    private val today = LocalDate(2026, 10, 5)

    private fun days(vararg d: Int) = d.map { LocalDate(2026, 10, it) }.toSet()

    @Test
    fun `streak counts consecutive days ending today`() {
        val info = StreakCalculator.compute(StreakType.STEPS, days(3, 4, 5), today)
        assertEquals(3, info.current)
        assertTrue(info.achievedToday)
    }

    @Test
    fun `streak survives while today is still in progress`() {
        val info = StreakCalculator.compute(StreakType.STEPS, days(2, 3, 4), today)
        assertEquals(3, info.current)
        assertFalse(info.achievedToday)
    }

    @Test
    fun `a missed day breaks the current streak but best is kept`() {
        val info = StreakCalculator.compute(StreakType.WATER, days(1, 2, 3, 4, 5).minus(days(4)), today)
        assertEquals(1, info.current)
        assertEquals(3, info.best)
    }

    @Test
    fun `no achieved days gives zero`() {
        val info = StreakCalculator.compute(StreakType.LOGGING, emptySet(), today)
        assertEquals(0, info.current)
        assertEquals(0, info.best)
    }

    @Test
    fun `streak older than yesterday is over`() {
        val info = StreakCalculator.compute(StreakType.PROTEIN, days(1, 2, 3), today)
        assertEquals(0, info.current)
        assertEquals(3, info.best)
    }
}

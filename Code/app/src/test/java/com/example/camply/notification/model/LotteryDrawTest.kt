package com.example.camply.lottery.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.random.Random

class LotteryDrawTest {
    private val endDate = LocalDate.parse("2026-11-10")

    @Test
    fun beforeElevenFiftyNineIsNotDue() {
        val now = ZonedDateTime.of(endDate, LocalTime.of(23, 58), ALBERTA)
        assertFalse(isDrawDue("2026-11-10", now))
    }

    @Test
    fun elevenFiftyNineOnLotteryEndIsDue() {
        val now = ZonedDateTime.of(endDate, LocalTime.of(23, 59), ALBERTA)
        assertTrue(isDrawDue("2026-11-10", now))
    }

    @Test
    fun theDayAfterLotteryEndIsDue() {
        val now = ZonedDateTime.of(endDate.plusDays(1), LocalTime.MIDNIGHT, ALBERTA)
        assertTrue(isDrawDue("2026-11-10", now))
    }

    @Test
    fun invalidLotteryEndIsNotDue() {
        val now = ZonedDateTime.of(endDate, LocalTime.of(23, 59), ALBERTA)
        assertFalse(isDrawDue("2026-11-5", now))
    }

    @Test
    fun fewerEntrantsThanWinnerCountAllWin() {
        val winners = selectWinners(listOf("a", "b"), winnerCount = 5, Random(1))
        assertEquals(setOf("a", "b"), winners)
    }

    @Test
    fun winnerCountSelectsThatManyEntrants() {
        val waiting = listOf("a", "b", "c", "d")
        val first = selectWinners(waiting, winnerCount = 2, Random(1))
        val second = selectWinners(waiting, winnerCount = 2, Random(1))
        assertEquals(2, first.size)
        assertEquals(first, second)
        assertTrue(waiting.containsAll(first))
    }

    @Test
    fun nobodyWaitingSelectsNobody() {
        assertEquals(emptySet<String>(), selectWinners(emptyList(), winnerCount = 3, Random(1)))
    }
}
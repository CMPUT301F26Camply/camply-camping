package com.example.camply.review

import com.example.camply.review.model.RatingSummary
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests the running total that lets a rating and the campsite average be saved atomically (US 01.13.01). */
class RatingSummaryTest {

    @Test
    fun emptySummary_averageIsZero() = assertEquals(0.0, RatingSummary().average(), 0.0)

    @Test
    fun firstRating_addsToSumAndCount() {
        val summary = RatingSummary().withRating(previous = null, value = 4)

        assertEquals(RatingSummary(sum = 4, count = 1), summary)
        assertEquals(4.0, summary.average(), 0.0001)
    }

    @Test
    fun secondEntrant_increasesCount() {
        val summary = RatingSummary(sum = 4, count = 1).withRating(previous = null, value = 2)

        assertEquals(RatingSummary(sum = 6, count = 2), summary)
        assertEquals(3.0, summary.average(), 0.0001)
    }

    @Test
    fun changingOwnRating_replacesValueWithoutChangingCount() {
        val summary = RatingSummary(sum = 6, count = 2).withRating(previous = 2, value = 5)

        assertEquals(RatingSummary(sum = 9, count = 2), summary)
        assertEquals(4.5, summary.average(), 0.0001)
    }

    @Test
    fun sameRatingAgain_leavesSummaryUnchanged() {
        val before = RatingSummary(sum = 9, count = 2)

        assertEquals(before, before.withRating(previous = 5, value = 5))
    }
}
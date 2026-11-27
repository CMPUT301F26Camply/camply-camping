package com.example.camply.review

import com.example.camply.review.model.Rating
import com.example.camply.review.model.Review
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests rating maths and review validation (US 01.13.01, US 01.13.02). */
class RatingAndReviewModelTest {

    @Test
    fun average_noRatings_isZero() = assertEquals(0.0, Rating.average(emptyList()), 0.0)

    @Test
    fun average_mixedRatings_isMean() {
        val ratings = listOf(Rating("a", 5), Rating("b", 4), Rating("c", 3))
        assertEquals(4.0, Rating.average(ratings), 0.0001)
    }

    @Test
    fun isValidValue_onlyOneToFive() {
        assertFalse(Rating.isValidValue(0))
        assertTrue(Rating.isValidValue(1))
        assertTrue(Rating.isValidValue(5))
        assertFalse(Rating.isValidValue(6))
    }

    @Test
    fun validateText_blankIsRejected() {
        assertNotNull(Review.validateText(""))
        assertNotNull(Review.validateText("   "))
    }

    @Test
    fun validateText_tooLongIsRejected() =
        assertNotNull(Review.validateText("a".repeat(Review.MAX_LENGTH + 1)))

    @Test
    fun validateText_normalTextIsAccepted() {
        assertNull(Review.validateText("Lovely quiet site."))
        assertNull(Review.validateText("a".repeat(Review.MAX_LENGTH)))
    }
}
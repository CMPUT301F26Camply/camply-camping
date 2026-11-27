package com.example.camply.review.model

// Rating.kt - One entrant's star rating (1-5) for a campsite (US 01.13.01).
// Stored at campsites/{campsiteId}/ratings/{entrantId}, so an entrant has at most one rating per campsite.
// Outstanding issues: none.
//
// AI Assistance: Anthropic, Claude, "Implement US 01.13.01 / 01.13.02 / 01.11.01 following Camply MVC", 2026-11-24.

/**
 * A single star rating given by an entrant.
 *
 * @property entrantId Firebase user ID of the entrant; also the Firestore document ID.
 * @property value The star value, from [MIN_VALUE] to [MAX_VALUE].
 */
data class Rating(
    val entrantId: String = "",
    val value: Int = 0
) {
    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 5

        /**
         * Checks that a star value is allowed.
         *
         * @param value The star value to check.
         * @return True if the value is from [MIN_VALUE] to [MAX_VALUE].
         */
        fun isValidValue(value: Int): Boolean = value in MIN_VALUE..MAX_VALUE

        /**
         * Calculates the average star value.
         *
         * @param ratings The ratings to average.
         * @return The average, or 0.0 when there are no ratings.
         */
        fun average(ratings: List<Rating>): Double =
            if (ratings.isEmpty()) 0.0 else ratings.map { it.value }.average()
    }
}
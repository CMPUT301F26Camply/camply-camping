package com.example.camply.review.model

// RatingSummary.kt - Running total of a campsite's star ratings (US 01.13.01).
// Stored at campsites/{campsiteId}/ratingSummary/summary. Firestore transactions cannot run queries,
// so keeping the sum and count lets a rating be saved and the average updated in one atomic transaction.
// Outstanding issues: none.

/**
 * The sum and number of a campsite's ratings.
 *
 * @property sum The total of every entrant's star value.
 * @property count How many entrants have rated the campsite.
 */
data class RatingSummary(
    val sum: Long = 0,
    val count: Long = 0
) {
    /**
     * Returns the summary after an entrant's rating is added or replaced.
     *
     * @param previous The entrant's earlier star value, or null if they have not rated before.
     * @param value The entrant's new star value.
     * @return The updated summary; the count only grows for a first-time rater.
     */
    fun withRating(previous: Int?, value: Int): RatingSummary =
        RatingSummary(sum - (previous ?: 0) + value, if (previous == null) count + 1 else count)

    /** The average star value, or 0.0 when there are no ratings. */
    fun average(): Double = if (count == 0L) 0.0 else sum.toDouble() / count
}
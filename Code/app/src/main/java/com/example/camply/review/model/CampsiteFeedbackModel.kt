package com.example.camply.review.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// CampsiteFeedbackModel.kt - State of the ratings and reviews section on the campsite details
// screen (US 01.13.01, US 01.13.02). Holds data only; the controller changes it, the view reads it.
// Outstanding issues: none.

/**
 * Stores the ratings, reviews and form drafts for one campsite and one entrant.
 */
class CampsiteFeedbackModel {

    var reviews: List<Review> by mutableStateOf(emptyList())
        private set

    var ratings: List<Rating> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    /** The star value currently selected; 0 means nothing is selected. */
    var ratingDraft: Int by mutableStateOf(0)
        private set

    var reviewDraft: String by mutableStateOf("")
        private set

    var isSubmitting: Boolean by mutableStateOf(false)
        private set

    var actionError: String? by mutableStateOf(null)
        private set

    var confirmation: String? by mutableStateOf(null)
        private set

    /** Average of all submitted ratings, or 0.0 if there are none. */
    val averageRating: Double
        get() = Rating.average(ratings)

    val ratingCount: Int
        get() = ratings.size

    /** Shows the loading state while feedback is fetched. */
    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * Stores the loaded feedback and preselects the entrant's existing rating.
     *
     * @param reviewList The campsite's reviews, newest first.
     * @param ratingList All ratings for the campsite.
     * @param ownRating The entrant's saved rating, or 0 if they have not rated.
     */
    fun showFeedback(reviewList: List<Review>, ratingList: List<Rating>, ownRating: Int) {
        reviews = reviewList
        ratings = ratingList
        ratingDraft = ownRating
        isLoading = false
        isSubmitting = false
        errorMessage = null
    }

    /** @param message Why the feedback could not be loaded. */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }

    /** @param value The star value the entrant tapped. */
    fun updateRatingDraft(value: Int) {
        ratingDraft = value
        actionError = null
        confirmation = null
    }

    /** @param text The review text typed so far. */
    fun updateReviewDraft(text: String) {
        reviewDraft = text
        actionError = null
        confirmation = null
    }

    /** Marks a save as in progress and clears old messages. */
    fun showSubmitting() {
        isSubmitting = true
        actionError = null
        confirmation = null
    }

    /** @param message Why the rating or review could not be saved. */
    fun showActionError(message: String) {
        isSubmitting = false
        actionError = message
    }

    /** @param message The success message to display. */
    fun showConfirmation(message: String) {
        isSubmitting = false
        confirmation = message
    }
}
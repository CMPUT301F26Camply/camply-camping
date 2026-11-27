package com.example.camply.review.controller

import com.example.camply.review.model.CampsiteFeedbackModel
import com.example.camply.review.model.Rating
import com.example.camply.review.model.Review
import com.example.camply.review.repository.FeedbackRepository
import kotlin.coroutines.cancellation.CancellationException

// CampsiteFeedbackController.kt - Handles entrant actions in the ratings and reviews section
// (US 01.13.01, US 01.13.02) and updates CampsiteFeedbackModel. Contains no UI code.
// Outstanding issues: none.

/**
 * Loads, validates and saves ratings and reviews for the campsite details screen.
 *
 * @param model The model storing the section's state.
 * @param repository The repository used to read and write ratings and reviews.
 * @param onRatingSaved Called after a rating is saved, so other sections showing the campsite's
 * average (such as the details header) can refresh. Defaults to doing nothing. // ▶ NEW
 */
class CampsiteFeedbackController(
    private val model: CampsiteFeedbackModel,
    private val repository: FeedbackRepository,
    private val onRatingSaved: suspend () -> Unit = {} // ▶ NEW
) {

    /**
     * Loads a campsite's reviews and ratings, and preselects the entrant's own rating.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun loadFeedback(campsiteId: String, entrantId: String) {
        model.showLoading()
        try {
            refresh(campsiteId, entrantId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load ratings and reviews.")
        }
    }

    /** @param value The star value the entrant tapped. */
    fun onStarSelected(value: Int) = model.updateRatingDraft(value)

    /** @param text The review text typed so far. */
    fun onReviewTextChanged(text: String) = model.updateReviewDraft(text)

    /**
     * Saves the selected star value as the entrant's rating (US 01.13.01).
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun submitRating(campsiteId: String, entrantId: String) {
        val value = model.ratingDraft
        if (!Rating.isValidValue(value)) {
            model.showActionError("Select a rating from 1 to 5 stars.")
            return
        }
        submit(campsiteId, entrantId, "Rating saved.", "Could not save your rating.") {
            repository.saveRating(campsiteId, Rating(entrantId, value))
            onRatingSaved() // ▶ NEW
        }
    }

    /**
     * Saves the typed text as a new review by the entrant (US 01.13.02).
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param entrantId The entrant's Firebase user ID.
     * @param entrantName The entrant's name, shown beside the review.
     */
    suspend fun submitReview(campsiteId: String, entrantId: String, entrantName: String) {
        val text = model.reviewDraft
        val error = Review.validateText(text)
        if (error != null) {
            model.showActionError(error)
            return
        }
        submit(campsiteId, entrantId, "Review posted.", "Could not post your review.") {
            repository.addReview(campsiteId, Review(authorId = entrantId, authorName = entrantName, text = text.trim()))
            model.updateReviewDraft("")
        }
    }

    // Runs a save, then re-reads the feedback so the list and average are up to date.
    private suspend fun submit(
        campsiteId: String,
        entrantId: String,
        successMessage: String,
        failureMessage: String,
        save: suspend () -> Unit
    ) {
        model.showSubmitting()
        try {
            save()
            refresh(campsiteId, entrantId)
            model.showConfirmation(successMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showActionError(e.message ?: failureMessage)
        }
    }

    private suspend fun refresh(campsiteId: String, entrantId: String) {
        val reviews = repository.getReviews(campsiteId)
        val ratings = repository.getRatings(campsiteId)
        model.showFeedback(reviews, ratings, ratings.firstOrNull { it.entrantId == entrantId }?.value ?: 0)
    }
}
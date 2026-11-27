package com.example.camply.review

import com.example.camply.review.controller.CampsiteFeedbackController
import com.example.camply.review.model.CampsiteFeedbackModel
import com.example.camply.review.model.Rating
import com.example.camply.review.model.Review
import com.example.camply.review.repository.FeedbackRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests loading, rating and reviewing through a fake repository (US 01.13.01, US 01.13.02). */
class CampsiteFeedbackControllerTest {

    private lateinit var repository: FakeFeedbackRepository
    private lateinit var model: CampsiteFeedbackModel
    private lateinit var controller: CampsiteFeedbackController

    @Before
    fun setUp() {
        repository = FakeFeedbackRepository()
        model = CampsiteFeedbackModel()
        controller = CampsiteFeedbackController(model, repository)
    }

    @Test
    fun loadFeedback_showsReviewsAndPreselectsOwnRating() = runBlocking {
        repository.ratings += Rating("entrant1", 4)
        repository.ratings += Rating("entrant2", 2)
        repository.reviews += Review("r1", "entrant2", "Sam", "Muddy but fun")

        controller.loadFeedback("site1", "entrant1")

        assertFalse(model.isLoading)
        assertEquals(1, model.reviews.size)
        assertEquals(4, model.ratingDraft)
        assertEquals(3.0, model.averageRating, 0.0001)
    }

    @Test
    fun loadFeedback_failureShowsError() = runBlocking {
        repository.failure = IllegalStateException("Offline")

        controller.loadFeedback("site1", "entrant1")

        assertEquals("Offline", model.errorMessage)
    }

    @Test
    fun submitRating_withoutSelection_isRejectedAndNotSaved() = runBlocking {
        controller.submitRating("site1", "entrant1")

        assertNotNull(model.actionError)
        assertTrue(repository.ratings.isEmpty())
    }

    @Test
    fun submitRating_savesWithEntrantIdAndUpdatesAverage() = runBlocking {
        controller.loadFeedback("site1", "entrant1")
        controller.onStarSelected(5)

        controller.submitRating("site1", "entrant1")

        assertEquals(listOf(Rating("entrant1", 5)), repository.ratings)
        assertEquals(5.0, model.averageRating, 0.0001)
        assertEquals("Rating saved.", model.confirmation)
    }

    @Test
    fun submitRating_again_replacesEarlierRating() = runBlocking {
        controller.onStarSelected(2)
        controller.submitRating("site1", "entrant1")
        controller.onStarSelected(4)
        controller.submitRating("site1", "entrant1")

        assertEquals(listOf(Rating("entrant1", 4)), repository.ratings)
    }

    @Test // ▶ NEW
    fun submitRating_notifiesListenerOnlyWhenSaved() = runBlocking {
        var notified = 0
        val listening = CampsiteFeedbackController(model, repository) { notified++ }

        listening.submitRating("site1", "entrant1") // nothing selected: rejected
        assertEquals(0, notified)

        listening.onStarSelected(3)
        listening.submitRating("site1", "entrant1")
        assertEquals(1, notified)
    }

    @Test // ▶ NEW
    fun submitReview_doesNotNotifyRatingListener() = runBlocking {
        var notified = 0
        val listening = CampsiteFeedbackController(model, repository) { notified++ }
        listening.onReviewTextChanged("Nice")

        listening.submitReview("site1", "entrant1", "Alex")

        assertEquals(0, notified)
    }

    @Test
    fun submitReview_blankIsRejectedAndNotSaved() = runBlocking {
        controller.onReviewTextChanged("   ")

        controller.submitReview("site1", "entrant1", "Alex")

        assertNotNull(model.actionError)
        assertTrue(repository.reviews.isEmpty())
    }

    @Test
    fun submitReview_savesWithAuthorIdAndNameThenClearsDraft() = runBlocking {
        controller.onReviewTextChanged("  Great views!  ")

        controller.submitReview("site1", "entrant1", "Alex")

        val saved = repository.reviews.single()
        assertEquals("entrant1", saved.authorId)
        assertEquals("Alex", saved.authorName)
        assertEquals("Great views!", saved.text)
        assertEquals("", model.reviewDraft)
        assertEquals(1, model.reviews.size)
        assertNull(model.actionError)
    }

    @Test
    fun submitReview_failureKeepsDraftAndShowsError() = runBlocking {
        controller.onReviewTextChanged("Nice")
        repository.failure = IllegalStateException("Permission denied")

        controller.submitReview("site1", "entrant1", "Alex")

        assertEquals("Permission denied", model.actionError)
        assertEquals("Nice", model.reviewDraft)
        assertFalse(model.isSubmitting)
    }

    /** In-memory repository, so no Firebase is needed. */
    private class FakeFeedbackRepository : FeedbackRepository {
        val reviews = mutableListOf<Review>()
        val ratings = mutableListOf<Rating>()
        var failure: Exception? = null

        override suspend fun getReviews(campsiteId: String): List<Review> {
            failure?.let { throw it }
            return reviews.reversed()
        }

        override suspend fun addReview(campsiteId: String, review: Review) {
            failure?.let { throw it }
            reviews += review.copy(id = "review${reviews.size + 1}")
        }

        override suspend fun getRatings(campsiteId: String): List<Rating> {
            failure?.let { throw it }
            return ratings.toList()
        }

        override suspend fun saveRating(campsiteId: String, rating: Rating) {
            failure?.let { throw it }
            ratings.removeAll { it.entrantId == rating.entrantId }
            ratings += rating
        }
    }
}
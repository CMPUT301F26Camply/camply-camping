package com.example.camply.review.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.camply.review.controller.CampsiteFeedbackController
import com.example.camply.review.model.CampsiteFeedbackModel
import com.example.camply.review.model.Rating
import com.example.camply.review.model.Review
import com.example.camply.review.repository.FeedbackRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests the ratings and reviews card with a fake repository (US 01.13.01, US 01.13.02). */
class CampsiteFeedbackViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var repository: FakeFeedbackRepository
    private lateinit var view: CampsiteFeedbackView

    @Before
    fun setUp() {
        repository = FakeFeedbackRepository()
        val model = CampsiteFeedbackModel()
        view = CampsiteFeedbackView(model, CampsiteFeedbackController(model, repository))
    }

    private fun show() {
        compose.setContent { MaterialTheme { view.Display("site1", "entrant1", "Alex") } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Ratings & reviews", ignoreCase = true).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Post review").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun display_showsExistingReviewWithAuthorName() {
        repository.reviews += Review("r1", "entrant2", "Sam", "Great spot!")
        repository.ratings += Rating("entrant2", 4)

        show()

        compose.onNodeWithText("Sam").assertIsDisplayed()
        compose.onNodeWithText("Great spot!").assertIsDisplayed()
        compose.onNodeWithText("4.0 / 5 · 1 rating").assertIsDisplayed()
    }

    @Test
    fun display_noFeedback_showsEmptyStates() {
        show()

        compose.onNodeWithText("No ratings yet").assertIsDisplayed()
        compose.onNodeWithText("No reviews yet. Be the first to share your experience.").assertIsDisplayed()
        compose.onNodeWithText("Submit rating").assertIsNotEnabled()
        compose.onNodeWithText("Post review").assertIsNotEnabled()
    }

    @Test
    fun selectingStarsAndSubmitting_savesRatingWithEntrantId() {
        show()

        compose.onNodeWithTag("star_4").performClick()
        compose.onNodeWithText("Submit rating").performClick()

        compose.waitUntil(5_000) { repository.ratings.isNotEmpty() }
        assertEquals(listOf(Rating("entrant1", 4)), repository.ratings)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Rating saved.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun writingAndPostingReview_savesAndShowsIt() {
        show()

        compose.onNodeWithTag("review_field").performTextInput("Peaceful and clean")
        compose.onNodeWithText("Post review").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Peaceful and clean").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("entrant1", repository.reviews.single().authorId)
        assertEquals("Alex", repository.reviews.single().authorName)
        compose.onNodeWithText("Review posted.").assertIsDisplayed()
    }

    private class FakeFeedbackRepository : FeedbackRepository {
        val reviews = mutableListOf<Review>()
        val ratings = mutableListOf<Rating>()

        override suspend fun getReviews(campsiteId: String) = reviews.reversed()
        override suspend fun addReview(campsiteId: String, review: Review) {
            reviews += review.copy(id = "review${reviews.size + 1}")
        }
        override suspend fun getRatings(campsiteId: String) = ratings.toList()
        override suspend fun saveRating(campsiteId: String, rating: Rating) {
            ratings.removeAll { it.entrantId == rating.entrantId }
            ratings += rating
        }
    }
}
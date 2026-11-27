package com.example.camply.review.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.camply.review.controller.CampsiteFeedbackController
import com.example.camply.review.model.CampsiteFeedbackModel
import com.example.camply.review.model.Rating
import com.example.camply.review.model.Review
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// CampsiteFeedbackView.kt - Ratings and reviews card for the campsite details screen
// (US 01.13.01, US 01.13.02). Plugs into CampsiteDetailView's `extraContent` slot, so the
// details layout does not need to change. Not a lazy list because the details screen already scrolls.
// Outstanding issues: none.

/**
 * Shows the average rating, the star selector, the review form and the posted reviews.
 *
 * @param model The model containing the section's state.
 * @param controller The controller that loads and saves ratings and reviews.
 */
class CampsiteFeedbackView(
    private val model: CampsiteFeedbackModel,
    private val controller: CampsiteFeedbackController
) {

    /**
     * Loads and displays ratings and reviews for a campsite.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param entrantId The entrant's Firebase user ID.
     * @param entrantName The entrant's name, saved with any review they post.
     */
    @Composable
    fun Display(campsiteId: String, entrantId: String, entrantName: String) {
        val scope = rememberCoroutineScope()
        LaunchedEffect(campsiteId, entrantId) { controller.loadFeedback(campsiteId, entrantId) }

        OrangeCard {
            CardLabel("Ratings & reviews")
            Spacer(Modifier.height(8.dp))

            val error = model.errorMessage
            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                error != null -> CenteredMessage(error, "Try again") {
                    scope.launch { controller.loadFeedback(campsiteId, entrantId) }
                }

                else -> {
                    RatingSection { scope.launch { controller.submitRating(campsiteId, entrantId) } }
                    Spacer(Modifier.height(16.dp))
                    ReviewForm { scope.launch { controller.submitReview(campsiteId, entrantId, entrantName) } }
                    ActionMessages()
                    Spacer(Modifier.height(16.dp))
                    ReviewList()
                }
            }
        }
    }

    // Average rating, five tappable stars and the submit button.
    @Composable
    private fun RatingSection(onSubmit: () -> Unit) {
        val summary = if (model.ratingCount == 0) {
            "No ratings yet"
        } else {
            val plural = if (model.ratingCount == 1) "rating" else "ratings"
            "${"%.1f".format(model.averageRating)} / 5 · ${model.ratingCount} $plural"
        }
        Text(summary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

        Row(verticalAlignment = Alignment.CenterVertically) {
            for (value in Rating.MIN_VALUE..Rating.MAX_VALUE) {
                IconButton(
                    onClick = { controller.onStarSelected(value) },
                    enabled = !model.isSubmitting,
                    modifier = Modifier.testTag("star_$value")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "$value ${if (value == 1) "star" else "stars"}",
                        tint = if (value <= model.ratingDraft) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                }
            }
        }
        PrimaryButton(
            text = "Submit rating",
            onClick = onSubmit,
            enabled = !model.isSubmitting && model.ratingDraft > 0
        )
    }

    // Text field with a character counter and the post button.
    @Composable
    private fun ReviewForm(onPost: () -> Unit) {
        OutlinedTextField(
            value = model.reviewDraft,
            onValueChange = controller::onReviewTextChanged,
            label = { Text("Write a review") },
            supportingText = { Text("${model.reviewDraft.length} / ${Review.MAX_LENGTH}") },
            minLines = 3,
            enabled = !model.isSubmitting,
            colors = camplyTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("review_field")
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = if (model.isSubmitting) "Saving..." else "Post review",
            onClick = onPost,
            enabled = !model.isSubmitting && model.reviewDraft.isNotBlank()
        )
    }

    @Composable
    private fun ActionMessages() {
        model.actionError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        model.confirmation?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    // Posted reviews, newest first, each with the author's name.
    @Composable
    private fun ReviewList() {
        if (model.reviews.isEmpty()) {
            Text(
                "No reviews yet. Be the first to share your experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.reviews.forEach { review ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(review.authorName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(dateText(review.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(review.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    private fun dateText(timestamp: Timestamp?): String =
        timestamp?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it.toDate()) } ?: "Just now"
}
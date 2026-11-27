package com.example.camply.review.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// Review.kt - A written campsite review (US 01.13.02).
// Stored at campsites/{campsiteId}/reviews/{reviewId}. createdAt is set by the server on save.
// Outstanding issues: reviews cannot be edited or deleted yet (admin removal is US 03.10.01).

/**
 * A text review written by an entrant.
 *
 * @property id Firestore document ID.
 * @property authorId Firebase user ID of the entrant who wrote the review.
 * @property authorName The entrant's name when the review was posted.
 * @property text The review text.
 * @property createdAt When the review was saved, or null while the server timestamp is pending.
 */
data class Review(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
) {
    companion object {
        const val MAX_LENGTH = 1000

        /**
         * Checks review text before it is saved.
         *
         * @param text The text typed by the entrant.
         * @return A user-facing error message, or null if the text is valid.
         */
        fun validateText(text: String): String? = when {
            text.isBlank() -> "Write a review before posting."
            text.trim().length > MAX_LENGTH -> "Reviews can be at most $MAX_LENGTH characters."
            else -> null
        }
    }
}
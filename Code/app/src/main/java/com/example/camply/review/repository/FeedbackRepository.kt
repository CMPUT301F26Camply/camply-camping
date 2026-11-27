package com.example.camply.review.repository

import com.example.camply.review.model.Rating
import com.example.camply.review.model.RatingSummary
import com.example.camply.review.model.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// FeedbackRepository.kt - Where campsite ratings and reviews are read and written (US 01.13.01, 01.13.02).
// Controllers depend on the interface, not on Firebase, so tests can use a fake.
// Outstanding issues: campsites rated before the ratingSummary document existed need their old
// ratings removed (test data only), because the summary starts empty.

/**
 * Defines storage operations for campsite ratings and reviews.
 */
interface FeedbackRepository {
    /**
     * Retrieves a campsite's reviews.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @return The reviews, newest first.
     */
    suspend fun getReviews(campsiteId: String): List<Review>

    /**
     * Saves a new review with a server timestamp.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param review The review to save; its ID and timestamp are assigned by Firestore.
     */
    suspend fun addReview(campsiteId: String, review: Review)

    /**
     * Retrieves every rating given to a campsite.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @return The campsite's ratings.
     */
    suspend fun getRatings(campsiteId: String): List<Rating>

    /**
     * Atomically saves an entrant's rating (replacing their earlier one) and updates the campsite's
     * average, so a failure never leaves the rating and the average out of step.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param rating The rating to save.
     */
    suspend fun saveRating(campsiteId: String, rating: Rating)
}

/**
 * Firestore-backed implementation. Collection: "campsites", subcollections "reviews", "ratings"
 * and "ratingSummary".
 */
class FirestoreFeedbackRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FeedbackRepository {
    private val campsites = db.collection("campsites")

    override suspend fun getReviews(campsiteId: String): List<Review> {
        requireDocumentId(campsiteId)
        return campsites.document(campsiteId).collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await().toObjects(Review::class.java)
    }

    override suspend fun addReview(campsiteId: String, review: Review) {
        requireDocumentId(campsiteId)
        require(review.authorId.isNotBlank()) { "Review author must not be blank." }
        require(Review.validateText(review.text) == null) { "Invalid review text." }
        campsites.document(campsiteId).collection("reviews").add(
            mapOf(
                "authorId" to review.authorId,
                "authorName" to review.authorName,
                "text" to review.text,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun getRatings(campsiteId: String): List<Rating> {
        requireDocumentId(campsiteId)
        return campsites.document(campsiteId).collection("ratings")
            .get().await().toObjects(Rating::class.java)
    }

    /**
     * Saves the rating, the updated rating summary and the campsite's average in one transaction,
     * so either all three change or none do. A missing campsite fails the whole transaction.
     */
    override suspend fun saveRating(campsiteId: String, rating: Rating) {
        requireDocumentId(campsiteId)
        requireDocumentId(rating.entrantId)
        require(Rating.isValidValue(rating.value)) { "Rating must be from 1 to 5." }

        val campsiteRef = campsites.document(campsiteId)
        val ratingRef = campsiteRef.collection("ratings").document(rating.entrantId)
        val summaryRef = campsiteRef.collection("ratingSummary").document(SUMMARY_ID)

        db.runTransaction { transaction ->
            // Firestore transactions require every read to come before any write.
            val previous = transaction.get(ratingRef).toObject(Rating::class.java)?.value
            val summary = transaction.get(summaryRef).toObject(RatingSummary::class.java)
                ?: RatingSummary()

            val updated = summary.withRating(previous, rating.value)

            transaction.set(ratingRef, rating)
            transaction.set(summaryRef, updated)
            transaction.update(campsiteRef, "rating", updated.average())
        }.await()
    }

    private fun requireDocumentId(value: String) =
        require(value.isNotBlank() && '/' !in value) { "Invalid ID." }

    private companion object {
        const val SUMMARY_ID = "summary"
    }
}
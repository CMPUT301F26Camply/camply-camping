package com.example.camply.user.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Removes uploaded images for the admin (US 03.03.01).
 */
interface AdminImageRepository {

    /**
     * Removes one promotional photo from a campsite.
     *
     * @param campsiteId The campsite's Firestore document ID.
     * @param photoId The ID of the photo to remove.
     */
    suspend fun removeCampsitePhoto(campsiteId: String, photoId: String)

    /**
     * Removes a user's profile image.
     *
     * @param userId The user's Firebase user ID.
     */
    suspend fun removeProfileImage(userId: String)
}

/**
 * Firestore implementation. Campsite photos are listed in a campsite's "images" field,
 * with full and preview versions in its "photos" and "previews" subcollections.
 * Profile images are stored in "profileImages".
 */
class FirestoreAdminImageRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AdminImageRepository {

    // Only the photo ID is taken out of the list, so the rest of the campsite
    // is not overwritten with an older copy.
    override suspend fun removeCampsitePhoto(campsiteId: String, photoId: String) {
        requireDocumentId(campsiteId)
        requireDocumentId(photoId)

        val campsite = db.collection("campsites").document(campsiteId)
        db.batch()
            .update(campsite, "images", FieldValue.arrayRemove(photoId))
            .delete(campsite.collection("photos").document(photoId))
            .delete(campsite.collection("previews").document(photoId))
            .commit()
            .await()
    }

    override suspend fun removeProfileImage(userId: String) {
        requireDocumentId(userId)
        db.collection("profileImages").document(userId).delete().await()
    }

    private fun requireDocumentId(id: String) =
        require(id.isNotBlank() && '/' !in id) { "Invalid ID." }
}

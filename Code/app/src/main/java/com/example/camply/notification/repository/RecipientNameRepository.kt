package com.example.camply.notification.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Defines how recipient names are retrieved for a sent notification's details (US 02.10.02).
 */
interface RecipientNameRepository {
    /**
     * Retrieves a user's name.
     *
     * @param userId The user's Firebase user ID.
     * @return The user's name, or null if the user no longer exists.
     */
    suspend fun getRecipientName(userId: String): String?
}

/**
 * Retrieves recipient names from the "users" collection in Cloud Firestore.
 *
 * @param db The Firestore database instance used to retrieve names.
 */
class FirestoreRecipientNameRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RecipientNameRepository {
    private val users = db.collection("users")

    override suspend fun getRecipientName(userId: String): String? =
        users.document(userId).get().await().getString("name")
}

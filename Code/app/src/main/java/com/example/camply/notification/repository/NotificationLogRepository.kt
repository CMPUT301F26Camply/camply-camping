package com.example.camply.notification.repository

import com.example.camply.notification.model.Notification
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Observes every notification in the app for the admin's notification log (US 03.08.01).
 */
interface NotificationLogRepository {
    /**
     * Starts listening to every notification record.
     *
     * @param onUpdate Called with all records whenever they change.
     * @param onError Called when the records cannot be read.
     * @return A function that stops listening.
     */
    fun observeAll(
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit
}

/**
 * Observes the notification log in Cloud Firestore.
 *
 * @param db The Firestore instance to read from.
 */
class FirestoreNotificationLogRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationLogRepository {
    private val notifications = db.collection("notifications")

    override fun observeAll(
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        val listener = notifications.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val records = try {
                snapshot?.toObjects(Notification::class.java) ?: emptyList()
            } catch (exception: Exception) {
                onError(exception)
                return@addSnapshotListener
            }
            onUpdate(records)
        }
        return { listener.remove() }
    }
}

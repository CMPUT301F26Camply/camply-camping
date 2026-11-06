package com.example.camply.notification.repository

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.SendResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Shared collection for entrant inboxes, owner sent history, and eventual admin logs. */
class FirestoreNotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {
    private val notifications = db.collection("notifications")

    override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
        val ref = notifications.document(id)
        val content = mapOf(
            "eventId" to notification.eventId,
            "senderId" to notification.senderId,
            "recipientId" to notification.recipientId,
            "origin" to notification.origin.name,
            "type" to notification.type.name,
            "title" to notification.title,
            "content" to notification.content,
            "lotteryId" to notification.lotteryId,
            "reservationId" to notification.reservationId
        )
        return db.runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (existing.exists()) {
                check(content.all { (key, value) -> existing.get(key) == value }) {
                    "A notification already exists for this event with different content."
                }
                // Retry must preserve the first timestamp and the recipient's read status.
                SendResult.ALREADY_EXISTS
            } else {
                transaction.set(ref, content + mapOf(
                    "createdAt" to FieldValue.serverTimestamp(),
                    "isRead" to false
                ))
                SendResult.CREATED
            }
        }.await()
    }

    override fun observeReceived(
        recipientId: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit = observe("recipientId", recipientId, onUpdate, onError)

    override fun observeSent(
        senderId: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit = observe("senderId", senderId, onUpdate, onError)

    private fun observe(
        field: String,
        uid: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        require(uid.isNotBlank())
        val listener = notifications.whereEqualTo(field, uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val records = try {
                snapshot?.toObjects(Notification::class.java)
                    ?.sortedWith(compareByDescending<Notification> { it.createdAt }.thenBy { it.id })
                    ?: emptyList()
            } catch (exception: Exception) {
                onError(exception)
                return@addSnapshotListener
            }
            onUpdate(records)
        }
        return { listener.remove() }
    }

    override suspend fun markAsRead(notificationId: String, recipientId: String) {
        require(notificationId.isNotBlank() && '/' !in notificationId)
        require(recipientId.isNotBlank())
        val ref = notifications.document(notificationId)
        db.runTransaction { transaction ->
            val record = transaction.get(ref)
            check(record.exists()) { "Notification not found." }
            check(record.getString("recipientId") == recipientId) { "Recipient does not match." }
            transaction.update(ref, "isRead", true)
        }.await()
    }
}

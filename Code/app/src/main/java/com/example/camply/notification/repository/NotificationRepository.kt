package com.example.camply.notification.repository

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.SendResult

interface NotificationRepository {
    /** Atomically creates a record, or rejects conflicting content for an existing ID. */
    suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult

    /** Returns an unsubscribe function; call it when the screen/model is disposed. */
    fun observeReceived(
        recipientId: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit

    fun observeSent(
        senderId: String,
        onUpdate: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit

    /** Updates only isRead, and checks that the recipient matches. Rules must enforce this too. */
    suspend fun markAsRead(notificationId: String, recipientId: String)
}

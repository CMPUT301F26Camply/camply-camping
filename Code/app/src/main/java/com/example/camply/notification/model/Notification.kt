package com.example.camply.notification.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class NotificationType {
    LOTTERY_WIN, LOTTERY_LOSE, RESERVATION_CONFIRMED, RESERVATION_CANCELLED, OWNER_MESSAGE
}

enum class NotificationOrigin { SYSTEM, OWNER }

/** Shared inbox record. Document IDs are assigned by NotificationService. */
data class Notification(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val origin: NotificationOrigin = NotificationOrigin.SYSTEM,
    val type: NotificationType = NotificationType.LOTTERY_WIN,
    val title: String = "",
    val content: String = "",
    val lotteryId: String? = null,
    val reservationId: String? = null,
    val createdAt: Timestamp? = null,
    // Explicit mapping avoids JavaBean's isRead/read naming ambiguity.
    @get:com.google.firebase.firestore.PropertyName("isRead")
    val isRead: Boolean = false
)

/** Creation input deliberately excludes read status and timestamps. */
data class NewNotification(
    val eventId: String,
    val senderId: String,
    val recipientId: String,
    val origin: NotificationOrigin,
    val type: NotificationType,
    val title: String,
    val content: String,
    val lotteryId: String? = null,
    val reservationId: String? = null
) {
    init {
        require(listOf(eventId, senderId, recipientId, title, content).none { it.isBlank() }) {
            "Notification identity and content must not be blank."
        }
        require(lotteryId == null || lotteryId.isNotBlank())
        require(reservationId == null || reservationId.isNotBlank())
        if (type == NotificationType.LOTTERY_WIN || type == NotificationType.LOTTERY_LOSE) {
            require(lotteryId != null) { "A lottery result requires a lottery ID." }
        }
    }
}

enum class SendResult { CREATED, ALREADY_EXISTS }

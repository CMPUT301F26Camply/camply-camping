package com.example.camply.notification.service

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import java.security.MessageDigest

/** Creates inbox records. Device push delivery is a separate integration. */
class NotificationService(private val repository: NotificationRepository) {
    suspend fun send(notification: NewNotification): SendResult {
        // Length-prefixing avoids ambiguous event/recipient concatenation.
        val identity = "${notification.eventId.length}:${notification.eventId}${notification.recipientId}"
        val id = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return repository.createIfAbsent(id, notification)
    }

    /** Call only with the persisted result. Retry failures without drawing again. */
    suspend fun sendLotteryResult(
        ownerId: String,
        entrantId: String,
        lotteryId: String,
        won: Boolean
    ): SendResult = send(NewNotification(
        eventId = "lottery-result:$lotteryId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = if (won) NotificationType.LOTTERY_WIN else NotificationType.LOTTERY_LOSE,
        title = if (won) "You won the campsite lottery!" else "Campsite lottery result",
        content = if (won) {
            "You won an opportunity to reserve a campsite. Check the lottery booking dates and available stays."
        } else {
            "You were not selected in this campsite lottery. You can explore other campsites and lotteries."
        },
        lotteryId = lotteryId
    ))
}

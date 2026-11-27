package com.example.camply.notification.model

import com.google.firebase.Timestamp

private const val PREVIEW_LENGTH = 80

/**
 * Represents one notification sent to one or more recipients (US 02.10.02, US 03.08.01).
 *
 * @property eventId The ID shared by every record created for this send.
 * @property title The notification's title.
 * @property content The full message.
 * @property recipientIds The IDs of the users who received the notification.
 * @property sentAt When it was sent, or null.
 * @property type What triggered the notification.
 */
data class SentNotification(
    val eventId: String,
    val title: String,
    val content: String,
    val recipientIds: List<String>,
    val sentAt: Timestamp?,
    val type: NotificationType = NotificationType.OWNER_MESSAGE
) {
    /** The number of users who received the notification. */
    val recipientCount: Int
        get() = recipientIds.size

    /**
     * Shortens the message for display in a list.
     *
     * @return The full message if it is short enough, otherwise its start followed by "…".
     */
    fun preview(): String =
        if (content.length <= PREVIEW_LENGTH) content
        else content.take(PREVIEW_LENGTH).trimEnd() + "…"

    /**
     * Describes what triggered the notification.
     *
     * @return A short label for the notification's source.
     */
    fun source(): String = when (type) {
        NotificationType.LOTTERY_WIN -> "Lottery result: won"
        NotificationType.LOTTERY_LOSE -> "Lottery result: lost"
        NotificationType.RESERVATION_CONFIRMED -> "Reservation confirmed"
        NotificationType.RESERVATION_CANCELLED -> "Reservation cancelled"
        NotificationType.OWNER_MESSAGE -> "Owner message"
        NotificationType.LOTTERY_CANCELLED -> "Lottery cancelled"
        NotificationType.CAMPSITE_REMOVED_FROM_LOTTERY -> "Campsite removed from lottery"
    }
}

/**
 * Groups stored notification records into the sends made by a campsite owner.
 *
 * Each recipient has their own record, so records sharing an event ID form one send.
 * System notifications (lottery results,...) are left out because the owner did not write them.
 *
 * @param records The notification records with the owner as their sender.
 * @return The owner's sent notifications, newest first.
 */
fun groupSentNotifications(records: List<Notification>): List<SentNotification> =
    groupNotificationLog(records.filter { it.origin == NotificationOrigin.OWNER })

/**
 * Groups every stored notification record into the app's notification log (US 03.08.01).
 *
 * Records sharing an event ID and type form one entry. The type is part of the group because
 * a lottery's win and loss notifications share an event ID but have different messages.
 *
 * @param records All notification records in the app.
 * @return The log entries, newest first.
 */
fun groupNotificationLog(records: List<Notification>): List<SentNotification> =
    records
        .groupBy { it.eventId to it.type }
        .map { (key, group) ->
            SentNotification(
                eventId = key.first,
                title = group.first().title,
                content = group.first().content,
                recipientIds = group.map { it.recipientId }.distinct(),
                sentAt = group.mapNotNull { it.createdAt }.maxOrNull(),
                type = key.second
            )
        }
        .sortedWith(
            compareByDescending<SentNotification> { it.sentAt }
                .thenBy { it.eventId }
                .thenBy { it.type }
        )

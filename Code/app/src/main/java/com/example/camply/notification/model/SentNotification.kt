package com.example.camply.notification.model

import com.google.firebase.Timestamp

private const val PREVIEW_LENGTH = 80

/**
 * Represents one notification sent by a campsite owner to one or more recipients (US 02.10.02).
 *
 * @property eventId The ID shared by every record created for this send.
 * @property title The notification's title.
 * @property content The full message.
 * @property recipientIds The IDs of the users who received the notification.
 * @property sentAt When it was sent, or null.
 */
data class SentNotification(
    val eventId: String,
    val title: String,
    val content: String,
    val recipientIds: List<String>,
    val sentAt: Timestamp?
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
    records
        .filter { it.origin == NotificationOrigin.OWNER }
        .groupBy { it.eventId }
        .map { (eventId, group) ->
            SentNotification(
                eventId = eventId,
                title = group.first().title,
                content = group.first().content,
                recipientIds = group.map { it.recipientId }.distinct(),
                sentAt = group.mapNotNull { it.createdAt }.maxOrNull()
            )
        }
        .sortedWith(compareByDescending<SentNotification> { it.sentAt }.thenBy { it.eventId })

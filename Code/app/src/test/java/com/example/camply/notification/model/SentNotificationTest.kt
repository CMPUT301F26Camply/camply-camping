package com.example.camply.notification.model

import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import org.junit.Test

/**
 * Unit tests for grouping and previewing an owner's sent notifications (US 02.10.02).
 */
class SentNotificationTest {

    /**
     * Checks that records sharing an event ID become one send with all of its recipients.
     */
    @Test
    fun groupSentNotifications_sameEventId_groupsIntoOneSend() {
        val records = listOf(
            record("event1", recipientId = "entrant1", content = "Quiet hours start at 10 PM."),
            record("event1", recipientId = "entrant2", content = "Quiet hours start at 10 PM.")
        )

        val sent = groupSentNotifications(records).single()

        assertEquals("event1", sent.eventId)
        assertEquals("Quiet hours start at 10 PM.", sent.content)
        assertEquals(listOf("entrant1", "entrant2"), sent.recipientIds)
        assertEquals(2, sent.recipientCount)
    }

    /**
     * Checks that a repeated recipient is only counted once.
     */
    @Test
    fun groupSentNotifications_repeatedRecipient_countsOnce() {
        val records = listOf(
            record("event1", recipientId = "entrant1"),
            record("event1", recipientId = "entrant1")
        )

        assertEquals(1, groupSentNotifications(records).single().recipientCount)
    }

    /**
     * Checks that system notifications are left out of the owner's history.
     */
    @Test
    fun groupSentNotifications_systemNotification_isLeftOut() {
        val records = listOf(
            record("owner-event"),
            record("lottery-result:lottery1", origin = NotificationOrigin.SYSTEM)
        )

        val sent = groupSentNotifications(records)

        assertEquals(listOf("owner-event"), sent.map { it.eventId })
    }

    /**
     * Checks that the newest send comes first.
     */
    @Test
    fun groupSentNotifications_differentEvents_sortsNewestFirst() {
        val records = listOf(
            record("older", createdAt = Timestamp(100, 0)),
            record("newer", createdAt = Timestamp(200, 0))
        )

        val sent = groupSentNotifications(records)

        assertEquals(listOf("newer", "older"), sent.map { it.eventId })
    }

    /**
     * Checks that a send without a time comes after sends that have one.
     */
    @Test
    fun groupSentNotifications_missingTime_sortsLast() {
        val records = listOf(
            record("no-time", createdAt = null),
            record("has-time", createdAt = Timestamp(100, 0))
        )

        val sent = groupSentNotifications(records)

        assertEquals(listOf("has-time", "no-time"), sent.map { it.eventId })
    }

    /**
     * Checks that a message at the preview limit is not shortened.
     */
    @Test
    fun preview_messageAtLimit_returnsFullMessage() {
        val message = "a".repeat(80)

        assertEquals(message, sentNotification(message).preview())
    }

    /**
     * Checks that a message over the preview limit is shortened.
     */
    @Test
    fun preview_messageOverLimit_shortensMessage() {
        val message = "a".repeat(81)

        assertEquals("a".repeat(80) + "…", sentNotification(message).preview())
    }

    private fun record(
        eventId: String,
        recipientId: String = "entrant1",
        origin: NotificationOrigin = NotificationOrigin.OWNER,
        content: String = "Message",
        createdAt: Timestamp? = Timestamp(100, 0)
    ) = Notification(
        id = "$eventId-$recipientId",
        eventId = eventId,
        senderId = "owner1",
        recipientId = recipientId,
        origin = origin,
        type = NotificationType.OWNER_MESSAGE,
        title = "Title",
        content = content,
        createdAt = createdAt
    )

    private fun sentNotification(content: String) = SentNotification(
        eventId = "event1",
        title = "Title",
        content = content,
        recipientIds = listOf("entrant1"),
        sentAt = null
    )
}

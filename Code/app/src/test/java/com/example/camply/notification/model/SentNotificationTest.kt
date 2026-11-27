package com.example.camply.notification.model

import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import org.junit.Test

/**
 * Unit tests for grouping and previewing sent notifications and the notification log
 * (US 02.10.02, US 03.08.01).
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

    /**
     * Checks that a notification sent to several users is one log entry with all its recipients.
     */
    @Test
    fun groupNotificationLog_sameEvent_groupsIntoOneEntry() {
        val records = listOf(
            record("event1", recipientId = "entrant1"),
            record("event1", recipientId = "entrant2"),
            record("event1", recipientId = "entrant3")
        )

        val entry = groupNotificationLog(records).single()

        assertEquals(listOf("entrant1", "entrant2", "entrant3"), entry.recipientIds)
        assertEquals(3, entry.recipientCount)
    }

    /**
     * Checks that system notifications are included in the log.
     */
    @Test
    fun groupNotificationLog_systemNotification_isIncluded() {
        val records = listOf(
            record("owner-event"),
            record(
                "reservation-confirmed:reservation1",
                origin = NotificationOrigin.SYSTEM,
                type = NotificationType.RESERVATION_CONFIRMED
            )
        )

        val log = groupNotificationLog(records)

        assertEquals(
            setOf("owner-event", "reservation-confirmed:reservation1"),
            log.map { it.eventId }.toSet()
        )
    }

    /**
     * Checks that a lottery's win and loss notifications are separate entries.
     */
    @Test
    fun groupNotificationLog_lotteryWinAndLoss_areSeparateEntries() {
        val records = listOf(
            record("lottery-result:lottery1", "winner", NotificationOrigin.SYSTEM, NotificationType.LOTTERY_WIN, "You won"),
            record("lottery-result:lottery1", "loser1", NotificationOrigin.SYSTEM, NotificationType.LOTTERY_LOSE, "You lost"),
            record("lottery-result:lottery1", "loser2", NotificationOrigin.SYSTEM, NotificationType.LOTTERY_LOSE, "You lost")
        )

        val log = groupNotificationLog(records)

        val won = log.single { it.type == NotificationType.LOTTERY_WIN }
        val lost = log.single { it.type == NotificationType.LOTTERY_LOSE }
        assertEquals(2, log.size)
        assertEquals("You won", won.content)
        assertEquals(listOf("winner"), won.recipientIds)
        assertEquals("You lost", lost.content)
        assertEquals(listOf("loser1", "loser2"), lost.recipientIds)
    }

    /**
     * Checks that the newest log entry comes first.
     */
    @Test
    fun groupNotificationLog_differentEvents_sortsNewestFirst() {
        val records = listOf(
            record("older", createdAt = Timestamp(100, 0)),
            record("newest", createdAt = Timestamp(300, 0)),
            record("middle", createdAt = Timestamp(200, 0))
        )

        val log = groupNotificationLog(records)

        assertEquals(listOf("newest", "middle", "older"), log.map { it.eventId })
    }

    /**
     * Checks that each notification type has a readable source.
     */
    @Test
    fun source_eachType_returnsLabel() {
        val labels = NotificationType.entries.associateWith {
            sentNotification("Message").copy(type = it).source()
        }

        assertEquals(
            mapOf(
                NotificationType.LOTTERY_WIN to "Lottery result: won",
                NotificationType.LOTTERY_LOSE to "Lottery result: lost",
                NotificationType.RESERVATION_CONFIRMED to "Reservation confirmed",
                NotificationType.RESERVATION_CANCELLED to "Reservation cancelled",
                NotificationType.OWNER_MESSAGE to "Owner message",
                NotificationType.LOTTERY_CANCELLED to  "Lottery cancelled",
                NotificationType.CAMPSITE_REMOVED_FROM_LOTTERY to  "Campsite removed from lottery",
                NotificationType.ENTRANT_REMOVED_FROM_LOTTERY to "Entrant removed from lottery"
            ),
            labels
        )
    }

    private fun record(
        eventId: String,
        recipientId: String = "entrant1",
        origin: NotificationOrigin = NotificationOrigin.OWNER,
        type: NotificationType = NotificationType.OWNER_MESSAGE,
        content: String = "Message",
        createdAt: Timestamp? = Timestamp(100, 0)
    ) = Notification(
        id = "$eventId-$recipientId",
        eventId = eventId,
        senderId = "owner1",
        recipientId = recipientId,
        origin = origin,
        type = type,
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

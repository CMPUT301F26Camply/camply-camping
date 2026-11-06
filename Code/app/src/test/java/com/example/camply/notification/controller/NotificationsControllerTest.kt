package com.example.camply.notification.controller

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for keeping the shared notification screen up to date (US 02.10.02).
 */
class NotificationsControllerTest {

    private lateinit var model: NotificationsModel
    private lateinit var repository: TestNotificationRepository
    private lateinit var controller: NotificationsController

    /**
     * Creates the model, repository, and controller before each test.
     */
    @Before
    fun setUp() {
        model = NotificationsModel()
        repository = TestNotificationRepository()
        controller = NotificationsController(model, repository)
    }

    /**
     * Checks that an owner's screen shows loading until the first update arrives.
     */
    @Test
    fun startObserving_owner_showsLoadingUntilUpdate() {
        controller.startObserving(OWNER_ID, "owner")

        assertTrue(model.isLoading)
        assertEquals(OWNER_ID, repository.senderId)

        repository.sendUpdate(emptyList())

        assertFalse(model.isLoading)
    }

    /**
     * Checks that an update is grouped by send and leaves out system notifications.
     */
    @Test
    fun startObserving_ownerUpdate_showsGroupedNotifications() {
        controller.startObserving(OWNER_ID, "owner")

        repository.sendUpdate(
            listOf(
                record("event1", "entrant1"),
                record("event1", "entrant2"),
                record("lottery-result:lottery1", "entrant1", NotificationOrigin.SYSTEM)
            )
        )

        val sent = model.sentNotifications.single()
        assertEquals("event1", sent.eventId)
        assertEquals(2, sent.recipientCount)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that a live update replaces the displayed list.
     */
    @Test
    fun startObserving_laterUpdate_replacesList() {
        controller.startObserving(OWNER_ID, "owner")
        repository.sendUpdate(listOf(record("event1", "entrant1")))

        repository.sendUpdate(listOf(record("event2", "entrant1")))

        assertEquals(listOf("event2"), model.sentNotifications.map { it.eventId })
    }

    /**
     * Checks that starting again stops the previous listener.
     */
    @Test
    fun startObserving_calledAgain_stopsPreviousListener() {
        controller.startObserving(OWNER_ID, "owner")

        controller.startObserving(OWNER_ID, "owner")

        assertEquals(2, repository.observeCount)
        assertEquals(1, repository.stopCount)
    }

    /**
     * Checks that closing the screen stops the listener only once.
     */
    @Test
    fun stopObserving_listening_stopsListener() {
        controller.startObserving(OWNER_ID, "owner")

        controller.stopObserving()
        controller.stopObserving()

        assertEquals(1, repository.stopCount)
    }

    private fun record(
        eventId: String,
        recipientId: String,
        origin: NotificationOrigin = NotificationOrigin.OWNER
    ) = Notification(
        id = "$eventId-$recipientId",
        eventId = eventId,
        senderId = OWNER_ID,
        recipientId = recipientId,
        origin = origin,
        type = NotificationType.OWNER_MESSAGE,
        title = "Title",
        content = "Message"
    )

    /**
     * Lets tests trigger listener updates without accessing Firestore.
     */
    private class TestNotificationRepository : NotificationRepository {
        var senderId: String? = null
        var observeCount = 0
        var stopCount = 0
        private var onUpdate: (List<Notification>) -> Unit = {}

        fun sendUpdate(records: List<Notification>) = onUpdate(records)

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit {
            this.senderId = senderId
            this.onUpdate = onUpdate
            observeCount++
            return { stopCount++ }
        }

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult =
            error("Not used by controller tests")

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by controller tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by controller tests")
    }

    private companion object {
        const val OWNER_ID = "owner1"
    }
}

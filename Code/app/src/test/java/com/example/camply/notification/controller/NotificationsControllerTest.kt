package com.example.camply.notification.controller

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.model.SentNotification
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for keeping the shared notification screen up to date (US 02.10.02).
 */
class NotificationsControllerTest {
    @Test
    fun entrantReceivesUpdatesAndUnreadState() {
        controller.startObserving("entrant1", "entrant")
        assertEquals("entrant1", repository.recipientId)
        repository.sendUpdate(listOf(record("win", "entrant1", NotificationOrigin.SYSTEM)))
        assertEquals(1, model.receivedNotifications.size)
        assertTrue(model.hasUnread)
        assertTrue(model.sentNotifications.isEmpty())
    }

    @Test
    fun openingReceivedNotificationMarksItRead() = runBlocking {
        controller.startObserving("entrant1", "entrant")
        val notification = record("win", "entrant1", NotificationOrigin.SYSTEM)
        repository.sendUpdate(listOf(notification))
        controller.onReceivedNotificationSelected(notification)
        assertEquals(notification.id to "entrant1", repository.markedRead)
        assertFalse(model.hasUnread)
        assertEquals(true, model.selectedReceivedNotification?.isRead)
    }

    @Test
    fun failedReadKeepsMessageAndUnreadUntilRetrySucceeds() = runBlocking {
        controller.startObserving("entrant1", "entrant")
        val notification = record("win", "entrant1", NotificationOrigin.SYSTEM)
        repository.sendUpdate(listOf(notification))
        repository.failRead = true
        controller.onReceivedNotificationSelected(notification)
        assertTrue(model.hasUnread)
        assertFalse(model.isMarkingRead)
        assertEquals(notification, model.selectedReceivedNotification)
        assertEquals("Could not mark this notification as read.", model.readError)
        repository.failRead = false
        controller.markSelectedAsRead()
        assertFalse(model.hasUnread)
        assertNull(model.readError)
    }

    @Test
    fun changingUserClearsDataAndIgnoresPreviousListener() {
        controller.startObserving("entrant1", "entrant")
        repository.sendUpdate(listOf(record("one", "entrant1")))
        val oldUpdate = repository.savedUpdate()
        controller.startObserving("entrant2", "entrant")
        assertTrue(model.receivedNotifications.isEmpty())
        assertFalse(model.hasUnread)
        oldUpdate(listOf(record("one", "entrant1")))
        assertTrue(model.receivedNotifications.isEmpty())
        repository.sendUpdate(listOf(record("two", "entrant2")))
        assertEquals("entrant2", model.receivedNotifications.single().recipientId)
    }

    @Test
    fun foreignRecipientCannotBeOpenedOrMarkedRead() = runBlocking {
        controller.startObserving("entrant1", "entrant")
        controller.onReceivedNotificationSelected(record("one", "entrant2"))
        assertNull(model.selectedReceivedNotification)
        assertNull(repository.markedRead)
    }

    private lateinit var model: NotificationsModel
    private lateinit var repository: TestNotificationRepository
    private lateinit var nameRepository: TestRecipientNameRepository
    private lateinit var controller: NotificationsController

    /**
     * Creates the model, repository, and controller before each test.
     */
    @Before
    fun setUp() {
        model = NotificationsModel()
        repository = TestNotificationRepository()
        nameRepository = TestRecipientNameRepository()
        controller = NotificationsController(model, repository, nameRepository)
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

    /**
     * Checks that an entrant listens for the notifications sent to them (US 01.05.01).
     */
    @Test
    fun startObserving_entrant_listensForReceived() {
        controller.startObserving("entrant1", "entrant")

        assertEquals("entrant1", repository.recipientId)
        assertEquals(0, repository.observeCount)
        assertEquals(1, repository.receivedObserveCount)
        assertTrue(model.isLoading)
    }

    /**
     * Checks that an update shows the entrant's received notifications (US 01.05.01).
     */
    @Test
    fun startObserving_entrantUpdate_showsReceivedNotifications() {
        val received = record("reservation-confirmed:reservation1", "entrant1", NotificationOrigin.SYSTEM)
        controller.startObserving("entrant1", "entrant")

        repository.sendReceivedUpdate(listOf(received))

        assertEquals(listOf(received), model.receivedNotifications)
        assertFalse(model.isLoading)
    }

    /**
     * Checks that admins get an empty screen without reading any notifications.
     */
    @Test
    fun startObserving_admin_showsEmptyWithoutListening() {
        controller.startObserving("admin1", "admin")

        assertEquals(0, repository.observeCount)
        assertNull(repository.recipientId)
        assertFalse(model.isLoading)
    }

    /**
     * Checks that selecting a notification loads its recipients' names in order.
     */
    @Test
    fun onNotificationSelected_validRecipients_showsNames() = runBlocking {
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam")
        val notification = sentNotification()

        controller.onNotificationSelected(notification)

        assertEquals(notification, model.selectedNotification)
        assertEquals(listOf("Alex", "Sam"), model.recipientNames)
        assertFalse(model.isLoadingRecipients)
    }

    /**
     * Checks that a failed name lookup keeps the notification open and shows an error.
     */
    @Test
    fun onNotificationSelected_lookupFails_keepsMessageAndShowsError() = runBlocking {
        nameRepository.shouldFail = true
        val notification = sentNotification()

        controller.onNotificationSelected(notification)

        assertEquals(notification, model.selectedNotification)
        assertEquals("Could not read names.", model.recipientsError)
        assertFalse(model.isLoadingRecipients)
    }

    /**
     * Checks that going back to the list clears the selection and names.
     */
    @Test
    fun onBackToList_notificationSelected_clearsSelection() = runBlocking {
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam")
        controller.onNotificationSelected(sentNotification())

        controller.onBackToList()

        assertNull(model.selectedNotification)
        assertTrue(model.recipientNames.isEmpty())
    }

    private fun sentNotification() = SentNotification(
        eventId = "event1",
        title = "Title",
        content = "Message",
        recipientIds = listOf("entrant1", "entrant2"),
        sentAt = null
    )

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
        var recipientId: String? = null
        var markedRead: Pair<String, String>? = null
        var failRead = false
        var senderId: String? = null
        var observeCount = 0
        var receivedObserveCount = 0
        var stopCount = 0
        private var onUpdate: (List<Notification>) -> Unit = {}
        private var onReceivedUpdate: (List<Notification>) -> Unit = {}

        fun sendUpdate(records: List<Notification>) = onUpdate(records)
        fun savedUpdate(): (List<Notification>) -> Unit = onUpdate

        fun sendReceivedUpdate(records: List<Notification>) = onReceivedUpdate(records)

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
        ): () -> Unit {
            this.recipientId = recipientId
            onReceivedUpdate = onUpdate
            this.onUpdate = onUpdate
            receivedObserveCount++
            return { stopCount++ }
        }

        override suspend fun markAsRead(notificationId: String, recipientId: String) {
            if (failRead) throw Exception("Write failed")
            markedRead = notificationId to recipientId
        }
    }

    /**
     * Provides recipient names without accessing Firestore.
     */
    private class TestRecipientNameRepository : RecipientNameRepository {
        var names: Map<String, String> = emptyMap()
        var shouldFail = false

        override suspend fun getRecipientName(userId: String): String? {
            if (shouldFail) throw Exception("Could not read names.")
            return names[userId]
        }
    }

    private companion object {
        const val OWNER_ID = "owner1"
    }
}

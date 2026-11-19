package com.example.camply.notification.controller

import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for device pop-up notifications.
 *
 * A mock repository replaces Firestore, so the tests run offline.
 */
class DeviceNotificationsTest {
    private val userId = "entrant_1"
    private val repository = MockNotificationRepository()
    private val controller = NotificationsController(NotificationsModel(), repository, NoRecipientNames)
    private val shown = mutableListOf<Pair<String, String>>()

    /**
     * This test is designed to check that a new unread pop-up notification
     * is received by the user.
     */
    @Test
    fun startDeviceNotifications_newPopUpNotification() {
        start(emptyList(), listOf(notification("notification_1")))
        assertEquals(listOf("Title_notification_1" to "Body_notification_1"), shown)
    }

    /**
     * This test checks that notifications already
     * in the inbox do not show a pop-up.
     */
    @Test
    fun noPopUpForExistingInbox() {
        start(listOf(notification("notification_1"), notification("notification_2")))
        assertTrue(shown.isEmpty())
    }

    /**
     * Checks that only
     * the most recent notification pops up.
     */
    @Test
    fun showLatestNotificationPopUp() {
        start(
            listOf(notification("notification_1")),
            listOf(notification("notification_1"), notification("notification_2"))
        )
        assertEquals(listOf("Title_notification_2" to "Body_notification_2"), shown)
    }

    /**
     * Test: A notification without a creation time does not pop up.
     */
    @Test
    fun missingCreatedAt_showsNoPopUp() {
        start(emptyList(), listOf(notification("notification_1", createdAt = null)))
        assertTrue(shown.isEmpty())
    }

    /**
     * Test: the controller listens for the given user's received notifications.
     */
    @Test
    fun listensForGivenUser() {
        start(emptyList())
        assertEquals(userId, repository.observedRecipientId)
    }

    // Starts device notifications, recording each pop-up instead of showing it.
    // The first snapshot is the existing inbox; each later one is a new update.
    private fun start(vararg snapshots: List<Notification>) {
        repository.snapshots = snapshots.toList()
        controller.startDeviceNotifications(userId) { title, body -> shown += title to body }
    }

    private fun notification(
        id: String,
        createdAt: Timestamp? = Timestamp(Date())
    ) = Notification(
        id = id,
        recipientId = userId,
        title = "Title_$id",
        content = "Body_$id",
        createdAt = createdAt,
        isRead = false
    )

    // Replaces Firestore: plays back the given snapshots in order, like a snapshot listener.
    private class MockNotificationRepository : NotificationRepository {
        var snapshots: List<List<Notification>> = emptyList()
        var observedRecipientId: String? = null

        override suspend fun createIfAbsent(id: String, notification: NewNotification) =
            SendResult.CREATED

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit {
            observedRecipientId = recipientId
            snapshots.forEach(onUpdate)
            return { }
        }

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = { }

        override suspend fun markAsRead(notificationId: String, recipientId: String) = Unit
    }

    // Device notifications never look up recipient names.
    private object NoRecipientNames : RecipientNameRepository {
        override suspend fun getRecipientName(id: String): String? = null
    }
}
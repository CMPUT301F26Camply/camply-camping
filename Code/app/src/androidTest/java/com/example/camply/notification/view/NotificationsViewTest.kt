package com.example.camply.notification.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.notification.controller.NotificationsController
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the shared notification screen (US 02.10.02).
 */
class NotificationsViewTest {
    @Test
    fun entrantCanReadWinAndLoseMessagesAndReturnToList() {
        repository.records = listOf(
            record(OWNER_ID).copy(id = "win", title = "Lottery won", content = "Choose your dates.", type = NotificationType.LOTTERY_WIN),
            record(OWNER_ID).copy(id = "lose", title = "Lottery lost", content = "Try another lottery.", type = NotificationType.LOTTERY_LOSE)
        )
        showScreen("entrant")
        compose.onNodeWithText("Lottery won").assertIsDisplayed().performClick()
        compose.onNodeWithText("Choose your dates.").assertIsDisplayed()
        compose.runOnIdle { assertEquals("win" to OWNER_ID, repository.markedRead) }
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Lottery lost").assertIsDisplayed().performClick()
        compose.onNodeWithText("Try another lottery.").assertIsDisplayed()
    }

    @Test
    fun entrantReadFailureShowsMessageAndRetry() {
        repository.records = listOf(record(OWNER_ID).copy(id = "win", title = "Lottery won"))
        repository.failRead = true
        showScreen("entrant")
        compose.onNodeWithText("Lottery won").performClick()
        compose.onNodeWithText("Could not mark this notification as read.").assertIsDisplayed()
        compose.runOnIdle { repository.failRead = false }
        compose.onNodeWithText("Retry read status").performClick()
        compose.onNodeWithText("Could not mark this notification as read.").assertDoesNotExist()
    }

    @get:Rule
    val compose = createComposeRule()

    private lateinit var repository: TestNotificationRepository
    private lateinit var nameRepository: TestRecipientNameRepository
    private lateinit var view: NotificationsView

    /**
     * Creates the view with a fake repository before each test.
     */
    @Before
    fun setUp() {
        repository = TestNotificationRepository()
        nameRepository = TestRecipientNameRepository()
        val model = NotificationsModel()
        view = NotificationsView(
            model,
            NotificationsController(model, repository, nameRepository)
        )
    }

    /**
     * Checks that a sent notification shows its preview, sent time, and number of recipients.
     */
    @Test
    fun display_sentNotification_showsPreviewTimeAndCount() {
        val sentAt = Timestamp(1_800_000_000, 0)
        repository.records = listOf(
            record("entrant1", sentAt),
            record("entrant2", sentAt)
        )
        val expectedTime = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(sentAt.toDate())

        showScreen("owner")

        compose.onNodeWithText(MESSAGE).assertIsDisplayed()
        compose.onNodeWithText(expectedTime).assertIsDisplayed()
        compose.onNodeWithText("2 recipients").assertIsDisplayed()
    }

    /**
     * Checks that one recipient uses the singular label.
     */
    @Test
    fun display_oneRecipient_usesSingularLabel() {
        repository.records = listOf(record("entrant1"))

        showScreen("owner")

        compose.onNodeWithText("1 recipient").assertIsDisplayed()
    }

    /**
     * Checks that an owner with nothing sent sees the owner's empty message.
     */
    @Test
    fun display_ownerWithNothingSent_showsOwnerMessage() {
        showScreen("owner")

        compose.onNodeWithText("You haven't sent any notifications yet.").assertIsDisplayed()
    }

    /**
     * Checks that an entrant sees the shared empty message.
     */
    @Test
    fun display_entrant_showsEmptyMessage() {
        showScreen("entrant")

        compose.onNodeWithText("No notifications yet.").assertIsDisplayed()
    }

    /**
     * Checks that a listener failure shows the error and that trying again restarts the listener.
     */
    @Test
    fun display_listenerFails_showsErrorAndRetries() {
        repository.records = listOf(record("entrant1"))
        repository.shouldFail = true

        showScreen("owner")

        compose.onNodeWithText("Could not read notifications.").assertIsDisplayed()

        repository.shouldFail = false
        compose.onNodeWithText("Try again").performClick()

        compose.onNodeWithText(MESSAGE).assertIsDisplayed()
        compose.onNodeWithText("Could not read notifications.").assertDoesNotExist()
    }

    /**
     * Checks that leaving the screen stops the listener.
     */
    @Test
    fun display_screenCloses_stopsListener() {
        var screenOpen by mutableStateOf(true)

        compose.setContent {
            if (screenOpen) view.Display(OWNER_ID, "owner")
        }
        compose.runOnIdle { assertEquals(0, repository.stopCount) }

        screenOpen = false

        compose.runOnIdle { assertEquals(1, repository.stopCount) }
    }

    /**
     * Checks that selecting a notification shows its full message and recipients.
     */
    @Test
    fun notificationCard_click_showsFullMessageAndRecipients() {
        val fullMessage = "a".repeat(100)
        repository.records = listOf(
            record("entrant1", content = fullMessage),
            record("entrant2", content = fullMessage)
        )
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam")

        showScreen("owner")
        compose.onNodeWithText("a".repeat(80) + "…").performClick()

        compose.onNodeWithText("Notification details").assertIsDisplayed()
        compose.onNodeWithText(fullMessage).assertIsDisplayed()
        compose.onNodeWithText("RECIPIENTS (2)").assertIsDisplayed()
        compose.onNodeWithText("Alex").assertIsDisplayed()
        compose.onNodeWithText("Sam").assertIsDisplayed()
    }

    /**
     * Checks that the back button returns from the details to the list.
     */
    @Test
    fun detailsBackButton_click_returnsToList() {
        repository.records = listOf(record("entrant1"))
        nameRepository.names = mapOf("entrant1" to "Alex")

        showScreen("owner")
        compose.onNodeWithText(MESSAGE).performClick()
        compose.onNodeWithText("Alex").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Notifications").assertIsDisplayed()
        compose.onNodeWithText("1 recipient").assertIsDisplayed()
        compose.onNodeWithText("Alex").assertDoesNotExist()
    }

    /**
     * Checks that a reservation confirmation appears under system notifications (US 01.05.01).
     */
    @Test
    fun display_entrantWithConfirmation_showsUnderSystemSection() {
        repository.receivedRecords = listOf(
            receivedRecord(NotificationOrigin.SYSTEM, "Reservation confirmed", "Your reservation is confirmed.")
        )

        showScreen("entrant")

        compose.onNodeWithText("System notifications").assertIsDisplayed()
        compose.onNodeWithText("Reservation confirmed").assertIsDisplayed()
        compose.onNodeWithText("Your reservation is confirmed.").assertIsDisplayed()
        compose.onNodeWithText("Messages").assertDoesNotExist()
    }

    /**
     * Checks that an owner's message appears under messages (US 01.05.01).
     */
    @Test
    fun display_entrantWithOwnerMessage_showsUnderMessages() {
        repository.receivedRecords = listOf(
            receivedRecord(NotificationOrigin.OWNER, "Quiet hours", MESSAGE)
        )

        showScreen("entrant")

        compose.onNodeWithText("Messages").assertIsDisplayed()
        compose.onNodeWithText("Quiet hours").assertIsDisplayed()
        compose.onNodeWithText(MESSAGE).assertIsDisplayed()
        compose.onNodeWithText("System notifications").assertDoesNotExist()
    }

    private fun showScreen(role: String) {
        compose.setContent { view.Display(OWNER_ID, role) }
    }

    private fun record(
        recipientId: String,
        sentAt: Timestamp = Timestamp(1_800_000_000, 0),
        content: String = MESSAGE
    ) = Notification(
        id = "event1-$recipientId",
        eventId = "event1",
        senderId = OWNER_ID,
        recipientId = recipientId,
        origin = NotificationOrigin.OWNER,
        type = NotificationType.OWNER_MESSAGE,
        title = "Title",
        content = content,
        createdAt = sentAt
    )

    private fun receivedRecord(origin: NotificationOrigin, title: String, content: String) = Notification(
        id = "received1",
        eventId = "event1",
        senderId = OWNER_ID,
        recipientId = "entrant1",
        origin = origin,
        type = NotificationType.OWNER_MESSAGE,
        title = title,
        content = content,
        createdAt = Timestamp(1_800_000_000, 0)
    )

    /**
     * Delivers notification records without Firestore for testing purpose.
     */
    private class TestNotificationRepository : NotificationRepository {
        var markedRead: Pair<String, String>? = null
        var failRead = false
        var records: List<Notification> = emptyList()
        var receivedRecords: List<Notification> = emptyList()
        var shouldFail = false
        var stopCount = 0

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit {
            if (shouldFail) onError(Exception("Could not read notifications.")) else onUpdate(records)
            return { stopCount++ }
        }

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult =
            error("Not used by view tests")

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit {
            if (shouldFail) onError(Exception("Could not read notifications."))
            else onUpdate(receivedRecords + records.filter { it.recipientId == recipientId })
            return { stopCount++ }
        }

        override suspend fun markAsRead(notificationId: String, recipientId: String) {
            if (failRead) throw Exception("Write failed")
            markedRead = notificationId to recipientId
        }
    }

    /**
     * Provides recipient names without Firestore for testing purpose.
     */
    private class TestRecipientNameRepository : RecipientNameRepository {
        var names: Map<String, String> = emptyMap()

        override suspend fun getRecipientName(userId: String): String? = names[userId]
    }

    private companion object {
        const val OWNER_ID = "owner1"
        const val MESSAGE = "Quiet hours start at 10 PM."
    }
}

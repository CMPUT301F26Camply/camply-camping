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

    @get:Rule
    val compose = createComposeRule()

    private lateinit var repository: TestNotificationRepository
    private lateinit var view: NotificationsView

    /**
     * Creates the view with a fake repository before each test.
     */
    @Before
    fun setUp() {
        repository = TestNotificationRepository()
        val model = NotificationsModel()
        view = NotificationsView(
            model,
            NotificationsController(model, repository, TestRecipientNameRepository())
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

    private fun showScreen(role: String) {
        compose.setContent { view.Display(OWNER_ID, role) }
    }

    private fun record(recipientId: String, sentAt: Timestamp = Timestamp(1_800_000_000, 0)) = Notification(
        id = "event1-$recipientId",
        eventId = "event1",
        senderId = OWNER_ID,
        recipientId = recipientId,
        origin = NotificationOrigin.OWNER,
        type = NotificationType.OWNER_MESSAGE,
        title = "Title",
        content = MESSAGE,
        createdAt = sentAt
    )

    /**
     * Delivers notification records without Firestore for testing purpose.
     */
    private class TestNotificationRepository : NotificationRepository {
        var records: List<Notification> = emptyList()
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
        ): () -> Unit = error("Not used by view tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by view tests")
    }

    /**
     * Provides recipient names without Firestore for testing purpose.
     */
    private class TestRecipientNameRepository : RecipientNameRepository {
        override suspend fun getRecipientName(userId: String): String? = null
    }

    private companion object {
        const val OWNER_ID = "owner1"
        const val MESSAGE = "Quiet hours start at 10 PM."
    }
}

package com.example.camply.notification.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.notification.controller.NotificationsController
import com.example.camply.notification.model.*
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.model.Screen
import com.example.camply.shared.navigation.view.CamplyBottomNavBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntrantNotificationNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unreadBadgeAppearsOutsideInboxAndClearsAfterReadingWithoutAnotherListener() {
        val repository = InboxRepository()
        val model = NotificationsModel()
        val controller = NotificationsController(model, repository, object : RecipientNameRepository {
            override suspend fun getRecipientName(userId: String): String? = null
        })
        val view = NotificationsView(model, controller)
        val navigation = NavigationModel()
        val bar = CamplyBottomNavBar(navigation, NavigationController(navigation))
        compose.setContent {
            DisposableEffect(Unit) {
                controller.startObserving("entrant", "entrant")
                onDispose { controller.stopObserving() }
            }
            Column {
                bar.Display("entrant", model.hasUnread)
                if (navigation.getCurrentScreen() == Screen.NOTIFICATIONS) {
                    view.Display("entrant", "entrant", manageObservation = false)
                } else {
                    Text("Browse screen")
                }
            }
        }
        compose.onNodeWithText("Browse screen").assertIsDisplayed()
        compose.runOnIdle {
            repository.update(listOf(Notification(
                id = "win", recipientId = "entrant", title = "Lottery won", content = "Choose dates"
            )))
        }
        compose.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Notifications").performClick()
        compose.onNodeWithText("Lottery won").performClick()
        compose.onNodeWithText("Choose dates").assertIsDisplayed()
        compose.onNodeWithContentDescription("Unread notifications", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, repository.listenerCount) }
    }

    private class InboxRepository : NotificationRepository {
        lateinit var update: (List<Notification>) -> Unit
        var listenerCount = 0
        override fun observeReceived(
            recipientId: String, onUpdate: (List<Notification>) -> Unit, onError: (Exception) -> Unit
        ): () -> Unit {
            listenerCount++
            update = onUpdate
            onUpdate(emptyList())
            return {}
        }
        override suspend fun markAsRead(notificationId: String, recipientId: String) = Unit
        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult =
            error("Not used")
        override fun observeSent(
            senderId: String, onUpdate: (List<Notification>) -> Unit, onError: (Exception) -> Unit
        ): () -> Unit = error("Not used")
    }
}

package com.example.camply.notification.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.notification.controller.NotificationsController
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SentNotification
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Displays the shared notification screen (US 02.10.02).
 *
 * @param model The model containing the screen state.
 * @param controller The controller used to keep the notifications up to date.
 */
class NotificationsView(
    private val model: NotificationsModel,
    private val controller: NotificationsController
) {

    /**
     * Displays a user's notifications, or the selected notification's details,
     * and keeps them up to date while the screen is open.
     *
     * @param userId The user's Firebase user ID.
     * @param role The user's role.
     */
    @Composable
    fun Display(userId: String, role: String) {
        // Kept here because a card's own scope ends when the details replace the list.
        val scope = rememberCoroutineScope()
        val error = model.errorMessage
        val selected = model.selectedNotification

        DisposableEffect(userId, role) {
            controller.startObserving(userId, role)
            onDispose { controller.stopObserving() }
        }

        CamplyScreen(title = if (selected != null) "Notification details" else "Notifications") {
            // Loading and errors come first
            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }

                error != null -> CenteredMessage(error, "Try again") {
                    controller.startObserving(userId, role)
                }

                selected != null -> NotificationDetails(selected)

                model.sentNotifications.isEmpty() -> CenteredMessage(emptyMessage(role))

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(model.sentNotifications, key = { it.eventId }) { notification ->
                        SentNotificationCard(notification) {
                            scope.launch { controller.onNotificationSelected(notification) }
                        }
                    }
                }
            }
        }
    }

    // Shows one sent notification's preview, sent time, and number of recipients.
    @Composable
    private fun SentNotificationCard(notification: SentNotification, onClick: () -> Unit) =
        OrangeCard(Modifier.clickable(onClick = onClick)) {
            Text(notification.preview(), fontSize = 14.sp, color = CamplyPalette.Ink)
            Text(sentTimeText(notification), fontSize = 13.sp, color = CamplyPalette.Ink)
            Text(recipientCountText(notification), fontSize = 13.sp, color = CamplyPalette.Ink)
        }

    // Shows a sent notification's full message and recipients.
    @Composable
    private fun NotificationDetails(notification: SentNotification) {
        BackHandler { controller.onBackToList() }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrangeCard {
                Text(
                    notification.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CamplyPalette.Ink
                )
                Spacer(Modifier.height(6.dp))
                Text(notification.content, fontSize = 14.sp, color = CamplyPalette.Ink)
                Spacer(Modifier.height(6.dp))
                Text(sentTimeText(notification), fontSize = 13.sp, color = CamplyPalette.Ink)
            }

            OrangeCard {
                CardLabel("Recipients (${notification.recipientCount})")
                RecipientsContent()
            }

            SecondaryButton("Back", controller::onBackToList)
        }
    }

    // Shows the recipient names, or their loading or error state.
    @Composable
    private fun RecipientsContent() {
        val error = model.recipientsError

        when {
            model.isLoadingRecipients -> CircularProgressIndicator(color = CamplyPalette.Ink)

            error != null -> Text(error, fontSize = 14.sp, color = CamplyPalette.Ink)

            else -> model.recipientNames.forEach { name ->
                Text(name, fontSize = 14.sp, color = CamplyPalette.Ink)
            }
        }
    }

    private fun emptyMessage(role: String): String =
        if (role == "owner") "You haven't sent any notifications yet." else "No notifications yet."

    private fun sentTimeText(notification: SentNotification): String {
        val sentAt = notification.sentAt ?: return "Sending…"
        return SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(sentAt.toDate())
    }

    private fun recipientCountText(notification: SentNotification): String {
        val count = notification.recipientCount
        return if (count == 1) "1 recipient" else "$count recipients"
    }
}

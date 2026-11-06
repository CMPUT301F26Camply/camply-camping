package com.example.camply.notification.controller

import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.SentNotification
import com.example.camply.notification.model.groupSentNotifications
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Keeps the shared notification screen up to date (US 02.10.02).
 *
 * @param model The model storing the screen state.
 * @param repository The repository used to observe notifications.
 * @param recipientNameRepository The repository used to retrieve recipient names.
 */
class NotificationsController(
    private val model: NotificationsModel,
    private val repository: NotificationRepository,
    private val recipientNameRepository: RecipientNameRepository
) {
    private var stopListening: (() -> Unit)? = null

    /**
     * Starts showing a user's notifications, replacing any earlier listener.
     *
     * Only campsite owners have content so far. Other roles see an empty screen.
     *
     * @param userId The user's Firebase user ID.
     * @param role The user's role.
     */
    fun startObserving(userId: String, role: String) {
        stopObserving()
        model.clearSelection()

        if (role != "owner") {
            model.showSentNotifications(emptyList())
            return
        }

        model.showLoading()
        stopListening = repository.observeSent(
            senderId = userId,
            onUpdate = { records -> model.showSentNotifications(groupSentNotifications(records)) },
            onError = { error -> model.showError(error.message ?: "Could not load notifications.") }
        )
    }

    /**
     * Stops listening for notification updates, such as when the screen closes.
     */
    fun stopObserving() {
        stopListening?.invoke()
        stopListening = null
    }

    /**
     * Opens a sent notification's details and loads its recipient names.
     *
     * @param notification The notification selected by the owner.
     */
    suspend fun onNotificationSelected(notification: SentNotification) {
        model.selectNotification(notification)

        try {
            val names = notification.recipientIds.map { id ->
                recipientNameRepository.getRecipientName(id) ?: "Unknown recipient"
            }

            // The owner may have gone back while the names were loading.
            if (model.selectedNotification == notification) {
                model.showRecipientNames(names)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (model.selectedNotification == notification) {
                model.showRecipientsError(e.message ?: "Could not load recipients.")
            }
        }
    }

    /**
     * Returns from a notification's details to the list.
     */
    fun onBackToList() = model.clearSelection()
}

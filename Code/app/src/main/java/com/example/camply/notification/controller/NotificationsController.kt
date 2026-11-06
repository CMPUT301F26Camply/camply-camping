package com.example.camply.notification.controller

import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.Notification
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
    private var observationVersion = 0
    private var observedUserId: String? = null
    private var observedRole: String? = null

    /**
     * Starts showing a user's notifications, replacing any earlier listener.
     *
     * Owners see sent history; entrants see their received inbox.
     *
     * @param userId The user's Firebase user ID.
     * @param role The user's role.
     */
    fun startObserving(userId: String, role: String) {
        stopObserving()
        val version = observationVersion
        observedUserId = userId
        observedRole = role
        model.reset()

        if (role != "owner" && role != "entrant") {
            model.showSentNotifications(emptyList())
            return
        }

        model.showLoading()
        stopListening = if (role == "entrant") repository.observeReceived(
            recipientId = userId,
            onUpdate = { records ->
                if (version == observationVersion) model.showReceivedNotifications(records)
            },
            onError = { error ->
                if (version == observationVersion) model.showError(error.message ?: "Could not load notifications.")
            }
        ) else repository.observeSent(
            senderId = userId,
            onUpdate = { records ->
                if (version == observationVersion) model.showSentNotifications(groupSentNotifications(records))
            },
            onError = { error ->
                if (version == observationVersion) model.showError(error.message ?: "Could not load notifications.")
            }
        )
    }

    /**
     * Stops listening for notification updates, such as when the screen closes.
     */
    fun stopObserving() {
        observationVersion++
        observedUserId = null
        observedRole = null
        stopListening?.invoke()
        stopListening = null
        model.reset()
    }

    /** Opens only a received record belonging to the active entrant. */
    suspend fun onReceivedNotificationSelected(notification: Notification) {
        if (observedRole != "entrant" || notification.recipientId != observedUserId) return
        val record = model.receivedNotifications.firstOrNull { it.id == notification.id } ?: return
        model.selectReceivedNotification(record)
        markSelectedAsRead()
    }

    suspend fun markSelectedAsRead() {
        val notification = model.selectedReceivedNotification ?: return
        val userId = observedUserId ?: return
        if (observedRole != "entrant" || notification.recipientId != userId ||
            notification.isRead || model.isMarkingRead) return
        val version = observationVersion
        model.showMarkingRead()
        try {
            repository.markAsRead(notification.id, userId)
            if (version == observationVersion) model.finishMarkingRead(notification.id)
        } catch (error: CancellationException) {
            if (version == observationVersion) model.finishMarkingRead(notification.id, "Read status was not confirmed. Please retry.")
            throw error
        } catch (error: Exception) {
            if (version == observationVersion) model.finishMarkingRead(notification.id, "Could not mark this notification as read.")
        }
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

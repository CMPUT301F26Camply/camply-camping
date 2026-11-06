package com.example.camply.notification.controller

import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.model.groupSentNotifications
import com.example.camply.notification.repository.NotificationRepository

/**
 * Keeps the shared notification screen up to date (US 02.10.02).
 *
 * @param model The model storing the screen state.
 * @param repository The repository used to observe notifications.
 */
class NotificationsController(
    private val model: NotificationsModel,
    private val repository: NotificationRepository
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
}

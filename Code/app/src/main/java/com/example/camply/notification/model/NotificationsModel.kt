package com.example.camply.notification.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the state of the shared notification screen (US 02.10.02).
 */
class NotificationsModel {
    var receivedNotifications: List<Notification> by mutableStateOf(emptyList())
        private set

    var selectedReceivedNotification: Notification? by mutableStateOf(null)
        private set

    var isMarkingRead by mutableStateOf(false)
        private set

    var readError: String? by mutableStateOf(null)
        private set

    val hasUnread: Boolean get() = receivedNotifications.any { !it.isRead }

    fun reset() {
        sentNotifications = emptyList()
        receivedNotifications = emptyList()
        clearSelection()
        isLoading = false
        errorMessage = null
    }

    fun showReceivedNotifications(list: List<Notification>) {
        receivedNotifications = list
        selectedReceivedNotification = selectedReceivedNotification?.let { selected ->
            list.firstOrNull { it.id == selected.id }
        }
        isLoading = false
        errorMessage = null
    }

    fun selectReceivedNotification(notification: Notification) {
        clearSelection()
        selectedReceivedNotification = notification
    }

    fun showMarkingRead() {
        isMarkingRead = true
        readError = null
    }

    fun finishMarkingRead(id: String, error: String? = null) {
        if (error == null) {
            receivedNotifications = receivedNotifications.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
        }
        if (selectedReceivedNotification?.id == id) {
            isMarkingRead = false
            readError = error
            if (error == null) selectedReceivedNotification = selectedReceivedNotification?.copy(isRead = true)
        }
    }

    var sentNotifications: List<SentNotification> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var selectedNotification: SentNotification? by mutableStateOf(null)
        private set

    var recipientNames: List<String> by mutableStateOf(emptyList())
        private set

    var isLoadingRecipients: Boolean by mutableStateOf(false)
        private set

    var recipientsError: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model when notifications are being loaded.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * Updates the model with the notifications to display.
     *
     * @param list The owner's sent notifications.
     */
    fun showSentNotifications(list: List<SentNotification>) {
        sentNotifications = list
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when notifications can not be loaded.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }

    /**
     * Stores the notification whose details should be displayed while its recipients load.
     *
     * @param notification The selected notification.
     */
    fun selectNotification(notification: SentNotification) {
        selectedNotification = notification
        recipientNames = emptyList()
        isLoadingRecipients = true
        recipientsError = null
    }

    /**
     * Updates the model with the selected notification's recipient names.
     *
     * @param names The names to display.
     */
    fun showRecipientNames(names: List<String>) {
        recipientNames = names
        isLoadingRecipients = false
        recipientsError = null
    }

    /**
     * Updates the model when recipient names can not be loaded.
     *
     * @param message The error message to display.
     */
    fun showRecipientsError(message: String) {
        isLoadingRecipients = false
        recipientsError = message
    }

    /**
     * Clears the selected notification so that the list is displayed.
     */
    fun clearSelection() {
        selectedReceivedNotification = null
        isMarkingRead = false
        readError = null
        selectedNotification = null
        recipientNames = emptyList()
        isLoadingRecipients = false
        recipientsError = null
    }
}

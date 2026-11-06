package com.example.camply.notification.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the state of the shared notification screen (US 02.10.02).
 */
class NotificationsModel {

    var sentNotifications: List<SentNotification> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
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
}

package com.example.camply.reservations.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Represents one entry in the entrant's active reservations list.
 *
 * @property reservation The active reservation.
 * @property campsite The reserved campsite, or null if it no longer exists.
 */
data class ActiveReservationItem(
    val reservation: Reservation,
    val campsite: Campsite?
)

/**
 * Stores the state of the entrant's active reservations screen (US 01.06.01).
 */
class ActiveReservationsModel {

    var reservations: List<ActiveReservationItem> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model when active reservations are being loaded.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * Updates the model with the entrant's active reservations.
     *
     * @param list The active reservations to display.
     */
    fun showReservations(list: List<ActiveReservationItem>) {
        reservations = list
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when active reservations can not be loaded.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }
}

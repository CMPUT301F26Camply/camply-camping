package com.example.camply.reservations.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the state of the campsite reservation booking screen
 */
class ReservationBookingModel {
    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var datesValid: Boolean by mutableStateOf(false)
        private set

    var reservationConfirmed: Boolean by mutableStateOf(false)
        private set

    /**
     * Updates the model when reservation dates are being checked.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
        datesValid = false
    }

    /**
     * Updates the model when the selected dates pass validation.
     */
    fun showValidDates() {
        isLoading = false
        errorMessage = null
        datesValid = true
    }

    /**
     * Updates the model when reservation date validation fails.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
        datesValid = false
    }

    /**
     * Updates the booking screen after a reservation is successfully saved.
     */
    fun showConfirmedReservation() {
        isLoading = false
        errorMessage = null
        datesValid = false
        reservationConfirmed = true
    }

    /**
     * Resets the booking state when the entrant starts a new reservation.
     */
    fun resetBooking() {
        isLoading = false
        errorMessage = null
        datesValid = false
        reservationConfirmed = false
    }
}
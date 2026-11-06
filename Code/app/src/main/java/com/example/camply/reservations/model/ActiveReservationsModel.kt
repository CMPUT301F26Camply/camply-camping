package com.example.camply.reservations.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Represents one entry in the entrant's reservations list.
 *
 * @property reservation The reservation.
 * @property campsite The reserved campsite, or null if it no longer exists.
 */
data class ActiveReservationItem(
    val reservation: Reservation,
    val campsite: Campsite?
)

/**
 * Stores the state of the entrant's reservations screen (US 01.06.01, US 01.06.02).
 *
 * Supports filtering all reservation categories while preserving the original active
 * reservation functionality. Name of the class/file can be done at a later time to ensure
 * no conflicts right now...
 */
class ActiveReservationsModel {

    var reservations: List<ActiveReservationItem> by mutableStateOf(emptyList())
        private set

    // All reservations belonging to the entrant
    var allReservations: List<ActiveReservationItem> by mutableStateOf(emptyList())
        private set

    // Reservations matching the selected filters
    var filteredReservations: List<ActiveReservationItem> by mutableStateOf(emptyList())
        private set

    // Currently selected reservation categories
    var selectedFilters: Set<String> by mutableStateOf(emptySet())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var selectedReservation: ActiveReservationItem? by mutableStateOf(null)
        private set

    /**
     * Updates the model when reservations are being loaded.
     *
     * Clears the selection so that opening the screen again starts from the list.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
        selectedReservation = null
    }

    /**
     * Stores the reservation whose details should be displayed.
     *
     * @param item The selected reservation.
     */
    fun selectReservation(item: ActiveReservationItem) {
        selectedReservation = item
    }

    /**
     * Clears the selected reservation so that the list is displayed.
     */
    fun clearSelection() {
        selectedReservation = null
    }

    // Original active only reservations list
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
     * Updates all reservations while preserving the original active only list.
     *
     * @param all All of the entrant's reservations.
     * @param active The entrant's active reservations.
     */
    fun updateAllReservations(
        all: List<ActiveReservationItem>,
        active: List<ActiveReservationItem>
    ) {
        allReservations = all
        showReservations(active)
    }

    /**
     * Updates the reservations matching the selected filters.
     *
     * @param list Reservations matching the selected filters.
     */
    fun updateFilteredReservations(list: List<ActiveReservationItem>) {
        filteredReservations = list
    }

    /**
     * Updates the selected reservation filters.
     *
     * @param filters The currently selected categories.
     */
    fun updateSelectedFilters(filters: Set<String>) {
        selectedFilters = filters
    }


    /**
     * Updates the model when reservations cannot be loaded.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }
}

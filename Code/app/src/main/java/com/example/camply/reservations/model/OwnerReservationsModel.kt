package com.example.camply.reservations.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Stores a campsite together with its reservations.
 */
data class OwnerCampsiteReservations(
    val campsite: Campsite,
    val reservations: List<Reservation>
)

/**
 * Stores the state of the owner's reservation management screen.
 *
 * US 02.07.01.
 */
class OwnerReservationsModel {

    // Campsites owned by the user, along with their reservations
    var campsites: List<OwnerCampsiteReservations> by mutableStateOf(emptyList())
        private set

    // Currently selected reservation filters, with an empty set representing All
    var selectedFilters: Set<String> by mutableStateOf(emptySet())
        private set

    // ID of the campsite whose reservations are currently displayed
    var selectedCampsiteId: String? by mutableStateOf(null)
        private set

    // Reservation currently selected for viewing its details
    var selectedReservation: Reservation? by mutableStateOf(null)
        private set

    // Whether campsite and reservation data is currently loading
    var isLoading: Boolean by mutableStateOf(false)
        private set

    // Error message to display if loading fails
    var errorMessage: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model to indicate that reservations are loading.
     */
    fun updateLoadingState() {
        isLoading = true
        errorMessage = null
        selectedCampsiteId = null
        selectedReservation = null
    }

    /**
     * Updates the owner's campsite and reservation data after loading.
     *
     * @param list The owner's campsites and their reservations.
     */
    fun updateReservations(list: List<OwnerCampsiteReservations>) {
        campsites = list
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when loading reservations fails.
     *
     * @param message The error message to display.
     */
    fun updateErrorState(message: String) {
        isLoading = false
        errorMessage = message
    }

    /**
     * Updates the selected reservation filters.
     *
     * @param filters The selected reservation categories.
     */
    fun updateSelectedFilters(filters: Set<String>) {
        selectedFilters = filters
    }

    /**
     * Updates the currently selected campsite.
     *
     * @param campsiteId The campsite to display reservations for.
     */
    fun updateSelectedCampsite(campsiteId: String) {
        selectedCampsiteId = campsiteId
        selectedReservation = null
        selectedFilters = emptySet()
    }

    /**
     * Updates the currently selected reservation.
     *
     * @param reservation The reservation to display details for.
     */
    fun updateSelectedReservation(reservation: Reservation) {
        selectedReservation = reservation
    }

    /**
     * Clears the currently selected reservation.
     */
    fun clearSelectedReservation() {
        selectedReservation = null
    }

    /**
     * Clears the selected campsite and reservation.
     */
    fun clearSelectedCampsite() {
        selectedCampsiteId = null
        selectedReservation = null
    }

}

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
 * US 02.07.01, US 02.07.02, and US 02.08.01.
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

    /** Reservation awaiting the owner's confirmation; no save occurs until confirmed. */
    var pendingRejection: Reservation? by mutableStateOf(null)
        private set

    /** Whether a rejection is currently being saved. */
    var isRejecting: Boolean by mutableStateOf(false)
        private set

    /** Save error shown on the details screen without discarding its data. */
    var rejectionError: String? by mutableStateOf(null)
        private set

    /** Opens or dismisses rejection confirmation for [reservation]. */
    fun updatePendingRejection(reservation: Reservation?) {
        pendingRejection = reservation
        rejectionError = null
    }

    /** Begins saving a confirmed rejection and closes the confirmation dialog. */
    fun beginRejection() {
        pendingRejection = null
        rejectionError = null
        isRejecting = true
    }

    /** Replaces [reservation] in the overview and details only after a successful save. */
    fun updateSavedReservation(reservation: Reservation) {
        campsites = campsites.map { entry ->
            entry.copy(reservations = entry.reservations.map {
                if (it.id == reservation.id) reservation else it
            })
        }
        if (selectedReservation?.id == reservation.id) selectedReservation = reservation
    }

    /** Ends the save, retaining [message] for retry when it failed. */
    fun finishRejection(message: String? = null) {
        isRejecting = false
        rejectionError = message
    }

    /**
     * Updates the model to indicate that reservations are loading.
     */
    fun updateLoadingState() {
        isLoading = true
        errorMessage = null
        selectedCampsiteId = null
        selectedReservation = null
        pendingRejection = null
        rejectionError = null
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
     * Updates the selected campsite while preserving the overview's category filters.
     *
     * @param campsiteId The campsite to display reservations for.
     */
    fun updateSelectedCampsite(campsiteId: String) {
        selectedCampsiteId = campsiteId
        selectedReservation = null
        updatePendingRejection(null)
    }

    /**
     * Updates the currently selected reservation.
     *
     * @param reservation The reservation to display details for.
     */
    fun updateSelectedReservation(reservation: Reservation) {
        selectedReservation = reservation
        updatePendingRejection(null)
    }

    /**
     * Clears the currently selected reservation.
     */
    fun clearSelectedReservation() {
        selectedReservation = null
        updatePendingRejection(null)
    }

    /**
     * Clears the selected campsite and reservation.
     */
    fun clearSelectedCampsite() {
        selectedCampsiteId = null
        selectedReservation = null
        updatePendingRejection(null)
    }

}

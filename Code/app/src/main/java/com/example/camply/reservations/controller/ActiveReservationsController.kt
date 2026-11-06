package com.example.camply.reservations.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.reservations.model.ActiveReservationItem
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles the entrant's reservations screen (US 01.06.01, US 01.06.02).
 *
 * Loads reservations, filters them by status, and handles reservation selection.
 * Preserves the original active only reservation functionality.
 *
 * @param model The model storing the screen state.
 * @param reservationRepository The repository used to retrieve reservations.
 * @param campsiteRepository The repository used to retrieve reserved campsites.
 * @param today Provides today's date as an ISO string, like "2027-06-01".
 */
class ActiveReservationsController(
    private val model: ActiveReservationsModel,
    private val reservationRepository: ReservationRepository,
    private val campsiteRepository: CampsiteRepository,
    private val today: () -> String = { LocalDate.now().toString() }
) {

    /**
     * Loads an entrant's reservations, sorted by start date.
     * Preserves the active only list and stores all reservations for filtering.
     *
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun loadReservations(entrantId: String) {
        model.showLoading()

        try {
            val currentDate = today()

            // Retrieve all reservations with a valid status, sorted by start date.
            val allReservations = reservationRepository.getReservationsForEntrant(entrantId)
                .filter { it.status != ReservationStatus.UNINITIALIZED }
                .sortedBy { it.dates.from }

            // Several reservations can share a campsite, so each one is only read once.
            val campsites = allReservations
                .map { it.campsiteId }
                .distinct()
                .associateWith { campsiteRepository.getCampsite(it) }

            val allItems = allReservations.map {
                ActiveReservationItem(it, campsites[it.campsiteId])
            }

            // Preserve the original active only reservation items.
            val activeItems = allItems.filter {
                it.reservation.isActive(currentDate)
            }

            model.updateAllReservations(allItems, activeItems)
            applyFilters()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load reservations.")
        }
    }


    /**
     * Selects or deselects a reservation category.
     * Selecting All clears the other filters.
     *
     * @param category The selected reservation category.
     */
    fun onFilterSelected(category: String) {
        val filters = model.selectedFilters.toMutableSet()

        if (category == "All") {
            filters.clear()
        } else if (category in filters) {
            filters.remove(category)
        } else {
            filters.add(category)
        }

        model.updateSelectedFilters(filters)
        applyFilters()
    }


    /**
     * Returns the category to display for a reservation.
     *
     * @param reservation The reservation to classify.
     * @return The reservation category.
     */
    fun getReservationCategory(reservation: Reservation): String {
        val currentDate = today()

        return when {
            reservation.isActive(currentDate) -> "Active"
            reservation.isPast(currentDate) -> "Past"
            reservation.status == ReservationStatus.CANCELLED -> "Cancelled"
            reservation.status == ReservationStatus.REJECTED -> "Rejected"
            else -> "Uninitialized"
        }
    }

    /**
     * Updates the displayed reservations based on the selected categories.
     * An empty selection displays all reservations.
     */
    private fun applyFilters() {
        val filters = model.selectedFilters

        if (filters.isEmpty()) {
            model.updateFilteredReservations(model.allReservations)
            return
        }

        val filtered = model.allReservations.filter { item ->
            getReservationCategory(item.reservation) in filters
        }

        model.updateFilteredReservations(filtered)
    }

    /**
     * Opens the details of a selected reservation.
     *
     * @param item The reservation selected by the entrant.
     */
    fun onReservationSelected(item: ActiveReservationItem) = model.selectReservation(item)

    /**
     * Returns from a reservation's details to the list.
     */
    fun onBackToList() = model.clearSelection()
}

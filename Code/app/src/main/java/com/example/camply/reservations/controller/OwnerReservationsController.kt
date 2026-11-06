package com.example.camply.reservations.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.reservations.model.OwnerCampsiteReservations
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles owner reservation loading, filtering, and navigation.
 *
 * US 02.07.01.
 */
class OwnerReservationsController(
    private val model: OwnerReservationsModel,
    private val campsiteRepository: CampsiteRepository,
    private val reservationRepository: ReservationRepository,
    private val today: () -> String = { LocalDate.now().toString() }
) {

    /**
     * Loads all campsites belonging to the owner and their reservations.
     *
     * @param ownerId The current owner's user ID.
     */
    suspend fun loadAllReservations(ownerId: String) {
        model.updateLoadingState()

        try {
            val campsites = campsiteRepository.getCampsitesByOwner(ownerId)
                .sortedBy { it.name }

            val results = campsites.map { campsite ->
                val reservations = reservationRepository
                    .getReservationsForCampsite(campsite.id)
                    .filter {
                        it.campsiteId == campsite.id && it.status != ReservationStatus.UNINITIALIZED
                    }
                    .sortedBy { it.dates.from }

                OwnerCampsiteReservations(campsite, reservations)
            }

            model.updateReservations(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.updateErrorState(
                e.message ?: "Could not load owner reservations."
            )
        }
    }

    /**
     * Returns the display category for a reservation.
     *
     * @param reservation The reservation to classify.
     * @return Active, Past, Cancelled, or Rejected.
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
     * Selects or deselects a reservation filter.
     * Selecting All clears the other filters.
     *
     * @param category The filter selected by the owner.
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
    }

    /**
     * Returns reservations matching the selected filters.
     *
     * @param reservations The reservations to filter.
     * @return The matching reservations.
     */
    fun getFilteredReservations(
        reservations: List<Reservation>
    ): List<Reservation> {
        val filters = model.selectedFilters

        if (filters.isEmpty()) {
            return reservations
        }

        return reservations.filter {
            getReservationCategory(it) in filters
        }
    }

    /**
     * Returns the owner's campsites that have at least one reservation.
     *
     * @return Campsites with reservations.
     */
    fun getCampsitesWithReservations(): List<OwnerCampsiteReservations> {
        return model.campsites.filter {
            it.reservations.isNotEmpty()
        }
    }

    /**
     * Counts active reservations for a campsite.
     *
     * @param entry The campsite and its reservations.
     * @return The number of active reservations.
     */
    fun getActiveCount(entry: OwnerCampsiteReservations): Int {
        val currentDate = today()

        return entry.reservations.count {
            it.isActive(currentDate)
        }
    }

    /**
     * Opens the reservations for an owned campsite.
     *
     * @param campsiteId The selected campsite ID.
     */
    fun onCampsiteSelected(campsiteId: String) {
        if (model.campsites.any { it.campsite.id == campsiteId }) {
            model.updateSelectedCampsite(campsiteId)
        }
    }

    /**
     * Opens the details for a reservation in the selected campsite.
     *
     * @param reservation The selected reservation.
     */
    fun onReservationSelected(reservation: Reservation) {
        val entry = model.campsites.firstOrNull {
            it.campsite.id == model.selectedCampsiteId
        }

        if (entry?.reservations?.any { it.id == reservation.id } == true) {
            model.updateSelectedReservation(reservation)
        }
    }

    /**
     * Returns to the previous reservation screen.
     */
    fun onBack() {
        if (model.selectedReservation != null) {
            model.clearSelectedReservation()
        } else {
            model.clearSelectedCampsite()
        }
    }
}

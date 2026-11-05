package com.example.camply.reservations.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.reservations.model.ActiveReservationItem
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.repository.ReservationRepository
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles loading the entrant's active reservations (US 01.06.01).
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
     * Loads an entrant's active reservations, the first is the soonest and so on.
     *
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun loadReservations(entrantId: String) {
        model.showLoading()

        try {
            val currentDate = today()
            val activeReservations = reservationRepository.getReservationsForEntrant(entrantId)
                .filter { it.isActive(currentDate) }
                .sortedBy { it.dates.from }

            // Several reservations can share a campsite, so each one is only read once.
            val campsites = activeReservations
                .map { it.campsiteId }
                .distinct()
                .associateWith { campsiteRepository.getCampsite(it) }

            model.showReservations(
                activeReservations.map { ActiveReservationItem(it, campsites[it.campsiteId]) }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load reservations.")
        }
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

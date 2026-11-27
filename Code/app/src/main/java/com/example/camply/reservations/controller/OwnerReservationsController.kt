package com.example.camply.reservations.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.notification.repository.RecipientNameRepository
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
 * US 02.07.01, US 02.07.02, and US 02.08.01.
 */
class OwnerReservationsController(
    private val model: OwnerReservationsModel,
    private val campsiteRepository: CampsiteRepository,
    private val reservationRepository: ReservationRepository,
    private val entrantNameRepository: RecipientNameRepository,
    private val today: () -> String = { LocalDate.now().toString() }
) {
    private var loadedOwnerId: String? = null

    /**
     * Loads all campsites belonging to the owner and their reservations.
     *
     * @param ownerId The current owner's user ID.
     */
    suspend fun loadAllReservations(ownerId: String) {
        if (model.isRejecting) return
        loadedOwnerId = null
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

            val entrantNames = results.asSequence()
                .flatMap { it.reservations.asSequence() }
                .map { it.entrantId }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
                .mapNotNull { id -> findEntrantName(id)?.let { id to it } }
                .toMap()
            model.updateReservations(results, entrantNames)
            loadedOwnerId = ownerId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.updateErrorState(
                e.message ?: "Could not load owner reservations."
            )
        }
    }

    private suspend fun findEntrantName(entrantId: String): String? = try {
        entrantNameRepository.getRecipientName(entrantId)?.trim()?.takeIf { it.isNotEmpty() }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
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
     * Returns the owner's campsites that have reservations matching the selected filters.
     *
     * @return Campsites with at least one matching reservation.
     */
    fun getCampsitesWithReservations(): List<OwnerCampsiteReservations> {
        return model.campsites.filter {
            getFilteredReservations(it.reservations).isNotEmpty()
        }
    }

    /**
     * Counts reservations matching the selected categories for a campsite.
     *
     * @param entry The campsite and its reservations.
     * @return The number of matching reservations, or the total when All is selected.
     */
    fun getFilteredCount(entry: OwnerCampsiteReservations): Int =
        getFilteredReservations(entry.reservations).size

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
        if (model.isRejecting) return
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
        if (model.isRejecting) return
        val entry = model.campsites.firstOrNull {
            it.campsite.id == model.selectedCampsiteId
        }

        entry?.reservations?.firstOrNull { it.id == reservation.id }?.let(model::updateSelectedReservation)
    }

    /** Returns whether the selected reservation is active and belongs to the loaded owner. */
    fun canRejectSelectedReservation(): Boolean {
        val reservation = model.selectedReservation ?: return false
        return !model.isLoading && !model.isRejecting && reservation.id.isNotBlank() &&
            reservation.isActive(today()) && model.campsites.any { entry ->
                entry.campsite.id == reservation.campsiteId && entry.campsite.ownerId == loadedOwnerId &&
                    entry.reservations.any { it.id == reservation.id }
            }
    }

    /** Requests confirmation before rejecting the selected active reservation. */
    fun requestRejection() {
        if (canRejectSelectedReservation()) model.updatePendingRejection(model.selectedReservation)
    }

    /** Dismisses confirmation, leaving the saved reservation unchanged. */
    fun dismissRejection() = model.updatePendingRejection(null)

    /** Saves a confirmed rejection, preserving details and an inline error if saving fails. */
    suspend fun confirmRejection() {
        val pending = model.pendingRejection ?: return
        val ownerId = loadedOwnerId ?: return
        if (pending.id != model.selectedReservation?.id || !canRejectSelectedReservation()) {
            model.updatePendingRejection(null)
            return
        }
        model.beginRejection()
        try {
            val saved = reservationRepository.rejectReservation(pending.id, ownerId, today())
            model.updateSavedReservation(saved)
            model.finishRejection()
        } catch (e: CancellationException) {
            model.finishRejection()
            throw e
        } catch (e: Exception) {
            model.finishRejection(e.message ?: "Could not reject this reservation. Please try again.")
        }
    }

    /**
     * Returns to the previous reservation screen.
     */
    fun onBack() {
        if (model.isRejecting) return
        if (model.selectedReservation != null) {
            model.clearSelectedReservation()
        } else {
            model.clearSelectedCampsite()
        }
    }
}

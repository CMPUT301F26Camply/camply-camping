package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles reservation date validation for campsite bookings.
 *
 * @param bookingService The service used to check reservation dates.
 * @param repository The repository used to retrieve existing reservations.
 * @param model The model used to store booking screen state.
 */
class ReservationBookingController(
    private val bookingService: BookingService,
    private val repository: ReservationRepository,
    private val model: ReservationBookingModel = ReservationBookingModel()
) {

    /**
     * Checks whether the selected dates can be used for a campsite reservation.
     *
     * @param campsite The campsite being reserved.
     * @param selectedDates The entrant's selected dates.
     * @return An error message if the dates are unavailable, or null if no conflict is found.
     */
    suspend fun validateDates(campsite: Campsite, selectedDates: DateRange): String? {
        if (!bookingService.areDatesValid(selectedDates)) {
            return "Enter valid dates using the yyyy-MM-dd format."
        }

        if (!bookingService.areDatesAvailable(campsite, selectedDates)) {
            return "Selected dates are outside the campsite's availability."
        }

        val reservations = repository.getReservationsForCampsite(campsite.id)

        if (bookingService.hasOverlappingReservation(selectedDates, reservations)) {
            return "Selected dates overlap an existing reservation."
        }

        return null
    }

    /**
     * Checks whether an entrant has already used their lottery win.
     *
     * @param reservation The reservation the entrant wants to make.
     * @return An error message if the lottery win has already been used, or null otherwise.
     */
    suspend fun validateLotteryReservation(reservation: Reservation): String? {
        val reservations = repository.getReservationsForEntrant(reservation.entrantId)

        if (bookingService.hasExistingLotteryReservation(reservation.entrantId, reservation.lotteryId, reservations)) {
            return "You have already confirmed a reservation for this lottery."
        }

        return null
    }

    /**
     * Validates selected dates and updates the booking screen state.
     *
     * @param campsite The campsite being reserved.
     * @param selectedDates The entrant's selected dates.
     */
    suspend fun checkDates(campsite: Campsite, selectedDates: DateRange) {
        model.showLoading()

        try {
            val error = validateDates(campsite, selectedDates)

            if (error == null) {
                model.showValidDates()
            } else {
                model.showError(error)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not check reservation dates.")
        }
    }

    /**
     * Confirms a campsite reservation after verifying booking eligibility.
     *
     * @param campsite The campsite being reserved.
     * @param reservation The reservation to confirm.
     * @return The new reservation ID, or null if confirmation fails.
     */
    suspend fun confirmReservation(
        campsite: Campsite,
        reservation: Reservation
    ): String? {
        model.showLoading()

        // Prevent confirmation until lottery winner verification is implemented.
        model.showError("Lottery winner verification is not connected yet.")
        return null
    }
}
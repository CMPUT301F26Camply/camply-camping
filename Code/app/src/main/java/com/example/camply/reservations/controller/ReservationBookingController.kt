package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.lottery.repository.LotteryRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles reservation date validation for campsite bookings.
 *
 * @param bookingService The service used to check reservation dates.
 * @param repository The repository used to retrieve existing reservations.
 * @param notificationService The service used to send reservation confirmations.
 * @param model The model used to store booking screen state.
 * @param lotteryRepository The repository used to retrieve lottery information.
 * @param lotteryEntryRepository The repository used to verify the entrant's lottery result.
 */
class ReservationBookingController(
    private val bookingService: BookingService,
    private val repository: ReservationRepository,
    private val notificationService: NotificationService,
    private val model: ReservationBookingModel = ReservationBookingModel(),
    private val lotteryRepository: LotteryRepository,
    private val lotteryEntryRepository: LotteryEntryRepository
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
     * Confirms a reservation after checking the entrant's lottery win,
     * booking period, campsite availability, and rules acknowledgement.
     * US 01.04.01.
     *
     * @param campsite The campsite being reserved.
     * @param reservation The reservation containing the entrant, lottery, and selected dates.
     * @return The saved reservation ID, or null if confirmation fails.
     */
    suspend fun confirmReservation(
        campsite: Campsite,
        reservation: Reservation
    ): String? {
        if (!reservation.rulesAcknowledged) {
            model.showError("You must confirm that you have read the campsite rules and policies.")
            return null
        }

        model.showLoading()

        try {
            if (reservation.entrantId.isBlank() || reservation.lotteryId.isBlank()) {
                model.showError("Missing entrant or lottery information.")
                return null
            }

            if (reservation.campsiteId != campsite.id) {
                model.showError("The reservation does not match this campsite.")
                return null
            }

            val lottery = lotteryRepository.getLottery(reservation.lotteryId)

            if (lottery == null) {
                model.showError("Lottery not found.")
                return null
            }

            if (campsite.id !in lottery.campsiteIds) {
                model.showError("This campsite is not part of the lottery.")
                return null
            }

            val entry = lotteryEntryRepository.getEntry(
                reservation.lotteryId,
                reservation.entrantId
            )

            if (entry?.canConfirmReservation() != true) {
                model.showError("You do not have an available winning opportunity.")
                return null
            }

            val today = LocalDate.now(ZoneId.of("America/Edmonton")).toString()

            if (today < lottery.bookingStart || today > lottery.bookingEnd) {
                model.showError("The lottery booking period is not open.")
                return null
            }

            if (!bookingService.areDatesValid(reservation.dates)) {
                model.showError("Enter valid reservation dates.")
                return null
            }

            if (reservation.dates.from < lottery.lotteryStart ||
                reservation.dates.to > lottery.lotteryEnd
            ) {
                model.showError("Selected dates are outside the lottery stay period.")
                return null
            }

            val dateError = validateDates(campsite, reservation.dates)

            if (dateError != null) {
                model.showError(dateError)
                return null
            }

            val lotteryError = validateLotteryReservation(reservation)

            if (lotteryError != null) {
                model.showError(lotteryError)
                return null
            }

            val confirmedReservation = reservation.copy(
                status = ReservationStatus.CONFIRMED
            )

            val reservationId = repository.createReservation(confirmedReservation)

            model.showConfirmedReservation()

            notifyReservationConfirmed(campsite, confirmedReservation, reservationId)

            return reservationId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not confirm reservation.")
            return null
        }
    }

    /**
     * Sends the entrant a confirmation after their reservation has been saved (US 01.05.01).
     *
     * A failed send does not affect the booking, because the reservation already exists.
     *
     * @param campsite The reserved campsite.
     * @param reservation The saved reservation.
     * @param reservationId The ID returned when the reservation was saved.
     */
    suspend fun notifyReservationConfirmed(
        campsite: Campsite,
        reservation: Reservation,
        reservationId: String
    ) {
        try {
            notificationService.sendReservationConfirmation(
                campsite.ownerId,
                reservation.entrantId,
                reservationId,
                campsite.name,
                reservation.dates
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The reservation is already saved, so a failed notification is not a booking error.
        }
    }

    /**
     * Resets the booking screen state when an entrant starts a new reservation.
     */
    fun resetBooking() {
        model.resetBooking()
    }
}

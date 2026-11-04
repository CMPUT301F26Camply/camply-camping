package com.example.camply.reservations.service

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Handles campsite reservation date selection and availability checks.
 */
class BookingService {
    /**
     * Retrieves the availability date ranges for a campsite
     *
     * @param campsite The campsite being booked.
     * @return The campsite's advertised available date ranges
     */
    fun getAvailableDates(campsite: Campsite): List<DateRange> = campsite.availability

    /**
     * Checks whether a selected date range fits within the campsite's availability
     *
     * @param campsite The campsite being booked
     * @param selectedDates The entrant's selected date range
     * @return True if the selected range is within an available range, false otherwise
     */
    fun areDatesAvailable(campsite: Campsite, selectedDates: DateRange): Boolean {
        if (selectedDates.from.isBlank() || selectedDates.to.isBlank()) return false
        if (selectedDates.from > selectedDates.to) return false

        for (availableDates in campsite.availability) {
            if (availableDates.covers(selectedDates.from, selectedDates.to)) {
                return true
            }
        }

        return false
    }

    /**
     * Checks whether selected dates overlap an existing confirmed reservation.
     *
     * @param selectedDates The entrant's selected date range.
     * @param reservations Existing reservations for the campsite.
     * @return True if the selected dates overlap a confirmed reservation.
     */
    fun hasOverlappingReservation(
        selectedDates: DateRange,
        reservations: List<Reservation>
    ): Boolean {
        for (reservation in reservations) {
            if (reservation.status == ReservationStatus.CONFIRMED) {
                val bookedDates = reservation.dates

                if (selectedDates.from <= bookedDates.to && selectedDates.to >= bookedDates.from) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks whether an entrant has already booked using a lottery win.
     *
     * @param entrantId The entrant's Firebase user ID.
     * @param lotteryId The lottery associated with the reservation.
     * @param reservations The entrant's existing reservations.
     * @return True if the entrant already has a confirmed reservation for the lottery.
     */
    fun hasExistingLotteryReservation(
        entrantId: String,
        lotteryId: String,
        reservations: List<Reservation>
    ): Boolean {
        for (reservation in reservations) {
            if (reservation.entrantId == entrantId &&
                reservation.lotteryId == lotteryId &&
                reservation.status == ReservationStatus.CONFIRMED) {
                return true
            }
        }

        return false
    }

    /**
     * Checks whether the selected dates are valid ISO calendar dates.
     *
     * @param dates The dates selected by the entrant.
     * @return True if both dates are valid and in chronological order.
     */

    fun areDatesValid(dates: DateRange): Boolean {
        return try {
            val start = LocalDate.parse(dates.from)
            val end = LocalDate.parse(dates.to)

            dates.from == start.toString() && dates.to == end.toString() && !start.isAfter(end)
        } catch (e: DateTimeParseException) {
            false
        }
    }

}
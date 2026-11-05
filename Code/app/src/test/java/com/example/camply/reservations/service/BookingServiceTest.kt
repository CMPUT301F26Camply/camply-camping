package com.example.camply.reservations.service

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for campsite availability checks in BookingService (US 01.04.01).
 */
class BookingServiceTest {

    private lateinit var bookingService: BookingService
    private lateinit var campsite: Campsite

    /**
     * Creates a booking service and campsite before each test.
     */
    @Before
    fun setUp() {
        bookingService = BookingService()
        campsite = Campsite(
            id = "1",
            name = "Banff Pines",
            availability = listOf(
                DateRange("2027-06-01", "2027-06-10"),
                DateRange("2027-07-01", "2027-07-15")
            )
        )
    }

    /**
     * Checks that the campsite's advertised availability is returned.
     */
    @Test
    fun getAvailableDates_returnsCampsiteAvailability() {
        val availableDates = bookingService.getAvailableDates(campsite)

        assertEquals(campsite.availability, availableDates)
    }

    /**
     * Checks that dates inside an available range are accepted.
     */
    @Test
    fun areDatesAvailable_datesWithinRange_returnsTrue() {
        val selectedDates = DateRange("2027-06-03", "2027-06-05")

        assertTrue(bookingService.areDatesAvailable(campsite, selectedDates))
    }

    /**
     * Checks that dates outside the available ranges are rejected.
     */
    @Test
    fun areDatesAvailable_datesOutsideRange_returnsFalse() {
        val selectedDates = DateRange("2027-08-01", "2027-08-05")

        assertFalse(bookingService.areDatesAvailable(campsite, selectedDates))
    }

    /**
     * Checks that dates extending beyond an available range are rejected.
     */
    @Test
    fun areDatesAvailable_datesExtendBeyondRange_returnsFalse() {
        val selectedDates = DateRange("2027-06-08", "2027-06-12")

        assertFalse(bookingService.areDatesAvailable(campsite, selectedDates))
    }

    /**
     * Checks that a reversed date range is rejected.
     */
    @Test
    fun areDatesAvailable_reversedDates_returnsFalse() {
        val selectedDates = DateRange("2027-06-08", "2027-06-03")

        assertFalse(bookingService.areDatesAvailable(campsite, selectedDates))
    }

    /**
     * Checks that a date range containing a blank date is rejected.
     */
    @Test
    fun areDatesAvailable_blankDates_returnsFalse() {
        val selectedDates = DateRange("", "2027-06-05")

        assertFalse(bookingService.areDatesAvailable(campsite, selectedDates))
    }

    /**
     * Checks that a campsite without availability rejects the selected dates.
     */
    @Test
    fun areDatesAvailable_noAvailability_returnsFalse() {
        val emptyCampsite = Campsite(id = "2", name = "Empty Campsite")
        val selectedDates = DateRange("2027-06-03", "2027-06-05")

        assertFalse(bookingService.areDatesAvailable(emptyCampsite, selectedDates))
    }


    /**
     * Checks that overlapping confirmed reservations are detected.
     */
    @Test
    fun hasOverlappingReservation_overlappingDates_returnsTrue() {
        val selectedDates = DateRange("2027-06-05", "2027-06-08")
        val reservations = listOf(
            Reservation(
                dates = DateRange("2027-06-03", "2027-06-06"),
                status = ReservationStatus.CONFIRMED
            )
        )

        assertTrue(bookingService.hasOverlappingReservation(selectedDates, reservations))
    }

    /**
     * Checks that confirmed reservations without overlapping dates are accepted.
     */
    @Test
    fun hasOverlappingReservation_nonOverlappingDates_returnsFalse() {
        val selectedDates = DateRange("2027-06-07", "2027-06-10")
        val reservations = listOf(
            Reservation(
                dates = DateRange("2027-06-03", "2027-06-06"),
                status = ReservationStatus.CONFIRMED
            )
        )

        assertFalse(bookingService.hasOverlappingReservation(selectedDates, reservations))
    }

    /**
     * Checks that cancelled reservations do not prevent other reservations.
     */
    @Test
    fun hasOverlappingReservation_cancelledReservation_returnsFalse() {
        val selectedDates = DateRange("2027-06-05", "2027-06-08")
        val reservations = listOf(
            Reservation(
                dates = DateRange("2027-06-03", "2027-06-06"),
                status = ReservationStatus.CANCELLED
            )
        )

        assertFalse(bookingService.hasOverlappingReservation(selectedDates, reservations))
    }

    /**
     * Checks that no conflicts are detected when no reservations exist.
     */
    @Test
    fun hasOverlappingReservation_noReservations_returnsFalse() {
        val selectedDates = DateRange("2027-06-05", "2027-06-08")

        assertFalse(bookingService.hasOverlappingReservation(selectedDates, emptyList()))
    }

    /**
     * Checks that valid ISO dates are accepted.
     */
    @Test
    fun areDatesValid_validDates_returnsTrue() {
        val dates = DateRange("2027-06-03", "2027-06-08")

        assertTrue(bookingService.areDatesValid(dates))
    }

    /**
     * Checks that dates outside the expected ISO format are rejected.
     */
    @Test
    fun areDatesValid_incorrectFormat_returnsFalse() {
        val dates = DateRange("06/03/2027", "06/08/2027")

        assertFalse(bookingService.areDatesValid(dates))
    }

    /**
     * Checks that impossible calendar dates are rejected.
     */
    @Test
    fun areDatesValid_invalidCalendarDate_returnsFalse() {
        val dates = DateRange("2027-06-31", "2027-07-05")

        assertFalse(bookingService.areDatesValid(dates))
    }

    /**
     * Checks that an end date before the start date is rejected.
     */
    @Test
    fun areDatesValid_reversedDates_returnsFalse() {
        val dates = DateRange("2027-06-08", "2027-06-03")

        assertFalse(bookingService.areDatesValid(dates))
    }

    /**
     * Checks that a confirmed reservation prevents reusing the same lottery win.
     */
    @Test
    fun hasExistingLotteryReservation_confirmedReservation_returnsTrue() {
        val reservations = listOf(
            Reservation(entrantId = "entrant1", lotteryId = "lottery1", status = ReservationStatus.CONFIRMED)
        )

        assertTrue(bookingService.hasExistingLotteryReservation("entrant1", "lottery1", reservations))
    }

    /**
     * Checks that reservations from other lotteries do not count as using the win.
     */
    @Test
    fun hasExistingLotteryReservation_differentLottery_returnsFalse() {
        val reservations = listOf(
            Reservation(entrantId = "entrant1", lotteryId = "lottery2", status = ReservationStatus.CONFIRMED)
        )

        assertFalse(bookingService.hasExistingLotteryReservation("entrant1", "lottery1", reservations))
    }

    /**
     * Checks that cancelled reservations do not count as active lottery bookings.
     */
    @Test
    fun hasExistingLotteryReservation_cancelledReservation_returnsFalse() {
        val reservations = listOf(
            Reservation(entrantId = "entrant1", lotteryId = "lottery1", status = ReservationStatus.CANCELLED)
        )

        assertFalse(bookingService.hasExistingLotteryReservation("entrant1", "lottery1", reservations))
    }



}
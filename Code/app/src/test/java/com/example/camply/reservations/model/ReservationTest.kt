package com.example.camply.reservations.model

import com.example.camply.campsite.model.DateRange
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Unit tests for deciding whether a reservation is active (US 01.06.01).
 */
class ReservationTest {

    private val today = "2027-06-10"

    /**
     * Checks that a confirmed reservation with a future stay is active.
     */
    @Test
    fun isActive_confirmedFutureStay_returnsTrue() {
        val reservation = reservation(ReservationStatus.CONFIRMED, to = "2027-06-15")

        assertTrue(reservation.isActive(today))
    }

    /**
     * Checks that a confirmed reservation is still active on its last day.
     */
    @Test
    fun isActive_confirmedStayEndsToday_returnsTrue() {
        val reservation = reservation(ReservationStatus.CONFIRMED, to = today)

        assertTrue(reservation.isActive(today))
    }

    /**
     * Checks that a confirmed reservation is not active after its stay has ended.
     */
    @Test
    fun isActive_confirmedStayEnded_returnsFalse() {
        val reservation = reservation(ReservationStatus.CONFIRMED, to = "2027-06-09")

        assertFalse(reservation.isActive(today))
    }

    /**
     * Checks that a cancelled reservation is not active.
     */
    @Test
    fun isActive_cancelled_returnsFalse() {
        val reservation = reservation(ReservationStatus.CANCELLED, to = "2027-06-15")

        assertFalse(reservation.isActive(today))
    }

    /**
     * Checks that a rejected reservation is not active.
     */
    @Test
    fun isActive_rejected_returnsFalse() {
        val reservation = reservation(ReservationStatus.REJECTED, to = "2027-06-15")

        assertFalse(reservation.isActive(today))
    }

    /**
     * Checks that an uninitialized reservation is not active.
     */
    @Test
    fun isActive_uninitialized_returnsFalse() {
        val reservation = reservation(ReservationStatus.UNINITIALIZED, to = "2027-06-15")

        assertFalse(reservation.isActive(today))
    }

    private fun reservation(status: ReservationStatus, to: String) =
        Reservation(status = status, dates = DateRange("2027-06-01", to))
}

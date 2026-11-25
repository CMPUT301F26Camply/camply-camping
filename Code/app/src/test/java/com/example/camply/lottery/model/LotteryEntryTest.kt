package com.example.camply.lottery.model

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Unit tests for the rules of a lottery winner's reservation opportunity (US 01.04.03).
 */
class LotteryEntryTest {

    /**
     * Checks that a winner with an unused opportunity can decline it.
     */
    @Test
    fun canDecline_winnerWithUnusedOpportunity_returnsTrue() {
        val entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        assertTrue(entry.canDecline())
    }

    /**
     * Checks that an entrant who did not win cannot decline.
     */
    @Test
    fun canDecline_notWinner_returnsFalse() {
        val entry = LotteryEntry(status = "waiting")

        assertFalse(entry.canDecline())
    }

    /**
     * Checks that a declined opportunity cannot be used to confirm a reservation.
     */
    @Test
    fun canConfirmReservation_declined_returnsFalse() {
        val entry = LotteryEntry(
            status = LotteryEntry.STATUS_WON,
            opportunityStatus = LotteryEntry.OPPORTUNITY_DECLINED
        )

        assertFalse(entry.canConfirmReservation())
    }

    /**
     * A winner who already declined their opportunity cannot decline again.
     */
    @Test
    fun canDecline_alreadyDeclined_returnsFalse() {
        val entry = LotteryEntry(
            status = LotteryEntry.STATUS_WON,
            opportunityStatus = LotteryEntry.OPPORTUNITY_DECLINED
        )

        assertFalse(entry.canDecline())
    }

    /**
     * A winner with an unused opportunity can proceed to booking.
     */
    @Test
    fun canConfirmReservation_unusedWin_returnsTrue() {
        val entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        assertTrue(entry.canConfirmReservation())
    }

    /**
     * An entrant who has not won cannot confirm a reservation.
     */
    @Test
    fun canConfirmReservation_notWinner_returnsFalse() {
        val entry = LotteryEntry(status = "waiting")

        assertFalse(entry.canConfirmReservation())
    }

    /**
     * A booked lottery opportunity cannot be used to book again or declined.
     */
    @Test
    fun bookedOpportunity_cannotBookOrDecline() {
        val entry = LotteryEntry(
            status = LotteryEntry.STATUS_WON,
            opportunityStatus = LotteryEntry.OPPORTUNITY_BOOKED
        )

        assertFalse(entry.canConfirmReservation())
        assertFalse(entry.canDecline())
    }
}

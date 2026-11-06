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
}

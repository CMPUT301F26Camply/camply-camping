package com.example.camply.lottery.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryRegistrationTest {
    private val lottery = Lottery(
        registrationStart = "2026-11-04",
        registrationEnd = "2026-11-06",
        status = LotteryStatus.OPEN
    )

    @Test
    fun startAndEndDatesAreIncluded() {
        assertTrue(lottery.isRegistrationOpen("2026-11-04"))
        assertTrue(lottery.isRegistrationOpen("2026-11-06"))
        assertFalse(lottery.isRegistrationOpen("2026-11-03"))
        assertFalse(lottery.isRegistrationOpen("2026-11-07"))
    }

    @Test
    fun oneDayRegistrationIsAllowed() {
        assertTrue(lottery.copy(registrationEnd = "2026-11-04")
            .isRegistrationOpen("2026-11-04"))
    }

    @Test
    fun drawnBookingAndClosedLotteriesRejectEntries() {
        for (status in listOf(LotteryStatus.DRAWN, LotteryStatus.BOOKING, LotteryStatus.CLOSED, LotteryStatus.UNINITIALIZED)) {
            assertFalse(lottery.copy(status = status).isRegistrationOpen("2026-11-05"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedDatesAreRejected() {
        lottery.copy(registrationStart = "2026-11-07").isRegistrationOpen("2026-11-05")
    }

    @Test(expected = java.text.ParseException::class)
    fun invalidDateIsRejected() {
        lottery.copy(registrationStart = "2026-02-30").isRegistrationOpen("2026-11-05")
    }
}

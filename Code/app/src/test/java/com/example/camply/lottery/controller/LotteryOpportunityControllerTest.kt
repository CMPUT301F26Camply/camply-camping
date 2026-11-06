package com.example.camply.lottery.controller

import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.lottery.repository.DeclineResult
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for loading and declining a reservation opportunity (US 01.04.03).
 */
class LotteryOpportunityControllerTest {

    private lateinit var model: LotteryOpportunityModel
    private lateinit var entryRepository: TestLotteryEntryRepository
    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var controller: LotteryOpportunityController

    /**
     * Creates the model, repositories, and controller before each test.
     */
    @Before
    fun setUp() {
        model = LotteryOpportunityModel()
        entryRepository = TestLotteryEntryRepository()
        reservationRepository = TestReservationRepository()
        controller = LotteryOpportunityController(
            model,
            entryRepository,
            reservationRepository,
            BookingService()
        )
    }

    /**
     * Checks that an existing entry is loaded into the model.
     */
    @Test
    fun loadEntry_existingEntry_showsEntry() = runBlocking {
        entryRepository.entry = winningEntry()

        controller.loadEntry(LOTTERY_ID, ENTRANT_ID)

        assertEquals(winningEntry(), model.entry)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that declining an unused win is saved and keeps the draw result.
     */
    @Test
    fun declineOpportunity_unusedWin_savesAndShowsDeclined() = runBlocking {
        entryRepository.entry = winningEntry()
        controller.loadEntry(LOTTERY_ID, ENTRANT_ID)

        controller.declineOpportunity(LOTTERY_ID, ENTRANT_ID)

        assertEquals(1, entryRepository.declineCount)
        assertEquals(LotteryEntry.OPPORTUNITY_DECLINED, model.entry?.opportunityStatus)
        assertEquals(LotteryEntry.STATUS_WON, model.entry?.status)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that an opportunity used for a confirmed reservation cannot be declined.
     */
    @Test
    fun declineOpportunity_confirmedReservation_doesNotDecline() = runBlocking {
        entryRepository.entry = winningEntry()
        reservationRepository.reservations = listOf(
            Reservation(
                entrantId = ENTRANT_ID,
                lotteryId = LOTTERY_ID,
                status = ReservationStatus.CONFIRMED
            )
        )
        controller.loadEntry(LOTTERY_ID, ENTRANT_ID)

        controller.declineOpportunity(LOTTERY_ID, ENTRANT_ID)

        assertEquals(0, entryRepository.declineCount)
        assertNull(model.entry?.opportunityStatus)
        assertEquals("You already have a reservation for this lottery.", model.errorMessage)
    }

    /**
     * Checks that a rejected decline shows a message and reloads the entry.
     */
    @Test
    fun declineOpportunity_notEligible_showsErrorAndReloads() = runBlocking {
        entryRepository.entry = winningEntry()
        controller.loadEntry(LOTTERY_ID, ENTRANT_ID)
        val declinedElsewhere = winningEntry().copy(
            opportunityStatus = LotteryEntry.OPPORTUNITY_DECLINED
        )
        entryRepository.entry = declinedElsewhere
        entryRepository.declineResult = DeclineResult.NOT_ELIGIBLE

        controller.declineOpportunity(LOTTERY_ID, ENTRANT_ID)

        assertEquals(declinedElsewhere, model.entry)
        assertEquals("This opportunity can no longer be declined.", model.errorMessage)
        assertFalse(model.isLoading)
    }

    /**
     * Checks that a failed decline shows an error and is not shown as declined.
     */
    @Test
    fun declineOpportunity_repositoryFails_showsError() = runBlocking {
        entryRepository.entry = winningEntry()
        controller.loadEntry(LOTTERY_ID, ENTRANT_ID)
        entryRepository.shouldFailDecline = true

        controller.declineOpportunity(LOTTERY_ID, ENTRANT_ID)

        assertEquals("Could not save.", model.errorMessage)
        assertNull(model.entry?.opportunityStatus)
        assertFalse(model.isLoading)
    }

    private fun winningEntry() = LotteryEntry(
        entrantId = ENTRANT_ID,
        lotteryId = LOTTERY_ID,
        status = LotteryEntry.STATUS_WON
    )

    /**
     * Provides lottery entries for controller tests without accessing Firestore.
     */
    private class TestLotteryEntryRepository : LotteryEntryRepository {
        var entry: LotteryEntry? = null
        var declineResult = DeclineResult.DECLINED
        var shouldFailDecline = false
        var declineCount = 0

        override suspend fun getEntry(lotteryId: String, entrantId: String): LotteryEntry? = entry

        override suspend fun declineOpportunity(lotteryId: String, entrantId: String): DeclineResult {
            if (shouldFailDecline) throw Exception("Could not save.")
            declineCount++
            return declineResult
        }
    }

    /**
     * Provides reservations for controller tests without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {
        var reservations: List<Reservation> = emptyList()

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
            reservations.filter { it.entrantId == entrantId }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"
    }

    private companion object {
        const val LOTTERY_ID = "lottery1"
        const val ENTRANT_ID = "entrant1"
    }
}

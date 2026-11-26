package com.example.camply.lottery.model

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for the state of the owner's waitlist screen (US 02.09.01).
 */
class OwnerWaitlistModelTest {

    private lateinit var model: OwnerWaitlistModel

    /**
     * Creates a new model before each test.
     */
    @Before
    fun setUp() {
        model = OwnerWaitlistModel()
    }

    /**
     * Checks that loading clears a previous error and selection.
     */
    @Test
    fun showLoading_clearsErrorAndSelection() {
        model.showError("Could not load lotteries.")
        model.selectLottery(lottery("lottery1"))

        model.showLoading()

        assertTrue(model.isLoading)
        assertNull(model.errorMessage)
        assertNull(model.selectedLottery)
    }

    /**
     * Checks that loaded lotteries are stored and loading stops.
     */
    @Test
    fun showLotteries_storesListAndStopsLoading() {
        val lotteries = listOf(lottery("lottery1"), lottery("lottery2"))
        model.showLoading()

        model.showLotteries(lotteries)

        assertEquals(lotteries, model.lotteries)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that selecting a lottery clears the previous entrants and starts loading.
     */
    @Test
    fun selectLottery_clearsPreviousEntrantsAndStartsLoading() {
        model.selectLottery(lottery("lottery1"))
        model.showEntrants("lottery1", listOf(entrant("entrant1", 1000)))

        model.selectLottery(lottery("lottery2"))

        assertEquals("lottery2", model.selectedLottery?.lottery?.id)
        assertTrue(model.entrants.isEmpty())
        assertTrue(model.isLoadingEntrants)
    }

    /**
     * Checks that entrants are shown in the order they joined, with missing times last.
     */
    @Test
    fun showEntrants_sortsByJoinTimeWithMissingTimesLast() {
        model.selectLottery(lottery("lottery1"))

        model.showEntrants(
            "lottery1",
            listOf(entrant("late", 3000), entrant("unknown", null), entrant("early", 1000))
        )

        assertEquals(listOf("early", "late", "unknown"), model.entrants.map { it.entrantId })
        assertFalse(model.isLoadingEntrants)
    }

    /**
     * Checks that entrants for a lottery that is no longer selected are ignored.
     */
    @Test
    fun showEntrants_otherLottery_isIgnored() {
        model.selectLottery(lottery("lottery2"))

        model.showEntrants("lottery1", listOf(entrant("entrant1", 1000)))

        assertTrue(model.entrants.isEmpty())
        assertTrue(model.isLoadingEntrants)
    }

    /**
     * Checks that a failed entrant lookup keeps the lottery selected and shows an error.
     */
    @Test
    fun showEntrantsError_keepsSelectionAndShowsMessage() {
        model.selectLottery(lottery("lottery1"))

        model.showEntrantsError("lottery1", "Could not load the waitlist.")

        assertEquals("lottery1", model.selectedLottery?.lottery?.id)
        assertEquals("Could not load the waitlist.", model.entrantsError)
        assertFalse(model.isLoadingEntrants)
    }

    /**
     * Checks that going back clears the selected lottery and its entrants.
     */
    @Test
    fun clearSelection_returnsToLotteryList() {
        model.selectLottery(lottery("lottery1"))
        model.showEntrants("lottery1", listOf(entrant("entrant1", 1000)))

        model.clearSelection()

        assertNull(model.selectedLottery)
        assertTrue(model.entrants.isEmpty())
        assertFalse(model.isLoadingEntrants)
    }

    private fun lottery(id: String) = OwnerWaitlistLottery(
        lottery = Lottery(id = id, ownerId = "owner1", campsiteIds = listOf("campsite1")),
        campsiteNames = listOf("Lakeside")
    )

    private fun entrant(id: String, joinedAtMillis: Long?) = WaitlistEntrant(
        entrantId = id,
        name = "Name $id",
        status = "waiting",
        joinedAt = joinedAtMillis?.let { Date(it) }
    )
}

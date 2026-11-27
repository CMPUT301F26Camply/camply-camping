package com.example.camply.lottery.model

import com.example.camply.campsite.model.Campsite
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the state held for the owner's lottery list and edit form.
 */
class UpdateLotteryModelTest {
    private lateinit var model: UpdateLotteryModel
    private fun lottery(id: String, winners: Int) = Lottery(
        id = id,
        ownerId = "owner-1",
        campsiteIds = listOf("site-1"),
        registrationStart = "2027-05-01",
        registrationEnd = "2027-05-10",
        bookingStart = "2027-05-15",
        bookingEnd = "2027-05-20",
        numberOfWinners = winners,
        lotteryStart = "2027-06-01",
        lotteryEnd = "2027-06-10",
        status = LotteryStatus.OPEN
    )

    @Before
    fun setUp() {
        model = UpdateLotteryModel()
    }

    // Showing lotteries stores them with the campsites and stops loading
    @Test
    fun showLotteries_storesListAndStopsLoading() {
        model.showLoading()
        assertTrue(model.isLoading)

        model.showLotteries(
            listOf(lottery("a", 1)),
            listOf(Campsite(id = "site-1", name = "Pine Ridge"))
        )

        assertEquals(1, model.lotteries.size)
        assertEquals("Pine Ridge", model.ownedCampsites.single().name)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    // An error stops loading and stores the message
    @Test
    fun showError_storesMessageAndStopsLoading() {
        model.showLoading()

        model.showError("offline")

        assertEquals("offline", model.errorMessage)
        assertFalse(model.isLoading)
    }

    // Opening the form for a lottery clears an old error
    @Test
    fun startEditing_setsLotteryAndClearsError() {
        model.showError("old error")

        model.startEditing(lottery("a", 1))

        assertEquals("a", model.editingLottery?.id)
        assertNull(model.errorMessage)
    }

    // A saved edit replaces only the matching lottery and closes the form
    @Test
    fun showUpdated_replacesOnlyTheEditedLotteryAndClosesForm() {
        model.showLotteries(listOf(lottery("a", 1), lottery("b", 2)), emptyList())
        model.startEditing(lottery("a", 1))

        model.showUpdated(lottery("a", 7))

        assertEquals(7, model.lotteries.first { it.id == "a" }.numberOfWinners)
        assertEquals(2, model.lotteries.first { it.id == "b" }.numberOfWinners)
        assertNull(model.editingLottery)
        assertNull(model.errorMessage)
    }
}
package com.example.camply.lottery.model

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for the state of the owner's waitlist screen (US 02.09.01, US 02.10.01).
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

    /**
     * Checks that a message can only be sent when it has text and the waitlist has entrants.
     */
    @Test
    fun canSend_requiresMessageAndEntrants() {
        model.selectLottery(lottery("lottery1"))
        model.startComposing()
        model.updateMessage("Quiet hours start at 10 PM.")
        assertFalse(model.canSend)

        model.showEntrants("lottery1", listOf(entrant("entrant1", 1000)))
        assertTrue(model.canSend)

        model.updateMessage("   ")
        assertFalse(model.canSend)
    }

    /**
     * Checks that a message longer than the limit is cut to the limit.
     */
    @Test
    fun updateMessage_overLimit_keepsLimit() {
        model.updateMessage("a".repeat(OwnerWaitlistModel.MAX_MESSAGE_LENGTH + 1))

        assertEquals(OwnerWaitlistModel.MAX_MESSAGE_LENGTH, model.message.length)
    }

    /**
     * Checks that a failed send keeps the draft and its event ID so a retry reaches only the
     * entrants who missed it, while editing the message gives it a new event ID.
     */
    @Test
    fun showSendError_keepsEventIdUntilMessageChanges() {
        composeFor("lottery1")
        model.showSending("event1")

        model.showSendError("lottery1", "Could not send to every entrant.")

        assertEquals("event1", model.messageEventId)
        assertEquals("Quiet hours start at 10 PM.", model.message)
        assertEquals("Could not send to every entrant.", model.sendError)
        assertFalse(model.isSending)

        model.updateMessage("Quiet hours start at 11 PM.")

        assertNull(model.messageEventId)
    }

    /**
     * Checks that a successful send closes the message box and shows how many entrants got it.
     */
    @Test
    fun showSent_closesMessageAndShowsCount() {
        composeFor("lottery1")
        model.showSending("event1")

        model.showSent("lottery1", 1)

        assertEquals(1, model.sentCount)
        assertFalse(model.isComposing)
        assertFalse(model.isSending)
        assertEquals("", model.message)
        assertNull(model.messageEventId)
    }

    /**
     * Checks that a send result for a lottery that is no longer selected is ignored.
     */
    @Test
    fun showSent_otherLottery_isIgnored() {
        composeFor("lottery1")
        model.showSending("event1")
        model.selectLottery(lottery("lottery2"))

        model.showSent("lottery1", 1)
        model.showSendError("lottery1", "Could not send to every entrant.")

        assertNull(model.sentCount)
        assertNull(model.sendError)
    }

    /**
     * Checks that cancelling discards the draft.
     */
    @Test
    fun cancelComposing_discardsDraft() {
        composeFor("lottery1")

        model.cancelComposing()

        assertFalse(model.isComposing)
        assertEquals("", model.message)
    }

    /**
     * Checks that going back to the lottery list clears the message state.
     */
    @Test
    fun clearSelection_clearsMessage() {
        composeFor("lottery1")
        model.showSending("event1")

        model.clearSelection()

        assertFalse(model.isComposing)
        assertFalse(model.isSending)
        assertEquals("", model.message)
        assertNull(model.messageEventId)
    }

    // Selects a lottery with one entrant and starts writing a message to it.
    private fun composeFor(lotteryId: String) {
        model.selectLottery(lottery(lotteryId))
        model.showEntrants(lotteryId, listOf(entrant("entrant1", 1000)))
        model.startComposing()
        model.updateMessage("Quiet hours start at 10 PM.")
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

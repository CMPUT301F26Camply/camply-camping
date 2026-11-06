package com.example.camply.lottery.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.lottery.controller.LotteryOpportunityController
import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.lottery.repository.DeclineResult
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for an entrant's lottery result and decline action (US 01.04.03).
 */
class LotteryOpportunityViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var entryRepository: TestLotteryEntryRepository
    private lateinit var view: LotteryOpportunityView

    /**
     * Creates the view with fake repositories before each test.
     */
    @Before
    fun setUp() {
        entryRepository = TestLotteryEntryRepository()
        val model = LotteryOpportunityModel()
        val controller = LotteryOpportunityController(
            model,
            entryRepository,
            TestReservationRepository(),
            BookingService()
        )
        view = LotteryOpportunityView(model, controller)
    }

    /**
     * Checks that a winner with an unused opportunity sees their result and the decline button.
     */
    @Test
    fun display_unusedWin_showsResultAndDeclineButton() {
        entryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        showView()

        compose.onNodeWithText("Result: You won").assertIsDisplayed()
        compose.onNodeWithText(DECLINE_BUTTON).assertIsDisplayed()
    }

    /**
     * Checks that an entrant who did not win cannot decline.
     */
    @Test
    fun display_notWinner_hidesDeclineButton() {
        entryRepository.entry = LotteryEntry(status = "lost")

        showView()

        compose.onNodeWithText("Result: Not selected").assertIsDisplayed()
        compose.onNodeWithText(DECLINE_BUTTON).assertDoesNotExist()
    }

    /**
     * Checks that cancelling the dialog does not decline the opportunity.
     */
    @Test
    fun declineDialog_cancel_doesNotDecline() {
        entryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        showView()
        compose.onNodeWithText(DECLINE_BUTTON).performClick()
        compose.onNodeWithText("Cancel").performClick()

        compose.runOnIdle { assertEquals(0, entryRepository.declineCount) }
        compose.onNodeWithText(DECLINE_BUTTON).assertIsDisplayed()
        compose.onNodeWithText("Opportunity declined").assertDoesNotExist()
    }

    /**
     * Checks that confirming the dialog declines the opportunity and removes the button.
     */
    @Test
    fun declineDialog_confirm_showsDeclined() {
        entryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        showView()
        compose.onNodeWithText(DECLINE_BUTTON).performClick()
        // The card and the dialog both have a "Decline" button, so this targets the dialog's.
        compose.onNode(hasText("Decline") and hasAnyAncestor(isDialog())).performClick()

        compose.onNodeWithText("Opportunity declined").assertIsDisplayed()
        compose.onNodeWithText("Result: You won").assertIsDisplayed()
        compose.onNodeWithText(DECLINE_BUTTON).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, entryRepository.declineCount) }
    }

    private fun showView() {
        compose.setContent { view.Display(LOTTERY_ID, ENTRANT_ID) }
    }

    /**
     * Provides lottery entries without accessing Firestore.
     */
    private class TestLotteryEntryRepository : LotteryEntryRepository {
        var entry: LotteryEntry? = null
        var declineCount = 0

        override suspend fun getEntry(lotteryId: String, entrantId: String): LotteryEntry? = entry

        override suspend fun declineOpportunity(lotteryId: String, entrantId: String): DeclineResult {
            declineCount++
            return DeclineResult.DECLINED
        }
    }

    /**
     * Provides an empty reservation list without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {
        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> = emptyList()

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> = emptyList()

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"
    }

    private companion object {
        const val LOTTERY_ID = "lottery1"
        const val ENTRANT_ID = "entrant1"
        const val DECLINE_BUTTON = "Decline"
    }
}

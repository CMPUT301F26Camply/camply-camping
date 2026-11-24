package com.example.camply.lottery.view

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryStatus
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the extra content (Decline section) shown on My Lotteries cards (US 01.04.03).
 */
class EntrantLotteryViewTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Checks that each lottery's card shows the content given for that lottery.
     */
    @Test
    fun display_lottery_showsCardContent() {
        val model = EntrantLotteryModel()
        model.showLotteries(listOf(Lottery(id = "lottery1")))
        val view = EntrantLotteryView(model)

        compose.setContent {
            view.Display { lottery -> Text("Content for ${lottery.id}") }
        }

        compose.onNodeWithText("Lottery lottery1").assertIsDisplayed()
        compose.onNodeWithText("Content for lottery1").assertIsDisplayed()
    }


    /**
     * Shows the empty state when the entrant has not joined any lotteries.
     */
    @Test
    fun display_noLotteries_showsEmptyMessage() {
        val model = EntrantLotteryModel()
        model.showLotteries(emptyList())

        val view = EntrantLotteryView(model)
        compose.setContent { view.Display() }

        compose.onNodeWithText("You haven't joined any lotteries yet. Browse campsites to find an available lottery.")
            .assertIsDisplayed()
    }

    /**
     * Shows an error message when lottery information could not be loaded.
     */
    @Test
    fun display_error_showsErrorMessage() {
        val model = EntrantLotteryModel()
        model.showError("Could not load lotteries.")

        val view = EntrantLotteryView(model)
        compose.setContent { view.Display() }

        compose.onNodeWithText("Could not load lotteries.").assertIsDisplayed()
    }

    /**
     * Shows the booking status and all three lottery date periods.
     */
    @Test
    fun display_bookingLottery_showsStatusAndDates() {
        val model = EntrantLotteryModel()
        model.showLotteries(
            listOf(
                Lottery(
                    id = "lottery1",
                    status = LotteryStatus.BOOKING,
                    lotteryStart = "2027-08-01",
                    lotteryEnd = "2027-08-04",
                    registrationStart = "2027-05-01",
                    registrationEnd = "2027-05-15",
                    bookingStart = "2027-06-01",
                    bookingEnd = "2027-06-10"
                )
            )
        )

        val view = EntrantLotteryView(model)
        compose.setContent { view.Display() }

        compose.onNodeWithText("Lottery lottery1").assertIsDisplayed()
        compose.onNodeWithText("Booking").assertIsDisplayed()
        compose.onNodeWithText("2027-08-01 to 2027-08-04").assertIsDisplayed()
        compose.onNodeWithText("2027-05-01 to 2027-05-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-10").assertIsDisplayed()
    }

    /**
     * Hides the stay dates section when the lottery has no stay window.
     */
    @Test
    fun display_noStayDates_hidesStayDatesSection() {
        val model = EntrantLotteryModel()
        model.showLotteries(
            listOf(
                Lottery(
                    id = "lottery1",
                    lotteryStart = "",
                    lotteryEnd = ""
                )
            )
        )

        val view = EntrantLotteryView(model)
        compose.setContent { view.Display() }

        compose.onNodeWithText("Stay dates").assertDoesNotExist()
        compose.onNodeWithText("Registration period").assertIsDisplayed()
        compose.onNodeWithText("Booking period").assertIsDisplayed()
    }

    /**
     * Displays the correct status when a lottery draw is complete.
     */
    @Test
    fun display_drawnLottery_showsDrawComplete() {
        val model = EntrantLotteryModel()
        model.showLotteries(
            listOf(
                Lottery(
                    id = "lottery1",
                    status = LotteryStatus.DRAWN
                )
            )
        )

        val view = EntrantLotteryView(model)
        compose.setContent { view.Display() }

        compose.onNodeWithText("Draw complete").assertIsDisplayed()
        compose.onNodeWithText("Booking").assertDoesNotExist()
    }
}

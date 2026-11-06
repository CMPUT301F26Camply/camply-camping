package com.example.camply.lottery.view

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
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
}

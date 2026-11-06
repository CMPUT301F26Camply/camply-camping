package com.example.camply.lottery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard

/**
 * Displays the entrant's joined lotteries and their information.
 *
 * @param model The model containing the lottery screen state.
 */
class EntrantLotteryView(
    private val model: EntrantLotteryModel
) {

    /**
     * Displays the My Lotteries screen using the current model state.
     *
     * @param cardContent Extra content (Decline section) shown at the bottom of each lottery's card.
     */
    @Composable
    fun Display(cardContent: @Composable (Lottery) -> Unit = {}) {
        val error = model.errorMessage

        CamplyScreen(title = "My Lotteries") {
            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }

                error != null -> CenteredMessage(error)

                model.lotteries.isEmpty() -> CenteredMessage("Joined lottery information is not available yet.")

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(model.lotteries, key = { it.id }) { lottery ->
                        OrangeCard {
                            Text(
                                text = "Lottery ${lottery.id}",
                                color = CamplyPalette.Ink
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Registration: ${lottery.registrationStart} to ${lottery.registrationEnd}",
                                color = CamplyPalette.Ink
                            )

                            Text(
                                text = "Booking: ${lottery.bookingStart} to ${lottery.bookingEnd}",
                                color = CamplyPalette.Ink
                            )

                            Spacer(Modifier.height(8.dp))

                            cardContent(lottery)
                        }
                    }
                }
            }
        }
    }
}
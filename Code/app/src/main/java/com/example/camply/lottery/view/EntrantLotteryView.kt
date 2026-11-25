package com.example.camply.lottery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                error != null -> CenteredMessage(error)

                model.lotteries.isEmpty() -> CenteredMessage(
                    "You haven't joined any lotteries yet. Browse campsites to find an available lottery."
                )


                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(model.lotteries, key = { it.id }) { lottery ->

                        val statusLabel = when (lottery.status) {
                            LotteryStatus.OPEN -> "Open"
                            LotteryStatus.DRAWN -> "Draw complete"
                            LotteryStatus.BOOKING -> "Booking"
                            LotteryStatus.CLOSED -> "Closed"
                            LotteryStatus.UNINITIALIZED -> "Pending"
                        }

                        val statusBackground = when (lottery.status) {
                            LotteryStatus.OPEN, LotteryStatus.BOOKING -> MaterialTheme.colorScheme.primaryContainer
                            LotteryStatus.DRAWN -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val statusColor = when (lottery.status) {
                            LotteryStatus.OPEN, LotteryStatus.BOOKING -> MaterialTheme.colorScheme.onPrimaryContainer
                            LotteryStatus.DRAWN -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        OrangeCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    CardLabel("Joined lottery")
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Lottery ${lottery.id.take(8)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = statusBackground
                                ) {
                                    Text(
                                        statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        modifier = Modifier.padding(10.dp, 7.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (lottery.lotteryStart.isNotBlank() && lottery.lotteryEnd.isNotBlank()) {
                                Text(
                                    "Stay dates",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${lottery.lotteryStart} to ${lottery.lotteryEnd}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            Text(
                                "Registration period",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${lottery.registrationStart} to ${lottery.registrationEnd}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Booking period",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${lottery.bookingStart} to ${lottery.bookingEnd}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(16.dp))

                            cardContent(lottery)
                        }
                    }
                }
            }
        }
    }
}
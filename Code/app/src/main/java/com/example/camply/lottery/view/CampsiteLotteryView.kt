package com.example.camply.lottery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.controller.CampsiteLotteryController
import com.example.camply.lottery.controller.WaitingListController
import com.example.camply.lottery.model.CampsiteLotteryModel
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton

class CampsiteLotteryView(
    private val model: CampsiteLotteryModel,
    private val controller: CampsiteLotteryController
) {
    @Composable
    fun Display() {
        LaunchedEffect(model) { controller.loadLottery() }
        val waitingList = model.waitingList
        when {
            model.isLoading -> OrangeCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        "Loading campsite lottery...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            model.hasError -> OrangeCard {
                CardLabel("Lottery")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Unable to load the lottery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Try again", controller::loadLottery)
            }

            waitingList != null -> {
                val view = remember(waitingList) {
                    WaitingListView(waitingList, WaitingListController(waitingList))
                }
                view.Display()
            }
            else -> OrangeCard {
                CardLabel("Lottery")
                Spacer(Modifier.height(8.dp))
                Text(
                    "No lottery is accepting entries for this campsite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Check again", controller::loadLottery)
            }
        }
    }
}

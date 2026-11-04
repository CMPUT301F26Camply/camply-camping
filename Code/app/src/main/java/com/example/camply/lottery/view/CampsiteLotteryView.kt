package com.example.camply.lottery.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.camply.lottery.controller.CampsiteLotteryController
import com.example.camply.lottery.controller.WaitingListController
import com.example.camply.lottery.model.CampsiteLotteryModel
import com.example.camply.shared.view.CamplyPalette
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
                Text("Loading campsite lottery...", color = CamplyPalette.Ink)
            }
            model.hasError -> OrangeCard {
                Text("Unable to load the lottery.", color = CamplyPalette.Ink)
                PrimaryButton("Try again", controller::loadLottery)
            }
            waitingList != null -> {
                val view = remember(waitingList) {
                    WaitingListView(waitingList, WaitingListController(waitingList))
                }
                view.Display()
            }
            else -> OrangeCard {
                Text("No lottery is accepting entries for this campsite.", color = CamplyPalette.Ink)
                PrimaryButton("Check again", controller::loadLottery)
            }
        }
    }
}

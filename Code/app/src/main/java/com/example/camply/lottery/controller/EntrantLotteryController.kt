package com.example.camply.lottery.controller

import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.repository.LotteryRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles loading the entrant's joined lotteries for the My Lotteries screen.
 *
 * Updates the model using lottery information retrieved from the repository.
 *
 * @param model The model storing the screen state.
 * @param repository The repository used to retrieve lotteries.
 */
class EntrantLotteryController(
    private val model: EntrantLotteryModel,
    private val repository: LotteryRepository
) {
    /**
     * Loads the lotteries associated with the provided lottery IDs.
     *
     * @param lotteryIds The IDs of lotteries joined by the entrant.
     */
    suspend fun loadLotteries(lotteryIds: List<String>) {
        model.showLoading()

        try {
            val lotteries = mutableListOf<Lottery>()

            for (id in lotteryIds) {
                val lottery = repository.getLottery(id)
                if (lottery != null) {
                    lotteries.add(lottery)
                }
            }

            model.showLotteries(lotteries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load lotteries.")
        }
    }
}
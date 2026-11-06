package com.example.camply.lottery.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryBookingModel
import kotlin.coroutines.cancellation.CancellationException


/**
 * Handles loading a campsite when an entrant chooses to book a winning lottery.
 * US 01.04.01.
 *
 * @param model The model storing the selected lottery and campsite.
 * @param campsiteRepository The repository used to retrieve campsite information.
 */
class LotteryBookingController(
    private val model: LotteryBookingModel,
    private val campsiteRepository: CampsiteRepository
) {

    /**
     * Loads the campsite associated with a lottery and prepares the booking screen.
     *
     * @param lottery The lottery selected by the entrant.
     */
    suspend fun loadBooking(lottery: Lottery) {
        model.showLoading()

        try {
            val campsiteId = lottery.campsiteIds.firstOrNull()

            if (campsiteId == null) {
                model.showError("No campsite is linked to this lottery.")
                return
            }

            val campsite = campsiteRepository.getCampsite(campsiteId)

            if (campsite == null) {
                model.showError("Campsite not found.")
                return
            }

            model.showBooking(lottery, campsite)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load campsite.")
        }
    }

    /**
     * Clears the selected booking when the entrant returns to My Lotteries.
     */
    fun clearBooking() {
        model.clearBooking()
    }
}

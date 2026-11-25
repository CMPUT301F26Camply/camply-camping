package com.example.camply.lottery.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.OwnerWaitlistLottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.lottery.repository.OwnerWaitlistRepository
import com.example.camply.notification.repository.RecipientNameRepository

/**
 * Handles the owner's waitlist screen (US 02.09.01).
 *
 * Loads the owner's lotteries, then the entrants who joined the selected lottery.
 *
 * @param model The model storing the screen state.
 * @param waitlistRepository The repository used to retrieve lotteries and their entrants.
 * @param campsiteRepository The repository used to retrieve campsite names.
 * @param nameRepository The repository used to retrieve entrant names.
 */
class OwnerWaitlistController(
    private val model: OwnerWaitlistModel,
    private val waitlistRepository: OwnerWaitlistRepository,
    private val campsiteRepository: CampsiteRepository,
    private val nameRepository: RecipientNameRepository
) {

    /**
     * Loads the owner's lotteries, newest registration period first.
     *
     * @param ownerId The owner's Firebase user ID.
     */
    suspend fun loadLotteries(ownerId: String) {
        model.showLoading()

        try {
            val lotteries = waitlistRepository.getOwnerLotteries(ownerId)
                .sortedByDescending { it.registrationStart }
                .map { OwnerWaitlistLottery(it, findCampsiteName(it)) }
            model.showLotteries(lotteries)
        } catch (_: Exception) {
            model.showError("Could not load your lotteries.")
        }
    }

    /**
     * Selects a lottery and loads the entrants who joined it.
     *
     * @param item The selected lottery.
     */
    suspend fun onLotterySelected(item: OwnerWaitlistLottery) {
        val lotteryId = item.lottery.id
        model.selectLottery(item)

        try {
            val entrants = waitlistRepository.getEntrants(lotteryId)
                .map { it.copy(name = findEntrantName(it)) }
            // The model ignores these results if another lottery was selected meanwhile.
            model.showEntrants(lotteryId, entrants)
        } catch (_: Exception) {
            model.showEntrantsError(lotteryId, "Could not load the waitlist.")
        }
    }

    /**
     * Returns to the owner's lottery list.
     */
    fun onBackToLotteries() {
        model.clearSelection()
    }

    // A failed lookup only hides this name, so the rest of the list still loads.
    private suspend fun findCampsiteName(lottery: Lottery): String? {
        val campsiteId = lottery.campsiteIds.firstOrNull() ?: return null
        return try {
            campsiteRepository.getCampsite(campsiteId)?.name
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun findEntrantName(entrant: WaitlistEntrant): String? =
        try {
            nameRepository.getRecipientName(entrant.entrantId)
        } catch (_: Exception) {
            null
        }
}

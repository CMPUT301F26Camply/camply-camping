package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Date

/**
 * Represents one entrant who joined a lottery's waitlist (US 02.09.01).
 *
 * @property entrantId The entrant's Firebase user ID.
 * @property name The entrant's name, or null.
 * @property status The entry's draw status ("waiting", "won", or "lost").
 * @property joinedAt When the entrant joined, or null.
 */
data class WaitlistEntrant(
    val entrantId: String,
    val name: String?,
    val status: String,
    val joinedAt: Date?
)

/**
 * Represents one of the owner's lotteries in the waitlist screen.
 *
 * @property lottery The lottery.
 * @property campsiteNames The names of the lottery's campsites that could be found.
 */
data class OwnerWaitlistLottery(
    val lottery: Lottery,
    val campsiteNames: List<String>
)

/**
 * Stores the state of the owner's waitlist screen (US 02.09.01).
 *
 * The owner first sees their lotteries, then selects one to see the entrants who joined it.
 */
class OwnerWaitlistModel {

    var lotteries: List<OwnerWaitlistLottery> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var selectedLottery: OwnerWaitlistLottery? by mutableStateOf(null)
        private set

    var entrants: List<WaitlistEntrant> by mutableStateOf(emptyList())
        private set

    var isLoadingEntrants: Boolean by mutableStateOf(false)
        private set

    var entrantsError: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model when the owner's lotteries are being loaded.
     *
     * Clears the selection so that opening the screen again starts from the lottery list.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
        clearSelection()
    }

    /**
     * Updates the model with the owner's lotteries.
     *
     * @param list The lotteries to display.
     */
    fun showLotteries(list: List<OwnerWaitlistLottery>) {
        lotteries = list
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when the owner's lotteries cannot be loaded.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }

    /**
     * Stores the lottery whose waitlist should be displayed and starts loading its entrants.
     *
     * @param item The selected lottery.
     */
    fun selectLottery(item: OwnerWaitlistLottery) {
        selectedLottery = item
        entrants = emptyList()
        entrantsError = null
        isLoadingEntrants = true
    }

    /**
     * Updates the model with the entrants of the selected lottery.
     *
     * Entrants are shown in the order they joined, and those without a join time are shown last.
     * Results for a lottery that is no longer selected are ignored.
     *
     * @param lotteryId The ID of the lottery the entrants belong to.
     * @param list The entrants who joined that lottery.
     */
    fun showEntrants(lotteryId: String, list: List<WaitlistEntrant>) {
        if (selectedLottery?.lottery?.id != lotteryId) return
        entrants = list.sortedWith(compareBy(nullsLast()) { it.joinedAt })
        isLoadingEntrants = false
        entrantsError = null
    }

    /**
     * Updates the model when the selected lottery's entrants cannot be loaded.
     *
     * Errors for a lottery that is no longer selected are ignored.
     *
     * @param lotteryId The ID of the lottery whose entrants could not be loaded.
     * @param message The error message to display.
     */
    fun showEntrantsError(lotteryId: String, message: String) {
        if (selectedLottery?.lottery?.id != lotteryId) return
        isLoadingEntrants = false
        entrantsError = message
    }

    /**
     * Clears the selected lottery so that the lottery list is displayed.
     */
    fun clearSelection() {
        selectedLottery = null
        entrants = emptyList()
        isLoadingEntrants = false
        entrantsError = null
    }
}

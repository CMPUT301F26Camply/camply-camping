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
 * From a lottery's waitlist, the owner can send a notification to its entrants (US 02.10.01).
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

    var isComposing: Boolean by mutableStateOf(false)
        private set

    var message: String by mutableStateOf("")
        private set

    var isSending: Boolean by mutableStateOf(false)
        private set

    var sendError: String? by mutableStateOf(null)
        private set

    // The number of entrants the last message was sent to, or null if nothing was sent.
    var sentCount: Int? by mutableStateOf(null)
        private set

    // Shared by every record of the current message, so a retry cannot send it twice.
    var messageEventId: String? by mutableStateOf(null)
        private set

    /** Whether the message can be sent to the selected lottery's entrants. */
    val canSend: Boolean
        get() = message.isNotBlank() && entrants.isNotEmpty() && !isSending

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
        clearMessage()
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
        clearMessage()
    }

    /**
     * Opens the message box for the selected lottery's entrants.
     */
    fun startComposing() {
        isComposing = true
        sentCount = null
        sendError = null
    }

    /**
     * Closes the message box and discards the draft.
     */
    fun cancelComposing() = clearMessage()

    /**
     * Updates the message being written, up to [MAX_MESSAGE_LENGTH] characters.
     *
     * A changed message is a new send, so it gets a new event ID when it is sent.
     *
     * @param text The message typed by the owner.
     */
    fun updateMessage(text: String) {
        val limited = text.take(MAX_MESSAGE_LENGTH)
        if (limited != message) messageEventId = null
        message = limited
    }

    /**
     * Updates the model while the message is being sent.
     *
     * @param eventId The event ID shared by every record of this message.
     */
    fun showSending(eventId: String) {
        messageEventId = eventId
        isSending = true
        sendError = null
    }

    /**
     * Updates the model after the message was sent to every entrant.
     *
     * Results for a lottery that is no longer selected are ignored.
     *
     * @param lotteryId The ID of the lottery the message was sent for.
     * @param count The number of entrants who received the message.
     */
    fun showSent(lotteryId: String, count: Int) {
        if (selectedLottery?.lottery?.id != lotteryId) return
        clearMessage()
        sentCount = count
    }

    /**
     * Updates the model when the message could not be sent to every entrant.
     *
     * Keeps the draft and its event ID, so trying again only reaches the entrants who missed it.
     * Errors for a lottery that is no longer selected are ignored.
     *
     * @param lotteryId The ID of the lottery the message was sent for.
     * @param error The error message to display.
     */
    fun showSendError(lotteryId: String, error: String) {
        if (selectedLottery?.lottery?.id != lotteryId) return
        isSending = false
        sendError = error
    }

    private fun clearMessage() {
        isComposing = false
        message = ""
        isSending = false
        sendError = null
        sentCount = null
        messageEventId = null
    }

    companion object {
        /** The longest message an owner can send. */
        const val MAX_MESSAGE_LENGTH = 500
    }
}

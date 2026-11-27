package com.example.camply.lottery.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.OwnerWaitlistLottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.lottery.repository.OwnerWaitlistRepository
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.notification.service.NotificationService
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles the owner's waitlist screen (US 02.09.01, US 02.10.01).
 *
 * Loads the owner's lotteries, then the entrants who joined the selected lottery,
 * and sends the owner's notifications to those entrants.
 *
 * @param model The model storing the screen state.
 * @param waitlistRepository The repository used to retrieve lotteries and their entrants.
 * @param campsiteRepository The repository used to retrieve campsite names.
 * @param nameRepository The repository used to retrieve entrant names.
 * @param notificationService The service used to save notifications for entrants.
 */
class OwnerWaitlistController(
    private val model: OwnerWaitlistModel,
    private val waitlistRepository: OwnerWaitlistRepository,
    private val campsiteRepository: CampsiteRepository,
    private val nameRepository: RecipientNameRepository,
    private val notificationService: NotificationService
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
                .map { OwnerWaitlistLottery(it, findCampsiteNames(it)) }
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

    /**
     * Opens the message box for the selected lottery's entrants.
     */
    fun onStartMessage() = model.startComposing()

    /**
     * Updates the message being written.
     *
     * @param text The message typed by the owner.
     */
    fun onMessageChanged(text: String) = model.updateMessage(text)

    /**
     * Closes the message box without sending.
     */
    fun onCancelMessage() = model.cancelComposing()

    /**
     * Sends the owner's message to every entrant on the selected lottery's waitlist.
     *
     * Each entrant gets their own notification record, and all records share one event ID.
     * A failure for one entrant does not stop the others. Trying again reuses the event ID,
     * so entrants who already received the message are not sent it twice.
     *
     * @param ownerId The owner's Firebase user ID.
     */
    suspend fun onSendNotification(ownerId: String) {
        val item = model.selectedLottery ?: return
        if (!model.canSend) return

        val lotteryId = item.lottery.id
        val eventId = model.messageEventId ?: "owner-message:${UUID.randomUUID()}"
        val title = waitlistTitle(item)
        val content = model.message.trim()
        val recipients = model.entrants.map { it.entrantId }.distinct()

        model.showSending(eventId)

        val failed = recipients.count { entrantId ->
            !sendTo(entrantId, eventId, ownerId, title, content, lotteryId)
        }

        if (failed == 0) {
            model.showSent(lotteryId, recipients.size)
        } else {
            model.showSendError(
                lotteryId,
                "Could not send to $failed of ${recipients.size} entrants. Try again."
            )
        }
    }

    // Returns whether the entrant's notification was saved.
    private suspend fun sendTo(
        entrantId: String,
        eventId: String,
        ownerId: String,
        title: String,
        content: String,
        lotteryId: String
    ): Boolean =
        try {
            notificationService.send(
                NewNotification(
                    eventId = eventId,
                    senderId = ownerId,
                    recipientId = entrantId,
                    origin = NotificationOrigin.OWNER,
                    type = NotificationType.OWNER_MESSAGE,
                    title = title,
                    content = content,
                    lotteryId = lotteryId
                )
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

    private fun waitlistTitle(item: OwnerWaitlistLottery): String =
        item.campsiteNames.joinToString(", ").ifEmpty { "Lottery" } + " waitlist update"

    // A lottery can include several campsites. A failed lookup only skips that
    // campsite's name, so the rest of the list still loads.
    private suspend fun findCampsiteNames(lottery: Lottery): List<String> =
        lottery.campsiteIds.mapNotNull { campsiteId ->
            try {
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

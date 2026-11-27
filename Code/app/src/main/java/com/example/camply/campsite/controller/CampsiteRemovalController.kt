package com.example.camply.campsite.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRemovalModel
import com.example.camply.campsite.model.CampsiteRemovalRepository
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Removes one campsite for an administrator (US 03.01.01).
 *
 * Notifies entrants in every lottery that includes the campsite. A lottery with no
 * campsites left is cancelled and deleted. A lottery that still has other campsites
 * loses only this campsite. Confirmed reservations whose start date is after today
 * are cancelled and deleted. Past reservations are left in place. The campsite
 * document is deleted last.
 *
 * @param model Stores whether a removal is in progress and the latest error.
 * @param removalRepository Reads and updates the lotteries and reservations involved.
 * @param campsiteRepository Loads the campsite and deletes it at the end.
 * @param notifications Sends the lottery and reservation notices.
 * @param today Supplies today's date as YYYY-MM-DD. Tests can pass a fixed date.
 */
class CampsiteRemovalController(
    private val model: CampsiteRemovalModel,
    private val removalRepository: CampsiteRemovalRepository,
    private val campsiteRepository: CampsiteRepository,
    private val notifications: NotificationService,
    private val today: () -> String = { LocalDate.now().toString() }
) {
    /**
     * Runs the full removal for one campsite.
     *
     * @param campsiteId Firestore id of the campsite to remove.
     * @return true after the campsite document is deleted, or false when the campsite
     * is missing or a step fails. A second call while one is already running returns false.
     */
    suspend fun removeCampsite(campsiteId: String): Boolean {
        if (model.isRemoving) return false
        model.start()
        try {
            val campsite = campsiteRepository.getCampsite(campsiteId)
                ?: run {
                    model.fail("This campsite no longer exists.")
                    return false
                }
            updateLotteries(campsite)
            cancelFutureReservations(campsite)
            campsiteRepository.deleteCampsite(campsite.id)
            model.finish()
            return true
        } catch (e: CancellationException) {
            model.finish()
            throw e
        } catch (e: Exception) {
            model.fail(e.message ?: "Could not remove the campsite.")
            return false
        }
    }

    private suspend fun updateLotteries(campsite: Campsite) {
        val lotteries = removalRepository.getLotteriesForCampsite(campsite.id)
        for (lottery in lotteries) {
            val entrantIds = removalRepository.getLotteryEntrantIds(lottery.id)
            val remaining = lottery.campsiteIds.filter { it != campsite.id }
            if (remaining.isEmpty()) {
                notifyLottery(entrantIds, lottery, campsite, cancelled = true)
                removalRepository.deleteLottery(lottery.id)
            } else {
                notifyLottery(entrantIds, lottery, campsite, cancelled = false)
                removalRepository.removeCampsiteFromLottery(lottery.id, campsite.id)
            }
        }
    }

    private suspend fun notifyLottery(
        entrantIds: List<String>,
        lottery: Lottery,
        campsite: Campsite,
        cancelled: Boolean
    ) {
        for (entrantId in entrantIds.distinct()) {
            try {
                if (cancelled) {
                    notifications.sendLotteryCancelled(campsite.ownerId, entrantId, lottery.id, campsite.id)
                } else {
                    notifications.sendCampsiteRemovedFromLottery(campsite.ownerId, entrantId, lottery.id, campsite.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // One failed notice does not stop the removal.
            }
        }
    }

    private suspend fun cancelFutureReservations(campsite: Campsite) {
        val currentDate = today()
        val reservations = removalRepository.getReservationsForCampsite(campsite.id)
        for (reservation in reservations) {
            if (!shouldRemove(reservation, currentDate)) continue
            try {
                notifications.sendReservationRemovedForCampsite(
                    campsite.ownerId,
                    reservation.entrantId,
                    reservation.id,
                    campsite.name,
                    reservation.dates
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The reservation is still removed below.
            }
            removalRepository.deleteReservation(reservation.id)
        }
    }

    private fun shouldRemove(reservation: Reservation, currentDate: String): Boolean {
        return reservation.status == ReservationStatus.CONFIRMED &&
                reservation.dates.from > currentDate
    }
}
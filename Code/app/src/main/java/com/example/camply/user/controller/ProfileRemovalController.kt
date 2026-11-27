package com.example.camply.user.controller

import com.example.camply.campsite.controller.CampsiteRemovalController
import com.example.camply.campsite.model.CampsiteRemovalModel
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.repository.LotteryRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.user.model.ProfileRemovalModel
import com.example.camply.user.repository.ProfileRemovalRepository
import com.example.camply.user.model.User
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Removes one profile for an administrator (US 03.02.01).
 *
 * An owner is removed by running campsite removal for every campsite they own, then
 * deleting the user document. An entrant is removed from lotteries that have not ended
 * and from confirmed reservations that start after today. [removeEntrant] is public so
 * an entrant can delete their own profile later (US 01.08.01) without the admin screen.
 *
 * @param model Stores whether a removal is in progress and the latest error.
 * @param profileRepository Deletes lottery entries, future reservations, and the user document.
 * @param campsiteRepository Loads an owner's campsites and a reservation's campsite name.
 * @param campsiteRemoval Removes one campsite, including its lotteries and future reservations.
 * @param campsiteRemovalModel Error from the campsite removal that just failed.
 * @param lotteryRepository Finds the lotteries an entrant joined.
 * @param reservationRepository Finds an entrant's reservations.
 * @param notifications Tells a lottery or campsite owner that the entrant was removed.
 * @param today Supplies today's date as YYYY-MM-DD. Tests can pass a fixed date.
 */
class ProfileRemovalController(
    private val model: ProfileRemovalModel,
    private val profileRepository: ProfileRemovalRepository,
    private val campsiteRepository: CampsiteRepository,
    private val campsiteRemoval: CampsiteRemovalController,
    private val campsiteRemovalModel: CampsiteRemovalModel,
    private val lotteryRepository: LotteryRepository,
    private val reservationRepository: ReservationRepository,
    private val notifications: NotificationService,
    private val today: () -> String = { LocalDate.now().toString() }
) {
    /**
     * Removes an owner or an entrant, then deletes the user document.
     *
     * @param user Profile selected by the admin.
     * @return true after the user document is deleted. An admin profile returns false.
     */
    suspend fun removeProfile(user: User): Boolean {
        if (model.isRemoving) return false
        model.start()
        try {
            val removed = when (user.getRole()) {
                "owner" -> removeOwner(user.getUid())
                "entrant" -> removeEntrant(user.getUid())
                else -> {
                    model.fail("This profile cannot be removed.")
                    return false
                }
            }
            if (!removed) return false
            profileRepository.deleteUser(user.getUid())
            model.finish()
            return true
        } catch (e: CancellationException) {
            model.finish()
            throw e
        } catch (e: Exception) {
            model.fail(e.message ?: "Could not remove the profile.")
            return false
        }
    }

    /**
     * Removes every campsite owned by [ownerId].
     *
     * Does not delete the user document. [removeProfile] does that after this returns true.
     */
    private suspend fun removeOwner(ownerId: String): Boolean {
        val campsites = campsiteRepository.getCampsitesByOwner(ownerId)
        for (campsite in campsites) {
            if (campsiteRemoval.removeCampsite(campsite.id)) continue
            model.fail(campsiteRemovalModel.error ?: "Could not remove ${campsite.name}.")
            return false
        }
        return true
    }

    /**
     * Removes one entrant from current lotteries and future reservations (US 03.02.01, US 01.08.01).
     *
     * A lottery whose end date is before today is left alone. A confirmed reservation is
     * deleted only when its start date is after today. This does not delete the user document.
     *
     * @param entrantId Firebase user ID of the entrant.
     * @return true when every current lottery entry and future reservation has been removed.
     */
    suspend fun removeEntrant(entrantId: String): Boolean {
        val currentDate = today()
        for (lottery in lotteryRepository.getJoinedLotteries(entrantId)) {
            if (!isCurrent(lottery, currentDate)) continue
            notifyOwner {
                notifications.sendEntrantRemovedFromLottery(lottery.ownerId, entrantId, lottery.id)
            }
            profileRepository.deleteLotteryEntry(lottery.id, entrantId)
        }
        for (reservation in reservationRepository.getReservationsForEntrant(entrantId)) {
            if (!shouldRemove(reservation, currentDate)) continue
            val campsite = campsiteRepository.getCampsite(reservation.campsiteId)
            if (campsite != null) {
                notifyOwner {
                    notifications.sendReservationRemovedForEntrant(
                        campsite.ownerId,
                        entrantId,
                        reservation.id,
                        campsite.name,
                        reservation.dates
                    )
                }
            }
            profileRepository.deleteReservation(reservation.id)
        }
        return true
    }

    /** A lottery with no end date, or an end date of today or later, still includes this entrant. */
    private fun isCurrent(lottery: Lottery, currentDate: String): Boolean {
        return lottery.lotteryEnd.isBlank() || lottery.lotteryEnd >= currentDate
    }

    /** Same date rule as removing one campsite: confirmed, and the stay has not started yet. */
    private fun shouldRemove(reservation: Reservation, currentDate: String): Boolean {
        return reservation.status == ReservationStatus.CONFIRMED &&
                reservation.dates.from > currentDate
    }

    /** One failed notice does not stop the profile removal. */
    private suspend fun notifyOwner(send: suspend () -> Unit) {
        try {
            send()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The entry or reservation is still removed by the caller.
        }
    }
}
package com.example.camply.lottery.controller

import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.lottery.repository.DeclineResult
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles loading and declining an entrant's reservation opportunity (US 01.04.03).
 *
 * @param model The model storing the opportunity's state.
 * @param entryRepository The repository used to read and decline lottery entries.
 * @param reservationRepository The repository used to retrieve the entrant's reservations.
 * @param bookingService The service used to check whether the opportunity was already used.
 */
class LotteryOpportunityController(
    private val model: LotteryOpportunityModel,
    private val entryRepository: LotteryEntryRepository,
    private val reservationRepository: ReservationRepository,
    private val bookingService: BookingService
) {

    /**
     * Loads an entrant's entry in a lottery.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun loadEntry(lotteryId: String, entrantId: String) {
        model.showLoading()

        try {
            model.showEntry(entryRepository.getEntry(lotteryId, entrantId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load your lottery result.")
        }
    }

    /**
     * Declines an entrant's reservation opportunity if it has not been used.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     */
    suspend fun declineOpportunity(lotteryId: String, entrantId: String) {
        model.showLoading()

        try {
            val reservations = reservationRepository.getReservationsForEntrant(entrantId)

            if (bookingService.hasExistingLotteryReservation(entrantId, lotteryId, reservations)) {
                model.showError("You already have a reservation for this lottery.")
                return
            }

            when (entryRepository.declineOpportunity(lotteryId, entrantId)) {
                // The decline is already saved, so a failed re-read must not make it look unsaved.
                DeclineResult.DECLINED -> model.showEntry(
                    model.entry?.copy(opportunityStatus = LotteryEntry.OPPORTUNITY_DECLINED)
                )

                DeclineResult.NOT_ELIGIBLE -> {
                    model.showEntry(entryRepository.getEntry(lotteryId, entrantId))
                    model.showError("This opportunity can no longer be declined.")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not decline this opportunity.")
        }
    }
}

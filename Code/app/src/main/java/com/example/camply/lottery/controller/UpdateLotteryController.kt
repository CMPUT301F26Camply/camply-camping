package com.example.camply.lottery.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.UpdateLotteryModel
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.lottery.repository.LotteryRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads and edits the owner's open lotteries. Firestore is saved first,
 * and the model only changes once the save succeeds.
 *
 * @param model the state the view is showing
 * @param lotteryRepository where lotteries are loaded from and saved to
 * @param campsiteRepository where the owner's campsites are loaded from
 */

class UpdateLotteryController (
    private val model: UpdateLotteryModel,
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository
    ) {
        /** @param ownerId the Firebase uid of the signed-in owner */
        suspend fun loadLotteries(ownerId: String) {
            model.showLoading()
            try {
                val open = lotteryRepository.getLotteriesByOwner(ownerId)
                    .filter { it.status == LotteryStatus.OPEN }
                    .sortedBy { it.registrationStart }
                model.showLotteries(open, campsiteRepository.getCampsitesByOwner(ownerId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                model.showError(e.message ?: "Could not load your lotteries.")
            }
        }

        /** @param lottery the lottery the owner wants to edit */
        fun onEdit(lottery: Lottery) = model.startEditing(lottery)

        fun onCancelEdit() = model.stopEditing()

        /**
         * Checks the edited values and saves them to the lottery.
         *
         * @param existing the lottery being edited
         * @return true if saved; otherwise the model holds the error
         */
        suspend fun updateLottery(
            existing: Lottery,
            registrationStart: String,
            registrationEnd: String,
            bookingStart: String,
            bookingEnd: String,
            lotteryStart: String,
            lotteryEnd: String,
            winnerCount: String
        ): Boolean {
            val regStart = registrationStart.trim()
            val regEnd = registrationEnd.trim()
            val bookStart = bookingStart.trim()
            val bookEnd = bookingEnd.trim()
            val stayStart = lotteryStart.trim()
            val stayEnd = lotteryEnd.trim()
            val winners = winnerCount.trim().toIntOrNull()

            val error = when {
                !isDateText(regStart) || !isDateText(regEnd) ->
                    "Enter the registration period as YYYY-MM-DD."
                !isDateText(stayStart) || !isDateText(stayEnd) ->
                    "Enter the lottery range as YYYY-MM-DD."
                stayEnd < stayStart ->
                    "Lottery end must be on or after the lottery start."
                regEnd < regStart ->
                    "Registration end must be on or after the start."
                !isDateText(bookStart) || !isDateText(bookEnd) ->
                    "Enter the booking period as YYYY-MM-DD."
                bookEnd < bookStart ->
                    "Booking end must be on or after the start."
                winners == null || winners < 1 -> "Winner count must be at least 1."
                else -> null
            }
            if (error != null) {
                model.showError(error)
                return false
            }

            val edited = existing.copy(
                registrationStart = regStart,
                registrationEnd = regEnd,
                bookingStart = bookStart,
                bookingEnd = bookEnd,
                lotteryStart = stayStart,
                lotteryEnd = stayEnd,
                numberOfWinners = winners!!
            )

            try {
                lotteryRepository.updateLottery(edited)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                model.showError(e.message ?: "Could not save your changes.")
                return false
            }

            model.showUpdated(edited)
            return true
        }

    private suspend fun overlapsAnotherLottery(
        existing: Lottery,
        registrationStart: String,
        registrationEnd: String
    ): Boolean {
        return try {
            lotteryRepository.getLotteries().any { other ->
                other.id != existing.id &&
                        other.status == LotteryStatus.OPEN &&
                        other.registrationStart == registrationStart &&
                        other.registrationEnd == registrationEnd &&
                        other.campsiteIds.any { it in existing.campsiteIds }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }
    }

        private fun isDateText(text: String): Boolean {
            val parts = text.split("-")
            if (parts.size != 3 || parts[0].length != 4) return false
            val year = parts[0].toIntOrNull() ?: return false
            val month = parts[1].toIntOrNull() ?: return false
            val day = parts[2].toIntOrNull() ?: return false
            if (month !in 1..12) return false
            val daysInMonth = when (month) {
                2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            return day in 1..daysInMonth
        }


    }
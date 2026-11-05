package com.example.camply.lottery.controller
import com.example.camply.lottery.repository.LotteryRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryCreateModel
import kotlin.coroutines.cancellation.CancellationException
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.campsite.model.CampsiteRepository

/**
 * Turns the create-lottery form into one saved lottery.
 *
 * Loads the owner's campsites into the model, checks the typed fields,
 * and asks the repository to write the document.
 * US 02.04.01, US 02.04.03, US 02.04.04, US 02.04.05.
 *
 * @param model the form state the view is showing
 * @param lotteryRepository where the finished lottery is saved
 * @param campsiteRepository where the owner's campsites are loaded from
 */
class LotteryCreateController(
    private val model: LotteryCreateModel,
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository
) {
    /**
     * Loads campsites owned by this user so the form can list them.
     *
     * @param ownerId the Firebase uid of the signed-in owner
     */
    suspend fun loadOwnedCampsites(ownerId: String) {
        try {
            model.showOwnedCampsites(campsiteRepository.getCampsitesByOwner(ownerId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load your campsites.")
        }
    }
    /** @param id the campsite the owner tapped */
    fun onCampsiteSelected(id: String) = model.selectCampsite(id)
    /** @param text the typed registration start, YYYY-MM-DD */
    fun onRegistrationStart(text: String) = model.updateRegistrationStart(text)
    /** @param text the typed registration end, YYYY-MM-DD */
    fun onRegistrationEnd(text: String) = model.updateRegistrationEnd(text)
    /** @param text the typed booking start, YYYY-MM-DD */
    fun onBookingStart(text: String) = model.updateBookingStart(text)
    /** @param text the typed booking end, YYYY-MM-DD */
    fun onBookingEnd(text: String) = model.updateBookingEnd(text)
    /** @param text the first stay date winners may reserve, YYYY-MM-DD */
    fun onStayFrom(text: String) = model.updateStayFrom(text)
    /** @param text the last stay date winners may reserve, YYYY-MM-DD */
    fun onStayTo(text: String) = model.updateStayTo(text)
    /** @param text the typed winner count. Digits only. */
    fun onWinnerCount(text: String) = model.updateWinnerCount(text)
    /** Checks the form and saves one lottery for this owner.
     *
     * On bad input, stores an error on the model and does not write.
     * On success, stores the new Firestore id on the model.
     *
     * @param ownerId the Firebase uid stored on the lottery
     */
    suspend fun createLottery(ownerId: String) {
        val campsiteId = model.selectedCampsiteId
        val registrationStart = model.registrationStart.trim()
        val registrationEnd = model.registrationEnd.trim()
        val bookingStart = model.bookingStart.trim()
        val bookingEnd = model.bookingEnd.trim()
        val winners = model.winnerCount.toIntOrNull()

        val error = when {
            campsiteId == null -> "Select one of your campsites."
            !isDateText(registrationStart) || !isDateText(registrationEnd) ->
                "Enter the registration period as YYYY-MM-DD."
            registrationEnd < registrationStart ->
                "Registration end must be on or after the start."
            !isDateText(bookingStart) || !isDateText(bookingEnd) ->
                "Enter the booking period as YYYY-MM-DD."
            bookingEnd < bookingStart ->
                "Booking end must be on or after the start."
            winners == null || winners < 1 -> "Winner count must be at least 1."
            else -> null
        }
        if (error != null) {
            model.showError(error)
            return
        }

        val lottery = Lottery(
            ownerId = ownerId,
            campsiteIds = listOf(campsiteId!!),
            registrationStart = registrationStart,
            registrationEnd = registrationEnd,
            bookingStart = bookingStart,
            bookingEnd = bookingEnd,
            numberOfWinners = winners!!,
            status = LotteryStatus.OPEN
        )

        try {
            model.showSaved(lotteryRepository.create(lottery))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not save the lottery.")
        }
    }
    /**
     * Checks that the text looks like YYYY-MM-DD.
     *
     * @param text the candidate date
     * @return true when the year, month, and day are in range
     */
    private fun isDateText(text: String): Boolean {
        val parts = text.split("-")
        if (parts.size != 3 || parts[0].length != 4) return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false
        return parts[0].toIntOrNull() != null && month in 1..12 && day in 1..31
    }

}
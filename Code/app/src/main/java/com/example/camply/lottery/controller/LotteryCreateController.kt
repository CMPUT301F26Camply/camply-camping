package com.example.camply.lottery.controller
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryCreateModel
import com.example.camply.lottery.model.LotteryRepository
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlin.coroutines.cancellation.CancellationException

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
        val registrationStart = parseDate(model.registrationStart)
        val registrationEnd = parseDate(model.registrationEnd)
        val bookingStart = parseDate(model.bookingStart)
        val bookingEnd = parseDate(model.bookingEnd)
        val stayFrom = model.stayFrom.trim()
        val stayTo = model.stayTo.trim()
        val winners = model.winnerCount.toIntOrNull()
        val error = when {
            campsiteId == null -> "Select one of your campsites."
            registrationStart == null || registrationEnd == null ->
                "Enter the registration period as YYYY-MM-DD."
            registrationEnd.seconds < registrationStart.seconds ->
                "Registration end must be on or after the start."
            bookingStart == null || bookingEnd == null ->
                "Enter the booking period as YYYY-MM-DD."
            bookingEnd.seconds < bookingStart.seconds ->
                "Booking end must be on or after the start."
            !isDateText(stayFrom) || !isDateText(stayTo) ->
                "Enter the stay dates as YYYY-MM-DD."
            stayFrom > stayTo -> "Stay end must be on or after the start."
            winners == null || winners < 1 -> "Winner count must be at least 1."
            else -> null
        }
        if (error != null) {
            model.showError(error)
            return
        }
        val lottery = Lottery(  
            ownerId = ownerId,
            campsiteIds = listOf(campsiteId),
            registrationStart = registrationStart,
            registrationEnd = registrationEnd,
            bookingStart = bookingStart,
            bookingEnd = bookingEnd,
            availableStayRanges = listOf(DateRange(stayFrom, stayTo)),
            winnerCount = winners
        )
        try{
            model.showSaved(lotteryRepository.create(lottery))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not save the lottery.")
        }
    }
    /**
     * Turns a YYYY-MM-DD string into a timestamp at midnight.
     *
     * @param text the date typed in the form
     * @return the timestamp, or null when the text is not a real date
     */
    private fun parseDate(text: String): Timestamp? {
        if (!isDateText(text.trim())) return null
        val parts = text.trim().split("-")
        val calendar = Calendar.getInstance()
        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
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
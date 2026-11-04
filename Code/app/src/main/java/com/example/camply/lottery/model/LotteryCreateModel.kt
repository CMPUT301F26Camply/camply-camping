package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Holds the create-lottery form state.
 *
 * The view reads these values. The controller updates them.
 * Nothing in this class talks to Firebase.
 */
class LotteryCreateModel {

    /** Campsites owned by the signed-in user. Empty until they are loaded. */
    var ownedCampsites: List<Campsite> by mutableStateOf(emptyList())
        private set

    /** Id of the one campsite the owner selected for this lottery. */
    var selectedCampsiteId: String? by mutableStateOf(null)
        private set

    /** Registration period, typed as YYYY-MM-DD. US 02.04.03. */
    var registrationStart: String by mutableStateOf("")
        private set
    var registrationEnd: String by mutableStateOf("")
        private set

    /** Booking period, typed as YYYY-MM-DD. US 02.04.04. */
    var bookingStart: String by mutableStateOf("")
        private set
    var bookingEnd: String by mutableStateOf("")
        private set

    /** Stay dates winners will be allowed to reserve. US 02.04.01. */
    var stayFrom: String by mutableStateOf("")
        private set
    var stayTo: String by mutableStateOf("")
        private set

    /** How many winners to draw. US 02.04.05. */
    var winnerCount: String by mutableStateOf("1")
        private set

    /** Message shown when the form is incomplete or the save fails. */
    var errorMessage: String? by mutableStateOf(null)
        private set

    /** Id returned after a successful save. Null until then. */
    var savedId: String? by mutableStateOf(null)
        private set

    /** @param list campsites belonging to the current owner */
    fun showOwnedCampsites(list: List<Campsite>) { ownedCampsites = list }

    /** @param id the campsite the owner chose */
    fun selectCampsite(id: String) { selectedCampsiteId = id; errorMessage = null }

    /** @param text the typed registration start date */
    fun updateRegistrationStart(text: String) { registrationStart = text }

    /** @param text the typed registration end date */
    fun updateRegistrationEnd(text: String) { registrationEnd = text }

    /** @param text the typed booking start date */
    fun updateBookingStart(text: String) { bookingStart = text }

    /** @param text the typed booking end date */
    fun updateBookingEnd(text: String) { bookingEnd = text }

    /** @param text the typed first stay date */
    fun updateStayFrom(text: String) { stayFrom = text }

    /** @param text the typed last stay date */
    fun updateStayTo(text: String) { stayTo = text }

    /**
     * Keeps only digits, so the winner count cannot contain letters.
     *
     * @param text whatever the owner just typed
     */
    fun updateWinnerCount(text: String) { winnerCount = text.filter(Char::isDigit) }

    /** @param message the reason the form cannot be saved */
    fun showError(message: String) { errorMessage = message }

    /** @param id the Firestore id of the lottery that was just created */
    fun showSaved(id: String) { savedId = id; errorMessage = null }
}
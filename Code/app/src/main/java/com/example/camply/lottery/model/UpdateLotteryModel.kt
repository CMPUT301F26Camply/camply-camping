package com.example.camply.lottery.model
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Holds the owner's open lotteries and the one being edited.
 */
class UpdateLotteryModel {
    var lotteries: List<Lottery> by mutableStateOf(emptyList())
    var ownedCampsites: List<Campsite> by mutableStateOf(emptyList())
    var editingLottery: Lottery? by mutableStateOf(null)
    var isLoading: Boolean by mutableStateOf(false)
    var errorMessage: String? by mutableStateOf(null)

    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * @param list the owner's open lotteries
     * @param campsites the owner's campsites, used to show names
     */
    fun showLotteries(list: List<Lottery>, campsites: List<Campsite>) {
        lotteries = list
        ownedCampsites = campsites
        isLoading = false
        errorMessage = null
    }

    /** @param message why the action failed */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }

    /** @param lottery the lottery whose form is opened */
    fun startEditing(lottery: Lottery) {
        editingLottery = lottery
        errorMessage = null
    }

    fun stopEditing() {
        editingLottery = null
        errorMessage = null
    }

    /**
     * Replaces the lottery in the list after Firestore saved it, and closes the form.
     *
     * @param edited the saved lottery
     */
    fun showUpdated(edited: Lottery) {
        lotteries = lotteries.map { if (it.id == edited.id) edited else it }
        editingLottery = null
        errorMessage = null
    }

}
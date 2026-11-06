package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange

/**
 * Stores the selected lottery and campsite for an entrant's booking.
 * US 01.04.01.
 */
class LotteryBookingModel {

    var lotteryId: String? by mutableStateOf(null)
        private set

    var campsite: Campsite? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var stayDates: DateRange? by mutableStateOf(null)
        private set

    /**
     * Clears the previous selection while the campsite is being loaded.
     */
    fun showLoading() {
        lotteryId = null
        campsite = null
        isLoading = true
        errorMessage = null
        stayDates = null
    }

    /**
     * Stores the selected lottery, campsite, and permitted stay dates.
     *
     * @param lottery The selected winning lottery.
     * @param selectedCampsite The campsite linked to the lottery.
     */
    fun showBooking(lottery: Lottery, selectedCampsite: Campsite) {
        lotteryId = lottery.id
        campsite = selectedCampsite
        stayDates = DateRange(lottery.lotteryStart, lottery.lotteryEnd)
        isLoading = false
        errorMessage = null
    }

    /**
     * Displays an error when the campsite cannot be loaded.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        lotteryId = null
        campsite = null
        isLoading = false
        errorMessage = message
        stayDates = null
    }

    /**
     * Clears the selected booking when returning to My Lotteries.
     */
    fun clearBooking() {
        lotteryId = null
        campsite = null
        isLoading = false
        errorMessage = null
        stayDates = null
    }
}

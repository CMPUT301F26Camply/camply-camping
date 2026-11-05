package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the state of the entrant's My Lotteries screen.
 *
 * Holds the entrant's joined lotteries, loading state, and error message.
 */
class EntrantLotteryModel {

    var lotteries: List<Lottery> by mutableStateOf(emptyList())
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model when lottery information is being loaded.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * Updates the model with the entrant's joined lotteries.
     *
     * @param list The lotteries the entrant has joined
     */
    fun showLotteries(list: List<Lottery>) {
        lotteries = list
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when lottery information can not be loaded
     *
     * @param message The error message to display
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }
}
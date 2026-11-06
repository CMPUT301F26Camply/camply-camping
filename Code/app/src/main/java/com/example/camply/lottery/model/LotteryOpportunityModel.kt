package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the state of one entrant's reservation opportunity for a lottery (US 01.04.03).
 */
class LotteryOpportunityModel {

    var entry: LotteryEntry? by mutableStateOf(null)
        private set

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    /**
     * Updates the model while the entry is being loaded or updated.
     */
    fun showLoading() {
        isLoading = true
        errorMessage = null
    }

    /**
     * Updates the model with the entrant's lottery entry.
     *
     * @param value The entry, or null if the entrant has no entry in the lottery.
     */
    fun showEntry(value: LotteryEntry?) {
        entry = value
        isLoading = false
        errorMessage = null
    }

    /**
     * Updates the model when an action on the opportunity does not succeed.
     *
     * @param message The error message to display.
     */
    fun showError(message: String) {
        isLoading = false
        errorMessage = message
    }
}

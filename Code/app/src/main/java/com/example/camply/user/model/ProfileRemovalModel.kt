package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Observable state for removing one profile (US 03.02.01).
 *
 * The controller is the only writer. The admin profile screen reads [error]
 * when a removal fails.
 */
class ProfileRemovalModel {
    var isRemoving by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Marks a removal as running and clears the previous error. */
    fun start() {
        isRemoving = true
        error = null
    }

    /** Marks a removal as finished. Keeps [error] empty on success. */
    fun finish() {
        isRemoving = false
    }

    /**
     * Stops the removal and stores the message shown to the admin.
     *
     * @param message Reason the removal did not finish.
     */
    fun fail(message: String) {
        isRemoving = false
        error = message
    }
}
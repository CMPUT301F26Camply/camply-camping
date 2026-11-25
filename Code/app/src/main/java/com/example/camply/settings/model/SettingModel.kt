package com.example.camply.settings.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// SettingsModel.kt - State of the entrant Settings screen (US 01.11.01).
// US 01.10.01 (notification preferences) will add its state here.
// Outstanding issues: none.

/**
 * Stores whether the policy and guidelines are currently displayed.
 */
class SettingsModel {

    var isPolicyVisible: Boolean by mutableStateOf(false)
        private set

    /** Shows the policy and guidelines instead of the settings options. */
    fun showPolicy() { isPolicyVisible = true }

    /** Returns from the policy to the settings options. */
    fun hidePolicy() { isPolicyVisible = false }
}
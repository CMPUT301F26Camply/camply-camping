package com.example.camply.settings.controller

import com.example.camply.settings.model.SettingsModel

// SettingsController.kt - Handles user actions on the Settings screen (US 01.11.01).
// Outstanding issues: none.

/**
 * Forwards Settings screen actions to [SettingsModel].
 *
 * @param model The model storing the screen state.
 */
class SettingsController(private val model: SettingsModel) {

    /** Opens the policy and guidelines. */
    fun onViewPolicyClicked() = model.showPolicy()

    /** Returns from the policy to the settings options. */
    fun onPolicyBack() = model.hidePolicy()
}
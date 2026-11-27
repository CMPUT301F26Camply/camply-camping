package com.example.camply.settings.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.settings.controller.SettingsController
import com.example.camply.settings.model.AppPolicy
import com.example.camply.settings.model.SettingsModel
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests viewing the app policy and guidelines from Settings (US 01.11.01). */
class SettingsViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var view: SettingsView
    private var wentBack = false

    @Before
    fun setUp() {
        val model = SettingsModel()
        view = SettingsView(model, SettingsController(model))
        compose.setContent { MaterialTheme { view.Display(onBack = { wentBack = true }) } }
    }

    @Test
    fun settings_showsViewPolicyButtonAndNoPolicyYet() {
        compose.onNodeWithText("View policy and guidelines").assertIsDisplayed()
        compose.onNodeWithText(AppPolicy.sections.first().body).assertDoesNotExist()
    }

    @Test
    fun clickingViewPolicy_showsPolicyContent() {
        compose.onNodeWithText("View policy and guidelines").performClick()

        compose.onNodeWithText("App policy").assertIsDisplayed()
        compose.onNodeWithText(AppPolicy.sections.first().body).assertIsDisplayed()
    }

    @Test
    fun policyBack_returnsToSettingsOptions() {
        compose.onNodeWithText("View policy and guidelines").performClick()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("View policy and guidelines").assertIsDisplayed()
    }

    @Test
    fun settingsBack_callsOnBack() {
        compose.onNodeWithText("Back").performClick()
        assertTrue(wentBack)
    }
}
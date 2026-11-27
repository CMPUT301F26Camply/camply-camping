package com.example.camply.settings

import com.example.camply.settings.controller.SettingsController
import com.example.camply.settings.model.AppPolicy
import com.example.camply.settings.model.SettingsModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests viewing the app policy and guidelines (US 01.11.01). */
class SettingsControllerTest {

    @Test
    fun policyIsHiddenByDefault() = assertFalse(SettingsModel().isPolicyVisible)

    @Test
    fun viewPolicyClicked_showsPolicy() {
        val model = SettingsModel()
        SettingsController(model).onViewPolicyClicked()
        assertTrue(model.isPolicyVisible)
    }

    @Test
    fun policyBack_hidesPolicy() {
        val model = SettingsModel()
        val controller = SettingsController(model)
        controller.onViewPolicyClicked()
        controller.onPolicyBack()
        assertFalse(model.isPolicyVisible)
    }

    @Test
    fun policyHasContentForEverySection() {
        assertTrue(AppPolicy.sections.isNotEmpty())
        assertTrue(AppPolicy.sections.all { it.title.isNotBlank() && it.body.isNotBlank() })
    }
}
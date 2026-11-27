package com.example.camply.user.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.user.controller.AdminProfileController
import com.example.camply.user.model.AdminProfileModel
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.Profile
import com.example.camply.user.model.User
import com.example.camply.user.repository.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Read-only profile browsing for an admin.
 *
 * Covering US 03.05.01
 */
class AdminProfileBrowseViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_showsNameAndRole() {
        show(profiles = listOf(jane()))

        composeRule.onNodeWithText("Browse profiles").assertIsDisplayed()
        composeRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeRule.onNodeWithText("Entrant").assertIsDisplayed()
    }

    @Test
    fun emptyList_showsEmptyMessage() {
        show(profiles = emptyList())

        composeRule.onNodeWithText("No profiles yet.").assertIsDisplayed()
    }

    @Test
    fun listBack_callsOnBackFromList() {
        var backedOut = false
        show(profiles = listOf(jane()), onBackFromList = { backedOut = true })

        composeRule.onNodeWithText("Back").performClick()

        assertEquals(true, backedOut)
    }

    @Test
    fun detail_showsInformationAndHidesEditing() {
        show(profiles = listOf(jane()), selected = jane())

        composeRule.onNodeWithText("Personal Information").assertIsDisplayed()
        composeRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeRule.onNodeWithText("jane@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("7802222222").assertIsDisplayed()
        composeRule.onNodeWithText("Entrant").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Default profile image").assertIsDisplayed()
        composeRule.onNodeWithText("Save Changes").assertDoesNotExist()
    }

    @Test
    fun detail_blankPhone_showsNotProvided() {
        val user = EntrantUser("entrant-2", Profile("John Doe", "john@example.com", ""))
        show(profiles = listOf(user), selected = user)

        composeRule.onNodeWithText("Not provided").assertIsDisplayed()
    }

    @Test
    fun detailBack_callsOnBack() {
        var backedOut = false
        show(profiles = listOf(jane()), selected = jane(), onBack = { backedOut = true })

        composeRule.onNodeWithText("Back").performClick()

        assertEquals(true, backedOut)
    }

    private fun show(
        profiles: List<User>,
        selected: User? = null,
        onBack: () -> Unit = {},
        onBackFromList: () -> Unit = {}
    ) {
        val model = AdminProfileModel()
        model.showProfiles(profiles)
        composeRule.setContent {
            AdminProfileBrowseView().Display(
                controller = AdminProfileController(model, UserRepository()),
                model = model,
                selectedProfile = selected,
                onProfileSelected = {},
                onBack = onBack,
                onBackFromList = onBackFromList
            )
        }
    }

    private fun jane() = EntrantUser("entrant-1", Profile("Jane Doe", "jane@example.com", "7802222222"))
}
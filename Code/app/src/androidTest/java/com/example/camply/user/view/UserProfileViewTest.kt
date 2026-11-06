package com.example.camply.user.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.Profile
import com.example.camply.user.model.UserModel
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for displaying and editing personal information (US 01.07.03).
 *
 * These tests check the Profile screen without saving changes to Firestore.
 */
class UserProfileViewTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Checks that the screen displays the user's existing profile information.
     */
    @Test
    fun display_existingProfile_showsInformation() {
        showScreen()

        compose.onNodeWithText("Personal Information").assertIsDisplayed()
        compose.onNodeWithText("Alex").assertIsDisplayed()
        compose.onNodeWithText("alex@example.com").assertIsDisplayed()
        compose.onNodeWithText("7801234567").assertIsDisplayed()
        compose.onNodeWithText("Save Changes").assertIsDisplayed()
    }

    /**
     * Checks that the name field allows the user to edit their information.
     */
    @Test
    fun nameField_editName_updatesDisplayedText() {
        showScreen()

        compose.onNodeWithText("Alex").performTextReplacement("Alex Smith")

        compose.onNodeWithText("Alex Smith").assertIsDisplayed()
    }

    /**
     * Checks that the email field allows the user to edit their information.
     */
    @Test
    fun emailField_editEmail_updatesDisplayedText() {
        showScreen()

        compose.onNodeWithText("alex@example.com")
            .performTextReplacement("alex.smith@example.com")

        compose.onNodeWithText("alex.smith@example.com").assertIsDisplayed()
    }

    /**
     * Checks that the phone number field allows the user to edit their information.
     */
    @Test
    fun phoneField_editPhoneNumber_updatesDisplayedText() {
        showScreen()

        compose.onNodeWithText("7801234567").performTextReplacement("7805551234")

        compose.onNodeWithText("7805551234").assertIsDisplayed()
    }

    /**
     * Displays the Profile view with predefined user information.
     */
    private fun showScreen() {
        val user = EntrantUser(
            "entrant1",
            Profile("Alex", "alex@example.com", "7801234567")
        )

        val controller = UserController(UserModel())
        val view = UserProfileView()

        compose.setContent { view.Display(user, controller) }
    }
}
package com.example.camply.user.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.Profile
import com.example.camply.user.model.UserModel
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for displaying and editing personal information (US 01.07.03)
 * and profile image management (US 01.08.02).
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
     * Checks that a user without a profile image sees the default
     * profile icon and an option to add a photo (US 01.08.02).
     */
    @Test
    fun display_noProfileImage_showsAddPhotoButton() {
        showScreen()

        compose.onNodeWithContentDescription("Default profile image")
            .assertIsDisplayed()

        compose.onNodeWithContentDescription("Add profile image")
            .assertIsDisplayed()
    }

    /**
     * Checks that photo management options are not shown
     * when the user has no profile image (US 01.08.02).
     */
    @Test
    fun display_noProfileImage_hidesPhotoManagementOptions() {
        showScreen()

        compose.onNodeWithContentDescription("Manage profile image")
            .assertDoesNotExist()

        compose.onNodeWithText("Change photo")
            .assertDoesNotExist()

        compose.onNodeWithText("Remove photo")
            .assertDoesNotExist()
    }

    /**
     * Checks that an image processing error is displayed
     * on the profile screen (US 01.08.02).
     */
    @Test
    fun display_imageProcessingError_showsErrorMessage() {
        val model = UserModel()
        model.showImageProcessingError()

        showScreen(model)

        compose.onNodeWithText(
            "Could not process image. Please try again or select another."
        ).assertExists()
    }

    /**
     * Checks that clearing the profile image error
     * removes the message from the screen (US 01.08.02).
     */
    @Test
    fun display_clearImageError_hidesErrorMessage() {
        val model = UserModel()
        model.showImageProcessingError()

        showScreen(model)

        compose.onNodeWithText(
            "Could not process image. Please try again or select another."
        ).assertExists()

        compose.runOnIdle {
            model.clearProfileImageStatus()
        }

        compose.onNodeWithText(
            "Could not process image. Please try again or select another."
        ).assertDoesNotExist()
    }

    /**
     * Displays the Profile view with predefined user information.
     *
     * @param model The user model used to provide profile screen state.
     */
    private fun showScreen(model: UserModel = UserModel()) {
        val user = EntrantUser(
            "entrant1",
            Profile("Alex", "alex@example.com", "7801234567")
        )

        val controller = UserController(model)
        val view = UserProfileView()

        compose.setContent { view.Display(user, controller) }
    }

}
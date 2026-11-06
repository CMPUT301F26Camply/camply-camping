package com.example.camply.user.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.User

/**
 * Displays and edits the current user's profile (US 01.07.03).
 */
class UserProfileView {


    /**
     * Displays the current profile and forwards changes to the controller.
     *
     * @param user The currently signed-in user.
     * @param userController The controller handling profile updates.
     */
    @Composable
    fun Display(user: User, userController: UserController) {
        val profile = user.getProfile()

        /**
         * Clears previous profile messages when leaving this screen, so they are not displayed
         * when the user returns.
         */
        DisposableEffect(user.getUid()) { onDispose { userController.clearProfileStatus() }
        }

        // Start with the user's existing profile information.
        var name by remember(user.getUid()) { mutableStateOf(profile.getName()) }
        var email by remember(user.getUid()) { mutableStateOf(profile.getEmail()) }
        var phoneNumber by remember(user.getUid()) { mutableStateOf(profile.getPhoneNumber()) }

        val profileError = userController.getProfileError()
        val isSavingProfile = userController.isSavingProfile()
        val profileSaved = userController.isProfileSaved()

        CamplyScreen("My Profile") {
            OrangeCard(modifier = Modifier.weight(1f)) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile heading
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CamplyPalette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Editable profile information
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                Spacer(Modifier.height(16.dp))

                if (profileError != null) {
                    ErrorMessage(profileError)
                }

                if (profileSaved) {
                    Text(
                        text = "Profile updated successfully.",
                        color = CamplyPalette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))

                PrimaryButton(
                    text = if (isSavingProfile) "Saving..." else "Save Changes",
                    onClick = {
                        userController.updateUserProfile(name, email, phoneNumber)
                    },
                    enabled = !isSavingProfile
                )
            }
        }
    }

}
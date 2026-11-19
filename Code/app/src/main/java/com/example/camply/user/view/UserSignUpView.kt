package com.example.camply.user.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CamplySelectionButton
import com.example.camply.shared.view.CamplyTextField
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.user.controller.UserController
import com.example.camply.R
import com.example.camply.shared.view.SecondaryButton

/**
 * Displays the signup screen for new Camply users.
 */
class UserSignUpView {

    /**
     * Displays the signup form and forwards registration requests.
     *
     * @param userController the controller handling registration actions
     */
    @Composable
    fun Display(userController: UserController) {

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("entrant") }


        CamplyScreen("Welcome to Camply!") {
            OrangeCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Welcome section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your outdoor adventure awaits!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Please create your profile:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Profile information
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CamplyTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full name",
                            placeholder = "Enter your full name"
                        )

                        CamplyTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email address",
                            placeholder = "you@example.com",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            )
                        )

                        CamplyTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = "Phone number",
                            placeholder = "Enter your phone number",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            )
                        )
                    }

                    // Role selection
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose your account role",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CamplySelectionButton(
                                text = "Explore & book",
                                subtitle = "Entrant",
                                iconRes = R.drawable.camp,
                                iconSize = 18.dp,
                                selected = role == "entrant",
                                onClick = { role = "entrant" },
                                modifier = Modifier.weight(1f)
                            )

                            CamplySelectionButton(
                                text = "Host campers",
                                subtitle = "Campsite Owner",
                                iconRes = R.drawable.map,
                                iconSize = 18.dp,
                                selected = role == "owner",
                                onClick = { role = "owner" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Registration errors
            val signupError = userController.getSignupError()

            if (signupError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ErrorMessage(signupError)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit registration
            PrimaryButton(
                text = "Sign Up",
                onClick = {
                    userController.registerNewUser(
                        name,
                        email,
                        phoneNumber,
                        role
                    )
                }
            )
        }
    }
}

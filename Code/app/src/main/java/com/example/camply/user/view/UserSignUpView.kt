package com.example.camply.user.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import com.example.camply.user.controller.UserController

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


        CamplyScreen("Create Your Camply Profile") {
            OrangeCard(modifier = Modifier.weight(1f)) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Welcome section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Welcome to Camply!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = CamplyPalette.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Please create your profile.",
                            color = CamplyPalette.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Profile information
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = {
                                Text(
                                    "Name",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center
                            ),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = {
                                Text(
                                    "Email",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center
                            ),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = {
                                Text(
                                    "Phone Number",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center
                            ),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Role selection
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose your account role",
                            style = MaterialTheme.typography.titleMedium,
                            color = CamplyPalette.Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (role == "entrant") {
                                PrimaryButton(
                                    text = "Entrant",
                                    onClick = { role = "entrant" },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                SecondaryButton(
                                    text = "Entrant",
                                    onClick = { role = "entrant" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (role == "owner") {
                                PrimaryButton(
                                    text = "Campsite Owner",
                                    onClick = { role = "owner" },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                SecondaryButton(
                                    text = "Campsite Owner",
                                    onClick = { role = "owner" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        PrimaryButton(
                            text = "Administrator (Coming Soon)",
                            onClick = {},
                            enabled = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val signupError = userController.getSignupError()

                if (signupError != null) {
                    ErrorMessage(signupError)
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Sign Up",
                    onClick = {
                        userController.registerNewUser(name, email, phoneNumber, role)
                    }
                )
            }
        }
    }
}
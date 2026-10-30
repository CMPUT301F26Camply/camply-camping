package com.example.camply.user.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

        Column(
            modifier = Modifier.fillMaxSize().padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Camply!")
            Text("Please create your profile")

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") }
            )

            Text("Account role: $role")

            Button(
                onClick = { role = "entrant" }
            ) {
                Text("Entrant")
            }

            Button(
                onClick = { role = "owner" }
            ) {
                Text("Campsite Owner")
            }

            Button(
                onClick = { userController.registerNewUser(name, email, phoneNumber, role) }
            ) {
                Text("Sign Up")
            }
        }
    }
}
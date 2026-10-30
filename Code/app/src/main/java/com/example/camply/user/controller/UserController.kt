package com.example.camply.user.controller

import com.example.camply.user.model.UserModel

/**
 * Handles user actions and forwards them to the user model
 *
 * @param userModel the model managing user information and registration
 */
class UserController(private val userModel: UserModel) {

    /**
     * Registers a new user using the provided profile information
     *
     * @param name the user's name
     * @param email the user's email address
     * @param phoneNumber the user's phone number
     * @param role the selected user role
     */
    fun registerNewUser(name: String, email: String, phoneNumber: String, role: String) {
        userModel.registerNewUser(name, email, phoneNumber, role)
    }
}

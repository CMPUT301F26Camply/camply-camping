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

    /**
     * Returns the signup validation error, if one exists.
     */
    fun getSignupError(): String? {
        return userModel.signupError
    }

    /**
     * Updates the current user's personal information.
     */
    fun updateUserProfile(name: String, email: String, phoneNumber: String) =
        userModel.updateUserProfile(name, email, phoneNumber)

    /**
     * Returns the error message from the last profile update attempt
     */
    fun getProfileError(): String? = userModel.profileError

    /**
     * Checks whether profile changes are being saved
     */
    fun isSavingProfile(): Boolean = userModel.isSavingProfile

    /**
     * Checks whether the profile was saved successfully
     */
    fun isProfileSaved(): Boolean = userModel.profileSaved

    /**
     * Clears previous profile update messages.
     */
    fun clearProfileStatus() = userModel.clearProfileStatus()
}

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


    // US 01.08.02: Profile image management

    /**
     * Loads the current user's profile image from Firestore.
     */
    fun loadProfileImage() = userModel.loadProfileImage()

    /**
     * Returns the currently loaded profile image.
     *
     * @return the image bytes, or null if no image exists
     */
    fun getProfileImageBytes(): ByteArray? = userModel.profileImageBytes

    /**
     * Checks whether the profile image is being loaded.
     *
     * @return true if the image is currently loading
     */
    fun isLoadingProfileImage(): Boolean = userModel.isLoadingProfileImage

    /**
     * Checks whether the profile image is being saved or removed.
     *
     * @return true if a save or removal is in progress
     */
    fun isSavingProfileImage(): Boolean = userModel.isSavingProfileImage

    /**
     * Saves or replaces the current user's profile image.
     *
     * @param imageBytes the image bytes to save
     */
    fun saveProfileImage(imageBytes: ByteArray) = userModel.saveProfileImage(imageBytes)

    /**
     * Removes the current user's profile image.
     */
    fun removeProfileImage() = userModel.removeProfileImage()

    /**
     * Returns the most recent profile image error.
     *
     * @return the error message, or null if no error exists
     */
    fun getProfileImageError(): String? = userModel.profileImageError

    /**
     * Returns the most recent profile image success message.
     *
     * @return the success message, or null if none exists
     */
    fun getProfileImageMessage(): String? = userModel.profileImageMessage

    /**
     * Reports an error when a selected profile image cannot be processed.
     */
    fun showImageProcessingError() = userModel.showImageProcessingError()

    /**
     * Clears previous profile image messages.
     */
    fun clearProfileImageStatus() = userModel.clearProfileImageStatus()

}

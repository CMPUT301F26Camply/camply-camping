
package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.user.repository.UserRepository
import com.example.camply.user.service.DeviceIdentityService

/**
 * Manages the current user's identity, profile, and startup state.
 *
 * Coordinates Firebase authentication and Firestore profile loading
 * to determine whether the user should sign up or enter the app.
 */
class UserModel {

    private val deviceIdentityService = DeviceIdentityService() // Handles Firebase device identity
    private val userRepository = UserRepository() // Handles user profile data in Firestore

    private var firebaseUid: String? = null // Firebase UID of the current user
    private var loadedUser: User? = null // Current user's loaded profile and account information
    private var currentStartupState by mutableStateOf("loading") // Tracks app startup and sign in state

    var signupError by mutableStateOf<String?>(null) // Error message shown during signup
        private set

    var profileError by mutableStateOf<String?>(null) // Error message shown when editing a profile
        private set

    var isSavingProfile by mutableStateOf(false) // True while profile changes are being saved to Firestore
        private set

    var profileSaved by mutableStateOf(false) // True after profile changes are saved successfully
        private set

    /**
     * Gets the Firebase UID associated with the current user.
     *
     * @return the user's UID, or null if it has not been retrieved
     */
    fun getUserId(): String? {
        return firebaseUid
    }

    /**
     * Gets the currently loaded Camply user.
     *
     * @return the current user, or null if no profile has been loaded
     */
    fun getCurrentUser(): User? {
        return loadedUser
    }

    /**
     * Gets the current startup state of the application.
     *
     * @return the current startup state
     */
    fun getStartupState(): String {
        return currentStartupState
    }

    // References:
    // Get data with Cloud Firestore
    // https://firebase.google.com/docs/firestore/query-data/get-data
    //
    // Firebase DocumentSnapshot Documentation
    // https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/DocumentSnapshot
    //
    // Used to understand how to check whether a Firestore document exists and retrieve its stored fields.

    /**
     * Checks whether a user profile exists in Firestore and loads it if found.
     *
     * Creates the appropriate user object based on the stored role
     * and updates the startup state depending on the result.
     *
     * @param uid the Firebase UID used to find the user's profile
     */
    private fun checkAndLoadUserProfile(uid: String) {

        // Store the UID so it is available if the user needs to sign up
        firebaseUid = uid

        userRepository.getUserProfile(
            uid,
            onSuccess = { document ->

                // Check whether the users profile exists in Firestore
                if (document.exists()) {

                    // Retrieve the users information from Firestore
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val phoneNumber = document.getString("phoneNumber")
                    val role = document.getString("role")

                    // Check that all the fields were retrieved
                    if (name != null && email != null && phoneNumber != null && role != null) {

                        val profile = Profile(name, email, phoneNumber)

                        // Create the user object based on their role
                        if (role == "entrant") {
                            loadedUser = EntrantUser(uid, profile)
                            currentStartupState = "ready"
                        } else if (role == "owner") {
                            loadedUser = CampsiteOwnerUser(uid, profile)
                            currentStartupState = "ready"
                        } else {
                            currentStartupState = "error"
                        }

                    } else {
                        currentStartupState = "error"
                    }

                } else {
                    // No profile exists so the user must complete signup
                    currentStartupState = "signup"
                }
            },
            onFailure = {
                // The Firestore request failed
                currentStartupState = "error"
            }
        )
    }

    /**
     * Initializes the user when Camply starts.
     *
     * Checks for an existing Firebase identity and signs in
     * anonymously when no authenticated user is available.
     */
    fun initializeUser() {
        currentStartupState = "loading"

        val uid = deviceIdentityService.getCurrentUserId()

        if (uid != null) {
            checkAndLoadUserProfile(uid)
        } else {
            deviceIdentityService.signInAnonymously { successful ->
                if (successful) {
                    val newUid = deviceIdentityService.getCurrentUserId()

                    if (newUid != null) {
                        checkAndLoadUserProfile(newUid)
                    } else {
                        currentStartupState = "error"
                    }
                } else {
                    currentStartupState = "error"
                }
            }
        }
    }


    /**
     * Registers a new Camply user with their profile information and role.
     *
     * Creates the user object and saves its information to Firestore.
     *
     * @param name the user's name
     * @param email the user's email address
     * @param phoneNumber the user's phone number
     * @param role the user's selected account role
     */
    fun registerNewUser(
        name: String,
        email: String,
        phoneNumber: String,
        role: String
    ) {
        val uid = firebaseUid

        // First we need to check that the user has a Firebase UID and needs to sign up
        if (uid == null || currentStartupState != "signup") {
            currentStartupState = "error"
            return
        }

        // Check the users profile information before creating a Profile
        if (name.isBlank() || email.isBlank() || !email.contains("@") || !email.contains(".") || phoneNumber.isBlank()) {
            signupError = null

            signupError = when {
                name.isBlank() -> "Please enter your name."
                email.isBlank() -> "Please enter your email address."
                !email.contains("@") || !email.contains(".") -> "Please enter a valid email address."
                phoneNumber.isBlank() -> "Please enter your phone number."
                else -> null
            }

            if (signupError != null) return
        }

        signupError = null

        val profile = Profile(name.trim(), email.trim(), phoneNumber.trim())

        // Then make the user object
        val user: User

        if (role == "entrant") {
            user = EntrantUser(uid, profile)
        } else if (role == "owner") {
            user = CampsiteOwnerUser(uid, profile)
        } else {
            return
        }

        currentStartupState = "loading"

        // Save the profile and update the application state
        userRepository.saveNewUserProfile(
            user,
            onSuccess = {
                loadedUser = user
                currentStartupState = "ready"
            },
            onFailure = {
                currentStartupState = "error"
            }
        )
    }

    /**
     * Validates and saves changes to the current user's profile.
     *
     * @param name The updated name.
     * @param email The updated email address.
     * @param phoneNumber The updated phone number.
     */
    fun updateUserProfile(name: String, email: String, phoneNumber: String) {
        val user = loadedUser ?: return
        if (isSavingProfile) return

        profileError = null
        profileSaved = false

        val currentProfile = user.getProfile()
        val updatedProfile = Profile(
            currentProfile.getName(),
            currentProfile.getEmail(),
            currentProfile.getPhoneNumber()
        )

        profileError = when {
            !updatedProfile.updateName(name) -> "Please enter your name."
            !updatedProfile.updateEmail(email) -> "Please enter a valid email address."
            !updatedProfile.updatePhoneNumber(phoneNumber) -> "Please enter your phone number."
            else -> null
        }

        if (profileError != null) return

        isSavingProfile = true

        userRepository.updateUserProfile(
            user.getUid(),
            updatedProfile.getName(),
            updatedProfile.getEmail(),
            updatedProfile.getPhoneNumber(),
            onSuccess = {
                currentProfile.updateName(updatedProfile.getName())
                currentProfile.updateEmail(updatedProfile.getEmail())
                currentProfile.updatePhoneNumber(updatedProfile.getPhoneNumber())

                isSavingProfile = false
                profileSaved = true
            },
            onFailure = {
                isSavingProfile = false
                profileError = "Could not update your profile. Please try again."
            }
        )
    }

    /**
     * Clears messages from previous profile update attempts.
     */
    fun clearProfileStatus() {
        profileError = null
        profileSaved = false
    }

}

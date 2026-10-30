
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

    private val deviceIdentityService = DeviceIdentityService()
    private val userRepository = UserRepository()

    private var firebaseUid: String? = null
    private var loadedUser: User? = null
    private var currentStartupState by mutableStateOf("loading")

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
            return
        }

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

}

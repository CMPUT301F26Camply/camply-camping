package com.example.camply.user.service


import com.google.firebase.auth.FirebaseAuth


// Reference: Authenticate with Firebase Anonymously on Android
// https://firebase.google.com/docs/auth/android/anonymous-auth
//
// Used to understand Firebase anonymous authentication, how to get
// the currently authenticated users UID, how to start anonymous sign in,
// and check whether authentication was successful.
//
// The document was used as a reference when implementing the DeviceIdentityService class.

/**
 * Handles user identification using Firebase Anonymous Authentication.
 *
 * Checks whether a user is already authenticated and allows the app
 * to sign in anonymously when no user exists.
 */
class DeviceIdentityService {

    private val auth = FirebaseAuth.getInstance()

    /**
     * Gets the UID of the currently authenticated Firebase user.
     *
     * @return the user's UID, or null if no user is signed in
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Signs in anonymously and report whether authentication succeeded or failed.
     *
     * @param onComplete function called with true on success or false on failure
     */
    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
}
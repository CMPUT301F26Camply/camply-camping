package com.example.camply.user.repository

import com.example.camply.user.model.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore


// References:
// Get data with Cloud Firestore
// https://firebase.google.com/docs/firestore/query-data/get-data
//
// Add data to Cloud Firestore
// https://firebase.google.com/docs/firestore/manage-data/add-data
//
// Used to understand how to retrieve and save documents in
// Cloud Firestore using get(), set(), and Firebase listeners.

/**
 * Handles retrieving and saving user profiles in Cloud Firestore.
 *
 * Provides methods for loading existing user documents and
 * storing new user profile information.
 */
class UserRepository {

    private val db = Firebase.firestore

    /**
     * Retrieves a user's profile document from Firestore.
     *
     * @param uid the Firebase UID identifying the user's document
     * @param onSuccess function called when the document is retrieved
     * @param onFailure function called if retrieving the document fails
     */
    fun getUserProfile(
        uid: String,
        onSuccess: (DocumentSnapshot) -> Unit,
        onFailure: () -> Unit
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                onSuccess(document)
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    /**
     * Saves a new user's profile information in Firestore.
     *
     * @param user the user whose profile will be saved
     * @param onSuccess function called when saving succeeds
     * @param onFailure function called if saving fails
     */
    fun saveNewUserProfile(
        user: User,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val profile = user.getProfile()

        val userData = hashMapOf(
            "name" to profile.getName(),
            "email" to profile.getEmail(),
            "phoneNumber" to profile.getPhoneNumber(),
            "role" to user.getRole()
        )

        db.collection("users").document(user.getUid()).set(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    /**
     * Updates an existing user's personal information in Firestore.
     *
     * @param uid The user's Firebase UID.
     * @param name The updated name.
     * @param email The updated email.
     * @param phoneNumber The updated phone number.
     * @param onSuccess Called when the update succeeds.
     * @param onFailure Called when the update fails.
     */
    fun updateUserProfile(
        uid: String,
        name: String,
        email: String,
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val profileData = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "phoneNumber" to phoneNumber
        )

        db.collection("users").document(uid).update(profileData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
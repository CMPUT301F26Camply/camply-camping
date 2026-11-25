package com.example.camply.user.repository

import androidx.compose.ui.semantics.Role
import com.example.camply.user.model.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.Blob
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


    // https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/Blob
    /**
     * Retrieves the current user's profile image from Firestore.
     *
     * @param uid the Firebase UID identifying the user's image document
     * @param onSuccess called with the image bytes, or null if no image exists
     * @param onFailure called if retrieving the image fails
     */
    fun getProfileImage(
        uid: String,
        onSuccess: (ByteArray?) -> Unit,
        onFailure: () -> Unit
    ) {
        db.collection("profileImages").document(uid).get()
            .addOnSuccessListener { document ->
                val imageBytes = document.getBlob("image")?.toBytes()
                onSuccess(imageBytes)
            }
            .addOnFailureListener { onFailure() }
    }

    //https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/Blob
    /**
     * Saves a  profile image in a separate Firestore document.
     *
     * @param uid the Firebase UID identifying the user's image document
     * @param imageBytes the compressed image bytes to save
     * @param onSuccess called after the image is saved
     * @param onFailure called if saving fails
     */
    fun saveProfileImage(
        uid: String,
        imageBytes: ByteArray,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val imageData = hashMapOf("image" to Blob.fromBytes(imageBytes))

        db.collection("profileImages").document(uid).set(imageData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }

    /**
     * Removes the user's profile image document from Firestore.
     *
     * @param uid the Firebase UID identifying the user's image document
     * @param onSuccess called after the image is removed
     * @param onFailure called if removal fails
     */
    fun removeProfileImage(
        uid: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        db.collection("profileImages").document(uid).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
    /**
     * Retrieves every user document whose role field matches the requested role.
     *
     * @param role the Firestore role to match, such as "owner"
     * @param onSuccess called with the matching user documents
     * @param onFailure called if the query fails
     */
    fun getUsersByRole(
        role: String,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: () -> Unit
    ){
        db.collection("users").whereEqualTo("role",role).get()
            .addOnSuccessListener { onSuccess(it.documents) }
            .addOnFailureListener { onFailure() }
    }
}
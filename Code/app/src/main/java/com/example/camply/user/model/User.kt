package com.example.camply.user.model

/**
 * Represents a Camply user with a Firebase identity and profile.
 *
 * This abstract class stores the information shared by all
 * types of Camply users.
 *
 * @param uid the user's Firebase Authentication UID
 * @param profile the user's personal profile information
 */
abstract class User(
    private val uid: String,
    private val profile: Profile
) {

    /**
     * Returns the user's Firebase UID.
     *
     * @return the user's UID
     */
    fun getUid(): String {
        return uid
    }

    /**
     * Returns the user's profile information.
     *
     * @return the user's Profile object
     */
    fun getProfile(): Profile {
        return profile
    }

    /**
     * Returns the user's account role.
     *
     * @return the user's account role
     */
    abstract fun getRole(): String
}
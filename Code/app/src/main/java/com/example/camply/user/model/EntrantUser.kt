package com.example.camply.user.model

/**
 * Represents a Camply user registered as an entrant.
 *
 * Inherits shared user information from User and implements
 * the Entrant interface for entrant related functionality.
 *
 * @param uid the user's Firebase Authentication UID
 * @param profile the user's personal profile information
 */
class EntrantUser(
    uid: String,
    profile: Profile
) : User(uid, profile), Entrant {

    /**
     * Returns the entrant account role.
     *
     * @return "entrant"
     */
    override fun getRole(): String {
        return "entrant"
    }

}
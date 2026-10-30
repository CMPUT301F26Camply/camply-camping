package com.example.camply.user.model

/**
 * Represents a Camply user registered as a campsite owner.
 *
 * Inherits shared user information from User and implements
 * the CampsiteOwner interface for owner related functionality.
 *
 * @param uid the user's Firebase Authentication UID
 * @param profile the user's personal profile information
 */
class CampsiteOwnerUser(
    uid: String,
    profile: Profile
) : User(uid, profile), CampsiteOwner {

    /**
     * Returns the campsite owner account role.
     *
     * @return "owner"
     */
    override fun getRole(): String {
        return "owner"
    }

}
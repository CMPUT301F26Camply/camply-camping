package com.example.camply.user.model

/**
 * Represents a Camply user registered as an administrator.
 *
 * Inherits shared user information from User. An admin account is created
 * by setting the Firestore users document role to "admin".
 *
 * @param uid the user's Firebase Authentication UID
 * @param profile the user's personal profile information
 *
 * Covering US 03.11.01
 */
class AdminUser (
    uid:String,
    profile: Profile): User(uid,profile){
    /**
     * Returns the administrator account role.
     *
     * @return "admin"
     */
    override fun getRole(): String {
        return "admin"
    }
}
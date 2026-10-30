package com.example.camply.user.model

/**
 * Stores the personal profile information associated with a Camply user.
 *
 * @param name the user's full name
 * @param email the user's email address
 * @param phoneNumber the user's phone number
 */
class Profile(
    private var name: String,
    private var email: String,
    private var phoneNumber: String
) {

    /**
     * Returns the user's full name.
     *
     * @return the user's full name
     */
    fun getName(): String {
        return name
    }

    /**
     * Returns the user's email address.
     *
     * @return the user's email address
     */
    fun getEmail(): String {
        return email
    }

    /**
     * Returns the user's phone number.
     *
     * @return the user's phone number
     */
    fun getPhoneNumber(): String {
        return phoneNumber
    }

    /**
     * Updates the user's full name if it is valid.
     *
     * @param newName the new full name
     * @return true if updated, false if invalid
     */
    fun updateName(newName: String): Boolean {
        if (newName.isBlank()) {
            return false
        }

        name = newName.trim()
        return true
    }

    /**
     * Updates the user's email address if it is valid.
     *
     * @param newEmail the new email address
     * @return true if updated, false if invalid
     */
    fun updateEmail(newEmail: String): Boolean {
        if (newEmail.isBlank() || !newEmail.contains("@") || !newEmail.contains(".")) {
            return false
        }

        email = newEmail.trim()
        return true
    }

    /**
     * Updates the user's phone number if it is valid.
     *
     * @param newPhoneNumber the new phone number
     * @return true if updated, false if invalid
     */
    fun updatePhoneNumber(newPhoneNumber: String): Boolean {
        if (newPhoneNumber.isBlank()) {
            return false
        }

        phoneNumber = newPhoneNumber.trim()
        return true
    }
}

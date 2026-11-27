package com.example.camply.user.controller

import com.example.camply.user.model.AdminProfileModel
import com.example.camply.user.repository.UserRepository
import kotlinx.coroutines.CancellationException

/**
 * Loads every user profile an admin can browse, and the photo for the profile they open.
 *
 * Covering US 03.05.01
 */
class AdminProfileController(
    private val model: AdminProfileModel,
    private val userRepository: UserRepository
) {
    private var requestedProfileId: String? = null

    /**
     * Loads every entrant, owner, and admin.
     *
     * A failed query leaves the list empty.
     */
    fun loadProfiles() {
        userRepository.getAllUsers(
            onSuccess = { users -> model.showProfiles(users) },
            onFailure = { model.showProfiles(emptyList()) }
        )
    }
    /**
     * Loads the photo for one profile.
     *
     * The previous photo is cleared first. A response for an older profile is ignored.
     *
     * @param uid Firebase UID of the profile whose photo should be loaded.
     */
    fun loadProfileImage(uid: String) {
        requestedProfileId = uid
        model.showProfileImage(null)
        model.showLoadingImage(true)
        userRepository.getProfileImage(
            uid = uid,
            onSuccess = { bytes ->
                if (requestedProfileId != uid) return@getProfileImage
                model.showProfileImage(bytes)
                model.showLoadingImage(false)
            },
            onFailure = {
                if (requestedProfileId != uid) return@getProfileImage
                model.showProfileImage(null)
                model.showLoadingImage(false)
            }
        )
    }
}
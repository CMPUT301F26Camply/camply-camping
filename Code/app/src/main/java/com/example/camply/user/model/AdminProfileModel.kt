package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores the profiles shown while an admin browses users.
 *
 * Covering US 03.05.01
 */
class AdminProfileModel {
    var profiles by mutableStateOf<List<User>>(emptyList())
        private set

    var profileImage by mutableStateOf<ByteArray?>(null)
        private set

    var isLoadingImage by mutableStateOf(false)
        private set

    fun showProfiles(list: List<User>) {
        profiles = list
    }

    fun showProfileImage(bytes: ByteArray?) {
        profileImage = bytes
    }

    fun showLoadingImage(loading: Boolean) {
        isLoadingImage = loading
    }
}
package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * Stores the owners and campsites shown while an admin browses campsites.
 *
 */
class AdminModel {
    var owners by mutableStateOf<List<User>>(emptyList())
        private set

    var campsiteCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var ownerCampsites by mutableStateOf<List<Campsite>>(emptyList())
        private set

    var isLoadingOwnerCampsites by mutableStateOf(false)
        private set
    /** Replaces the owner list. */
    fun showOwners(list: List<User>) {
        owners = list
    }

    /** Replaces the campsite count for each owner id. */
    fun showCampsiteCounts(counts: Map<String, Int>) {
        campsiteCounts = counts
    }

    /** Replaces the campsites for the owner the admin selected. */
    fun showOwnerCampsites(list: List<Campsite>) {
        ownerCampsites = list
    }
    /** Records whether the selected owner's campsites are still loading. */
    fun showLoadingOwnerCampsites(loading: Boolean) {
        isLoadingOwnerCampsites = loading
    }
}
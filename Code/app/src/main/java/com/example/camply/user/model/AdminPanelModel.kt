package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
/**
 * The admin tool currently open from the panel.
 *
 */
enum class AdminPanelPage { CAMPSITES, PROFILES }
/**
 * Stores which admin tool is open and which owner, campsite, or profile is selected.
 *
 */
class AdminPanelModel {
    var page by mutableStateOf<AdminPanelPage?>(null)
        private set
    var selectedOwner by mutableStateOf<User?>(null)
        private set
    var detailCampsiteId by mutableStateOf<String?>(null)
        private set
    var selectedProfile by mutableStateOf<User?>(null)
        private set

    fun showPage(page: AdminPanelPage?) { this.page = page }
    fun showOwner(owner: User?) { selectedOwner = owner }
    fun showCampsite(campsiteId: String?) { detailCampsiteId = campsiteId }
    fun showProfile(profile: User?) { selectedProfile = profile }
}
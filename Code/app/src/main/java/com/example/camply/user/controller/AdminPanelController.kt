package com.example.camply.user.controller

import com.example.camply.user.model.AdminPanelModel
import com.example.camply.user.model.AdminPanelPage
import com.example.camply.user.model.User
/**
 * Moves the admin between the panel menu, campsite browsing, and profile browsing.
 *
 * This controller only updates [AdminPanelModel]. Loading stays in
 * [AdminController] and [AdminProfileController].
 *
 */
class AdminPanelController(private val model: AdminPanelModel) {
    fun openCampsites() {
        model.showOwner(null)
        model.showCampsite(null)
        model.showPage(AdminPanelPage.CAMPSITES)
    }
    fun openProfiles() {
        model.showProfile(null)
        model.showPage(AdminPanelPage.PROFILES)
    }
    fun backToMenu() { model.showPage(null) }
    fun selectOwner(owner: User) { model.showOwner(owner) }
    fun backFromCampsites() { model.showOwner(null) }
    fun selectCampsite(campsiteId: String) { model.showCampsite(campsiteId) }
    fun backFromDetails() { model.showCampsite(null) }
    fun selectProfile(profile: User) { model.showProfile(profile) }
    fun backFromProfile() { model.showProfile(null) }
}
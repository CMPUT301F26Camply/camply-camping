package com.example.camply.user.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.view.CampsiteDetailView
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.OrangeCard
import com.example.camply.user.controller.AdminController
import com.example.camply.user.controller.AdminImageBrowseController
import com.example.camply.user.controller.AdminPanelController
import com.example.camply.user.controller.AdminProfileController
import com.example.camply.user.model.AdminImageBrowseModel
import com.example.camply.user.model.AdminModel
import com.example.camply.user.model.AdminPanelModel
import com.example.camply.user.model.AdminPanelPage
import com.example.camply.user.model.AdminProfileModel
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.User

/*
 * AI Assistance:
 * Tool: Cursor (Grok 4.7)
 * Date: 2026-11-26
 * Purpose: Assisted with creating the admin panel view. The view shows
 * a menu of admin tools and opens the existing campsite browse and
 * profile browse screens. Back from the first page of either tool
 * returns to the menu.
 *
 * Prompt: "Instead of having all the admin functionality occupying its
 * own place in the nav bar, we would have an admin panel that will have
 * all the admin functionality. The browse campsite functionality and
 * browse profile functionality are already good."
 *
 * The generated view was reviewed and adjusted before it was kept.
 * The title, button wording, and visible Back buttons were changed
 * during that review.
 */
class AdminPanelView {

    /**
     * Displays the page stored in [model].
     *

     * @param controller Updates the open page and the current selection.
     * @param model The open page and the selected owner, campsite, or profile.
     * @param campsiteController Loads owners and one owner's campsites.
     * @param campsiteModel Owners, counts, and the selected owner's campsites.
     * @param campsiteBrowseView Existing read-only campsite browse screen.
     * @param profileController Loads profiles and one profile photo.
     * @param profileModel Profiles and the open profile's photo.
     * @param profileBrowseView Existing read-only profile browse screen.
     * @param imageBrowseController Loads uploaded images and handles image browsing actions.
     * @param imageBrowseModel Stores image browsing data, selections, search queries and counts.
     * @param imageBrowseView Displays the uploaded image browsing screens.
     * @param detailView Existing campsite details screen.
     * @param currentUserId Signed-in admin id. Details uses it to keep editing hidden.
     * @param loadPhotoPreview Loads a small campsite photo.
     * @param loadPhoto Loads a full campsite photo.
     * @param onRemoveCampsitePhoto Removes a promotional photo after the admin confirms.
     * @param onRemoveProfileImage Removes a profile image after the admin confirms.
     */
    @Composable
    fun Display(
        controller: AdminPanelController,
        model: AdminPanelModel,
        campsiteController: AdminController,
        campsiteModel: AdminModel,
        campsiteBrowseView: AdminCampsiteBrowseView,
        profileController: AdminProfileController,
        profileModel: AdminProfileModel,
        profileBrowseView: AdminProfileBrowseView,
        imageBrowseController: AdminImageBrowseController,
        imageBrowseModel: AdminImageBrowseModel,
        imageBrowseView: AdminImageBrowseView,
        detailView: CampsiteDetailView,
        currentUserId: String,
        loadPhotoPreview: suspend (String, String) -> ByteArray?,
        loadPhoto: suspend (String, String) -> ByteArray?,
        onRemoveCampsitePhoto: suspend (Campsite, String) -> ImageRemovalResult,
        onRemoveProfileImage: suspend (User) -> ImageRemovalResult
    ) {
        when (model.page) {
            null -> {
                CamplyScreen(title = "Admin Functionality") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OrangeCard(Modifier.clickable { controller.openCampsites() }) {
                            Text("Browse all campsites", style = MaterialTheme.typography.titleMedium)
                        }

                        OrangeCard(Modifier.clickable { controller.openProfiles() }) {
                            Text("Browse all profiles", style = MaterialTheme.typography.titleMedium)
                        }

                        OrangeCard(Modifier.clickable { controller.openImages() }) {
                            Text("Browse uploaded images", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            AdminPanelPage.PROFILES -> {
                BackHandler(enabled = model.selectedProfile == null) {
                    controller.backToMenu()
                }
                profileBrowseView.Display(
                    controller = profileController,
                    model = profileModel,
                    selectedProfile = model.selectedProfile,
                    onProfileSelected = { controller.selectProfile(it) },
                    onBack = { controller.backFromProfile() },
                    onBackFromList = { controller.backToMenu() }

                )
            }

            AdminPanelPage.CAMPSITES -> {
                BackHandler(enabled = model.selectedOwner == null && model.detailCampsiteId == null) {
                    controller.backToMenu()
                }
                campsiteBrowseView.Display(
                    controller = campsiteController,
                    model = campsiteModel,
                    detailView = detailView,
                    currentUserId = currentUserId,
                    selectedOwner = model.selectedOwner,
                    detailCampsiteId = model.detailCampsiteId,
                    onOwnerSelected = { controller.selectOwner(it) },
                    onCampsiteSelected = { controller.selectCampsite(it) },
                    onBackFromCampsites = { controller.backFromCampsites() },
                    onBackFromDetails = { controller.backFromDetails() },
                    loadPhotoPreview = loadPhotoPreview,
                    loadPhoto = loadPhoto,
                    onBackFromOwners = { controller.backToMenu() }
                )
            }

            AdminPanelPage.IMAGES -> {
                imageBrowseView.Display(
                    controller = imageBrowseController,
                    model = imageBrowseModel,
                    onBack = { controller.backToMenu() },
                    onRemoveCampsitePhoto = onRemoveCampsitePhoto,
                    onRemoveProfileImage = onRemoveProfileImage
                )
            }
        }
    }
}
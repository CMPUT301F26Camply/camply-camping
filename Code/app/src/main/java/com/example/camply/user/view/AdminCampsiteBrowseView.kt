package com.example.camply.user.view

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.camply.campsite.view.CampsiteDetailView
import com.example.camply.user.controller.AdminController
import com.example.camply.user.model.AdminModel
import com.example.camply.user.model.User
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
/**
 * Shows the admin campsite browse pages: all owners, one owner's campsites, then campsite details.
 *
 * Covering US 03.04.01
 */
class AdminCampsiteBrowseView {
    private val ownerListView = AdminOwnerListView()
    private val campsiteListView = CampsiteListView()

    /**
     * Displays the page that matches the admin's current selection.
     *
     * @param controller Holds the loaded owners, counts, and selected owner's campsites.
     * @param detailView Existing campsite details screen.
     * @param currentUserId Signed-in admin id. Details uses it to keep editing hidden.
     * @param selectedOwner Owner whose campsites are open, or null on the owner list.
     * @param detailCampsiteId Campsite open on the details page, or null.
     * @param onOwnerSelected Stores the owner chosen from the list.
     * @param onCampsiteSelected Stores the campsite chosen from an owner's list.
     * @param onBackFromCampsites Returns from one owner's campsites to the owner list.
     * @param onBackFromDetails Returns from campsite details to that owner's campsites.
     * @param loadPhotoPreview Loads a small campsite photo.
     * @param loadPhoto Loads a full campsite photo.
     */
    @Composable
    fun Display(
        controller: AdminController,
        model: AdminModel,
        detailView: CampsiteDetailView,
        currentUserId: String,
        selectedOwner: User?,
        detailCampsiteId: String?,
        onOwnerSelected: (User) -> Unit,
        onCampsiteSelected: (String) -> Unit,
        onBackFromCampsites: () -> Unit,
        onBackFromDetails: () -> Unit,
        loadPhotoPreview: suspend (String, String) -> ByteArray?,
        loadPhoto: suspend (String, String) -> ByteArray?,
        onBackFromOwners: () -> Unit
    ) {
        val scope = rememberCoroutineScope()

        when {
            detailCampsiteId != null -> {
                BackHandler { onBackFromDetails() }
                detailView.Display(
                    campsiteId = detailCampsiteId,
                    onBack = onBackFromDetails,
                    currentUserId = currentUserId,
                    onEdit = null,
                    loadPhotoPreview = loadPhotoPreview,
                    loadPhoto = loadPhoto
                )
            }
            selectedOwner != null -> {
                BackHandler { onBackFromCampsites() }
                campsiteListView.Display(
                    ownerName = selectedOwner.getProfile().getName(),
                    campsites = model.ownerCampsites,
                    readOnly = true,
                    addCampsite = {},
                    editCampsite = {},
                    viewCampsite = { onCampsiteSelected(it.id) },
                    lotteryRegistration = {},
                    reservationList = {},
                    viewOrCreateLottery = {},
                    loadPhotoPreview = loadPhotoPreview,
                    onBack = onBackFromCampsites
                )
            }
            else -> {
                ownerListView.Display(
                    owners = model.owners,
                    campsiteCounts = model.campsiteCounts,
                    onOwnerClick = { owner ->
                        onOwnerSelected(owner)
                        scope.launch {
                            controller.loadCampsitesFor(owner.getUid())
                        }
                    },
                    onBack = onBackFromOwners
                )
            }
        }
    }
}
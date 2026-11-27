package com.example.camply.user.view

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camply.R
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.user.controller.AdminProfileController
import com.example.camply.user.model.AdminProfileModel
import com.example.camply.user.model.User

/**
 * Shows every user profile, then the read-only details for the one the admin opens.
 *
 * Covering US 03.05.01
 */
class AdminProfileBrowseView {

    /**
     *
     * @param controller Loads the photo when a profile is selected.
     * @param model Profiles, the open photo, and whether that photo is loading.
     * @param selectedProfile Profile open on the detail page, or null on the list.
     * @param onProfileSelected Stores the profile chosen from the list.
     * @param onBack Returns from one profile's details to the list.
     * @param onBackFromList Returns from the profile list to the admin menu.
     */
    @Composable
    fun Display(
        controller: AdminProfileController,
        model: AdminProfileModel,
        selectedProfile: User?,
        onProfileSelected: (User) -> Unit,
        onBack: () -> Unit,
        onBackFromList: () -> Unit
    ) {
        if (selectedProfile != null) {
            BackHandler { onBack() }
            ProfileDetail(
                profile = selectedProfile,
                imageBytes = model.profileImage,
                isLoadingImage = model.isLoadingImage,
                onBack = onBack
            )
        } else {
            ProfileList(
                profiles = model.profiles,
                onProfileSelected = { profile ->
                    onProfileSelected(profile)
                    controller.loadProfileImage(profile.getUid())
                },
                onBack=onBackFromList
            )
        }
    }

    @Composable
    private fun ProfileList(
        profiles: List<User>,
        onProfileSelected: (User) -> Unit,
        onBack: () -> Unit
    ) {
        CamplyScreen(title = "Browse profiles") {
            if (profiles.isEmpty()) {
                Text(
                    text = "No profiles yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    profiles.forEach { profile ->
                        OrangeCard(modifier = Modifier.clickable { onProfileSelected(profile) }) {
                            CardLabel("Profile")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = profile.getProfile().getName(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = roleLabel(profile.getRole()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Back", onClick = onBack)
        }
    }

    @Composable
    private fun ProfileDetail(
        profile: User,
        imageBytes: ByteArray?,
        isLoadingImage: Boolean,
        onBack: () -> Unit
    ) {
        val bitmap = remember(imageBytes) {
            imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }

        CamplyScreen(title = "Profile") {
            OrangeCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Profile image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.profile_icon),
                                contentDescription = "Default profile image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    if (isLoadingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    InfoLine("Name", profile.getProfile().getName())
                    InfoLine("Email", profile.getProfile().getEmail())
                    InfoLine("Phone number", profile.getProfile().getPhoneNumber())
                    InfoLine("Role", roleLabel(profile.getRole()))
                }
            }

            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Back", onClick = onBack)
        }
    }

    @Composable
    private fun InfoLine(label: String, value: String) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value.ifBlank { "Not provided" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    private fun roleLabel(role: String): String {
        return role.replaceFirstChar { it.uppercase() }
    }
}
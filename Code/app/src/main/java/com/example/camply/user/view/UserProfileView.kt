package com.example.camply.user.view

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.R
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.User
import com.example.camply.user.service.ProfileImageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Displays and edits the current user's profile and profile image.
 *
 * Supports personal information updates (US 01.07.03) and profile image management (US 01.08.02).
 */
class UserProfileView {

    /**
     * Displays the current profile and forwards changes to the controller.
     *
     * @param user The currently signed-in user.
     * @param userController The controller handling profile updates.
     */
    @Composable
    fun Display(user: User, userController: UserController) {
        val profile = user.getProfile()


        // Clears previous profile messages when leaving this screen, so they are not displayed when the user returns.
        DisposableEffect(user.getUid()) {
            onDispose {
                userController.clearProfileStatus()
                userController.clearProfileImageStatus()
            }
        }

        LaunchedEffect(user.getUid()) { userController.loadProfileImage() }

        // Start with the user's existing profile information.
        var name by remember(user.getUid()) { mutableStateOf(profile.getName()) }
        var email by remember(user.getUid()) { mutableStateOf(profile.getEmail()) }
        var phoneNumber by remember(user.getUid()) { mutableStateOf(profile.getPhoneNumber()) }

        val profileError = userController.getProfileError()
        val isSavingProfile = userController.isSavingProfile()
        val profileSaved = userController.isProfileSaved()


        // US 01.08.02: Profile image state and picker
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val imageService = remember { ProfileImageService() }
        var isProcessingImage by remember { mutableStateOf(false) }
        var showPhotoMenu by remember { mutableStateOf(false) }

        val imageBytes = userController.getProfileImageBytes()
        val isLoadingProfileImage = userController.isLoadingProfileImage()
        val isSavingProfileImage = userController.isSavingProfileImage()
        val profileImageError = userController.getProfileImageError()
        val profileImageMessage = userController.getProfileImageMessage()

        // Convert the saved image bytes into a Bitmap for Compose.
        val profileImageBitmap = remember(imageBytes) {
            imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }

        // Open Androids photo picker and process the selected image.
        //
        // AI Usage Declaration
        //
        // Contributor: Filip Kosmajac (fkosmajac)
        // AI Provider: OpenAI
        // Tool: ChatGPT 6
        // Date Used: November 20, 2026
        //
        // Prompt:
        // "In Kotlin with Jetpack Compose, how can I open Android's photo picker and
        // handle the selected image? I have a image service with a prepareImage(context, uri)
        // function that processes the image into a ByteArray"
        //
        // Purpose:
        // Used ChatGPT to understand and implement Android's photo picker and connect
        // it to the existing profile image processing service.
        //
        // Extent of Use:
        // Assistance with setting up the Android photo picker, and handling the selected image URI.
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    isProcessingImage = true

                    try {
                        // Process the image without blocking the UI thread.
                        val processedImage = withContext(Dispatchers.IO) {
                            imageService.prepareImage(context, uri)
                        }

                        if (processedImage != null) {
                            userController.saveProfileImage(processedImage)
                        } else {
                            userController.showImageProcessingError()
                        }
                    } finally {
                        isProcessingImage = false
                     }
                }
           }
        }

        val isImageBusy = isLoadingProfileImage || isSavingProfileImage || isProcessingImage

        CamplyScreen("My Profile") {
            OrangeCard(modifier = Modifier.weight(1f)) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Profile image with an add or edit button.
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageBitmap != null) {
                                Image(
                                    bitmap = profileImageBitmap,
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


                        // Add/edit button with photo management menu
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            IconButton(
                                onClick = {
                                    if (imageBytes == null) {
                                        // No photo yet so open the picker directly.
                                        imagePicker.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    } else {
                                        // Show options for an existing photo.
                                        showPhotoMenu = true
                                    }
                                },
                                enabled = !isImageBusy,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (imageBytes == null) {
                                        Icons.Default.Add
                                    } else {
                                        Icons.Default.Edit
                                    },
                                    contentDescription = if (imageBytes == null) {
                                        "Add profile image"
                                    } else {
                                        "Manage profile image"
                                    },
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showPhotoMenu,
                                onDismissRequest = { showPhotoMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change photo") },
                                    onClick = {
                                        showPhotoMenu = false
                                        imagePicker.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Remove photo",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showPhotoMenu = false
                                        userController.removeProfileImage()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }

                    }

                    // Show progress while loading, processing, or saving the image.
                    if (isImageBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    // Display image operation errors and success messages.
                    if (profileImageError != null) {
                        ErrorMessage(profileImageError)
                    }

                    if (profileImageMessage != null) {
                        Text(
                            text = profileImageMessage,
                            color = CamplyPalette.Ink,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Profile heading
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CamplyPalette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Editable profile information
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number", fontSize = 16.sp) },
                            textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp),
                            colors = camplyTextFieldColors(),
                            singleLine = true,
                            enabled = !isSavingProfile,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                Spacer(Modifier.height(16.dp))

                if (profileError != null) {
                    ErrorMessage(profileError)
                }

                if (profileSaved) {
                    Text(
                        text = "Profile updated successfully.",
                        color = CamplyPalette.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))

                PrimaryButton(
                    text = if (isSavingProfile) "Saving..." else "Save Changes",
                    onClick = {
                        userController.updateUserProfile(name, email, phoneNumber)
                    },
                    enabled = !isSavingProfile
                )
            }
        }
    }

}
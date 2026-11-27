package com.example.camply.user.view

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.view.CampsitePhotoGallery
import com.example.camply.campsite.view.SingleImageViewer
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CamplyTextField
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.user.controller.AdminImageBrowseController
import com.example.camply.user.model.AdminImageBrowseModel
import com.example.camply.user.model.AdminImageBrowsePage
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// AdminImageBrowseView.kt - Admin Image Browsing Interface
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT (GPT-6.2)
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI assisted with the design and implementation of the admin image browsing
// interface (US 03.06.01), including searchable browsing lists, image statistics,
// image previews, navigation and integration with the existing reusable photo
// gallery and full screen image viewer.
//
// Starting Prompt:
// "I want to implement an admin image browsing interface that allows admins
// to browse and inspect all uploaded images within Camply. I want to separate
// campsite promotional images and user profile images into two categories.
// For promotional images, the admin should first be able to browse campsite
// owners, select an owner, browse their campsites and then view the images
// belonging to a selected campsite. For profile images, the admin should
// be able to browse users with uploaded profile images and select their
// image to open it directly in the full screen image viewer.
//
// I want the owner and user lists to be searchable by profile name or email
// address, and the campsite list to be searchable by campsite name or
// location. Only display entries that have uploaded images, but also display
// counts showing how many owners, campsites and users have images compared
// to their respective totals. Include overall image counts that remain
// independent of the current search results.
//
// Review our existing project implementation and reuse our existing MVC
// architecture, repositories, shared UI components, Camply theme and photo
// gallery wherever possible. Avoid duplicating existing functionality or
// modifying unrelated screens. I want the interface to look clean, modern
// and consistent with the rest of the application."
//
// Follow-up prompts were used to further refine the UI components, visual
// layout, image statistics, browsing functionality, etc...

/**
 * Displays uploaded campsite promotional images and user profile images for administrative inspection (US 03.06.01).
 */
class AdminImageBrowseView {

    /** Represents a confirmed or pending removal request and its execution action. */
    private class RemovalRequest(
        val message: String,
        val execute: suspend () -> ImageRemovalResult
    )

    /**
     * Displays the current image browsing page and loads its data.
     *
     * @param controller manages browsing actions and image loading
     * @param model stores browsing data, selections and search state
     * @param onBack returns to the main admin panel
     * @param onRemoveCampsitePhoto optional removal action invoked after confirmation
     * @param onRemoveProfileImage optional removal action invoked after confirmation
     * @param loadData loads or refreshes the image browsing data
     */
    @Composable
    fun Display(
        controller: AdminImageBrowseController,
        model: AdminImageBrowseModel,
        onBack: () -> Unit,
        onRemoveCampsitePhoto: (suspend (Campsite, String) -> ImageRemovalResult)? = null,
        onRemoveProfileImage: (suspend (User) -> ImageRemovalResult)? = null,
        loadData: suspend () -> Boolean = { controller.loadData() }
    ) {
        // Retry key for loading browsing data independently of image removal.
        var reloadKey by remember { mutableIntStateOf(0) }
        // Keep removal state at screen level so refreshing a list cannot cancel it.
        val scope = rememberCoroutineScope()
        var pendingRemoval by remember(controller) { mutableStateOf<RemovalRequest?>(null) }
        var failedRemoval by remember(controller) { mutableStateOf<RemovalRequest?>(null) }
        var removalResult by remember(controller) { mutableStateOf<ImageRemovalResult?>(null) }
        var isRemoving by remember(controller) { mutableStateOf(false) }
        val interactionsBlocked = isRemoving || pendingRemoval != null || removalResult != null

        fun executeRemoval(request: RemovalRequest) {
            if (isRemoving) return
            isRemoving = true
            pendingRemoval = null
            failedRemoval = null
            removalResult = null
            scope.launch {
                try {
                    val result = request.execute()
                    if (result == ImageRemovalResult.FAILURE) failedRemoval = request
                    removalResult = result
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failedRemoval = request
                    removalResult = ImageRemovalResult.FAILURE
                } finally {
                    isRemoving = false
                }
            }
        }

        LaunchedEffect(controller, reloadKey) {
            loadData()
        }

        DisposableEffect(controller) {
            onDispose {
                controller.reset()
            }
        }

        val goBack: () -> Unit = {
            if (!interactionsBlocked && !controller.back()) onBack()
        }

        BackHandler(onBack = goBack)

        val title = when (model.page) {
            AdminImageBrowsePage.CATEGORIES -> "Uploaded Images"
            AdminImageBrowsePage.OWNERS -> "Campsite Owners"
            AdminImageBrowsePage.CAMPSITES -> "Owner Campsites"
            AdminImageBrowsePage.GALLERY -> "Promotional Images"
            AdminImageBrowsePage.PROFILES -> "Profile Images"
        }

        CamplyScreen(title = title) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    model.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    model.errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ErrorMessage(model.errorMessage.orEmpty())

                            PrimaryButton(
                                text = "Try again",
                                onClick = { reloadKey++ }
                            )
                        }
                    }

                    else -> {
                        when (model.page) {
                            AdminImageBrowsePage.CATEGORIES ->
                                Categories(model, controller)

                            AdminImageBrowsePage.OWNERS ->
                                OwnerList(model, controller)

                            AdminImageBrowsePage.CAMPSITES ->
                                CampsiteList(model, controller)

                            AdminImageBrowsePage.GALLERY ->
                                PhotoGallery(
                                    model,
                                    controller,
                                    onRemoveCampsitePhoto = onRemoveCampsitePhoto?.let { remove ->
                                        { campsite, photoId ->
                                            if (!isRemoving && pendingRemoval == null && removalResult == null) {
                                                pendingRemoval = RemovalRequest(
                                                    "Remove this promotional image from ${campsite.name}? This cannot be undone."
                                                ) { controller.removeCampsitePhoto(campsite, photoId, remove) }
                                            }
                                        }
                                    },
                                    isRemoving = interactionsBlocked
                                )

                            AdminImageBrowsePage.PROFILES ->
                                ProfileList(
                                    model,
                                    controller,
                                    onRemoveProfileImage = onRemoveProfileImage?.let { remove ->
                                        { user ->
                                            if (!isRemoving && pendingRemoval == null && removalResult == null) {
                                                pendingRemoval = RemovalRequest(
                                                    "Remove the profile image of ${user.getProfile().getName()}? This cannot be undone."
                                                ) { controller.removeProfileImage(user, remove) }
                                            }
                                        }
                                    },
                                    isRemoving = interactionsBlocked
                                )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SecondaryButton("Back", onClick = goBack, enabled = !interactionsBlocked)
        }

        // Show dialogs after the browsing content so they appear above the image viewer.
        pendingRemoval?.let { request ->
            RemoveImageConfirmation(
                message = request.message,
                onCancel = { pendingRemoval = null },
                onConfirm = { executeRemoval(request) }
            )
        }

        if (isRemoving) {
            RemovalProgressDialog()
        }

        removalResult?.let { result ->
            RemovalResultDialog(
                result = result,
                refreshFailed = result == ImageRemovalResult.SUCCESS && model.errorMessage != null,
                onClose = {
                    removalResult = null
                    failedRemoval = null
                },
                onRetry = failedRemoval?.let { request -> { executeRemoval(request) } }
            )
        }
    }

    /**
     * Displays the available image categories and overall image counts.
     *
     * @param model source of image counts
     * @param controller handles category selection
     */
    @Composable
    private fun Categories(
        model: AdminImageBrowseModel,
        controller: AdminImageBrowseController
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MutedText("Browse and inspect images uploaded to Camply.")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageStatCard(
                        label = "Total",
                        count = model.totalImageCount,
                        modifier = Modifier.weight(1f)
                    )

                    ImageStatCard(
                        label = "Promotional",
                        count = model.totalCampsiteImageCount,
                        modifier = Modifier.weight(1f)
                    )

                    ImageStatCard(
                        label = "Profile",
                        count = model.usersWithImagesCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Browse images",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                BrowseCard(
                    title = "Campsite promotional images",
                    subtitle = "${model.totalCampsiteImageCount} images",
                    description = "${model.ownersWithImagesCount} of " +
                            "${model.totalOwnerCount} owners with images",
                    onClick = controller::openOwners
                )
            }

            item {
                BrowseCard(
                    title = "Profile images",
                    subtitle = "${model.usersWithImagesCount} images",
                    description = "${model.usersWithImagesCount} of " +
                            "${model.totalUserCount} users with images",
                    onClick = controller::openProfiles
                )
            }
        }
    }

    /**
     * Displays a searchable list of campsite owners with promotional images.
     *
     * @param model source of owners, search results and image counts
     * @param controller handles searching and owner selection
     */
    @Composable
    private fun OwnerList(
        model: AdminImageBrowseModel,
        controller: AdminImageBrowseController
    ) {
        val owners = model.filteredOwners

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CountCard(
                    label = "Owners with uploaded images",
                    count = "${model.ownersWithImagesCount} / " +
                            "${model.totalOwnerCount}",
                    description = "${model.totalCampsiteImageCount} promotional images"
                )
            }

            item {
                SearchField(
                    query = model.ownerSearch,
                    onQueryChange = controller::searchOwners,
                    placeholder = "Search by name or email"
                )
            }

            item {
                MutedText(
                    "Showing ${owners.size} of " +
                            "${model.ownersWithImagesCount} owners"
                )
            }

            if (owners.isEmpty()) {
                item {
                    CenteredMessage(
                        if (model.ownerSearch.isBlank())
                            "No campsite owners have uploaded images."
                        else
                            "No owners match your search."
                    )
                }
            }

            items(owners, key = { it.getUid() }) { owner ->
                val ownerId = owner.getUid()
                val profile = owner.getProfile()
                val campsiteCount = model.campsitesForOwner(ownerId).size
                val campsitesWithImages = model.campsiteImageCountForOwner(ownerId)

                BrowseCard(
                    title = profile.getName(),
                    subtitle = profile.getEmail(),
                    description = "$campsitesWithImages / $campsiteCount " +
                            "campsites with images · " +
                            "${model.imageCountForOwner(ownerId)} images",
                    onClick = { controller.selectOwner(owner) }
                )
            }
        }
    }

    /**
     * Displays the selected owner's campsites containing promotional images.
     *
     * @param model source of campsite data, search results and image counts
     * @param controller handles searching, campsite selection and preview loading
     */
    @Composable
    private fun CampsiteList(
        model: AdminImageBrowseModel,
        controller: AdminImageBrowseController
    ) {
        val owner = model.selectedOwner ?: return
        val campsites = model.filteredCampsites

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = owner.getProfile().getName(),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                CountCard(
                    label = "Campsites with uploaded images",
                    count = "${model.ownerCampsitesWithImages.size} / " +
                            "${model.ownerCampsites.size}",
                    description = "${model.imageCountForOwner(owner.getUid())} " +
                            "promotional images"
                )
            }

            item {
                SearchField(
                    query = model.campsiteSearch,
                    onQueryChange = controller::searchCampsites,
                    placeholder = "Search by name or location"
                )
            }

            item {
                MutedText(
                    "Showing ${campsites.size} of " +
                            "${model.ownerCampsitesWithImages.size} campsites"
                )
            }

            if (campsites.isEmpty()) {
                item {
                    CenteredMessage(
                        if (model.campsiteSearch.isBlank())
                            "No campsites have uploaded images."
                        else
                            "No campsites match your search."
                    )
                }
            }

            items(campsites, key = { it.id }) { campsite ->
                BrowseCard(
                    title = campsite.name,
                    subtitle = campsite.location,
                    description = "${campsite.images.size} promotional images",
                    onClick = { controller.selectCampsite(campsite) },
                    leadingContent = {
                        campsite.images.firstOrNull()?.let { photoId ->
                            PhotoPreview(
                                campsite = campsite,
                                photoId = photoId,
                                controller = controller,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Displays promotional images for the selected campsite using
     * the existing campsite gallery and full screen photo viewer.
     *
     * @param model source of the selected campsite and owner
     * @param controller provides preview and full resolution image loading
     * @param onRemoveCampsitePhoto optional action to request removal of a selected photo
     * @param isRemoving whether image interactions are temporarily blocked
     */
    @Composable
    private fun PhotoGallery(
        model: AdminImageBrowseModel,
        controller: AdminImageBrowseController,
        onRemoveCampsitePhoto: ((Campsite, String) -> Unit)?,
        isRemoving: Boolean
    ) {
        val campsite = model.selectedCampsite ?: return

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = campsite.name,
                    style = MaterialTheme.typography.titleLarge
                )

                MutedText(
                    model.selectedOwner?.getProfile()?.getName().orEmpty()
                )

                MutedText(campsite.location)
            }

            item {
                CountCard(
                    label = "Promotional images",
                    count = campsite.images.size.toString(),
                    description = "Select an image to inspect it."
                )
            }

            item {
                if (campsite.images.isEmpty()) {
                    CenteredMessage("No promotional images.")
                } else {
                    CampsitePhotoGallery(
                        campsite = campsite,
                        loadPhotoPreview = controller::loadPhotoPreview,
                        loadPhoto = controller::loadPhoto,
                        onRemovePhoto = onRemoveCampsitePhoto?.let { request ->
                            { photoId: String -> request(campsite, photoId) }
                        },
                        isRemoving = isRemoving
                    )
                }
            }
        }
    }

    /**
     * Displays a searchable list of users with uploaded profile images.
     * Selecting a user opens their image directly in the shared viewer.
     *
     * @param model source of profiles, image bytes, search results and counts
     * @param controller handles searching and profile selection
     * @param onRemoveProfileImage optional action to request removal of the selected profile
     * @param isRemoving whether image interactions are temporarily blocked
     */
    @Composable
    private fun ProfileList(
        model: AdminImageBrowseModel,
        controller: AdminImageBrowseController,
        onRemoveProfileImage: ((User) -> Unit)?,
        isRemoving: Boolean
    ) {
        val profiles = model.filteredProfiles

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CountCard(
                    label = "Users with profile images",
                    count = "${model.usersWithImagesCount} / " +
                            "${model.totalUserCount}",
                    description = "${model.totalUserCount - model.usersWithImagesCount}" +
                            " users without images"
                )
            }

            item {
                SearchField(
                    query = model.profileSearch,
                    onQueryChange = controller::searchProfiles,
                    placeholder = "Search by name or email"
                )
            }

            item {
                MutedText(
                    "Showing ${profiles.size} of " +
                            "${model.usersWithImagesCount} users"
                )
            }

            if (profiles.isEmpty()) {
                item {
                    CenteredMessage(
                        if (model.profileSearch.isBlank())
                            "No users have uploaded profile images."
                        else
                            "No users match your search."
                    )
                }
            }

            items(profiles, key = { it.getUid() }) { user ->
                val profile = user.getProfile()

                BrowseCard(
                    title = profile.getName(),
                    subtitle = profile.getEmail(),
                    description = "Role: ${user.getRole().replaceFirstChar { it.uppercase() }}",
                    onClick = { controller.selectProfile(user) },
                    onRemove = onRemoveProfileImage?.let { request -> { request(user) } },
                    enabled = !isRemoving,
                    leadingContent = {
                        ProfilePreview(
                            bytes = model.profileImages[user.getUid()],
                            name = profile.getName()
                        )
                    }
                )
            }
        }

        model.selectedProfile?.let { user ->
            model.profileImages[user.getUid()]?.let { bytes ->
                SingleImageViewer(
                    imageId = user.getUid(),
                    imageDescription = "Enlarged profile image of ${user.getProfile().getName()}",
                    imageBytes = bytes,
                    onClose = controller::closeProfileImage,
                    onRemoveImage = onRemoveProfileImage?.let { request ->
                        { request(user) }
                    },
                    isRemoving = isRemoving
                )
            }
        }
    }

    /**
     * Displays a compact image statistic using the Camply theme.
     *
     * @param label type of image being counted
     * @param count number of browsable images of this type
     * @param modifier controls the statistic card layout
     */
    @Composable
    private fun ImageStatCard(label: String, count: Int, modifier: Modifier = Modifier) {
        // Starts the count at zero when the statistics card is displayed.
        var targetCount by remember { mutableIntStateOf(0) }

        val animatedCount by animateIntAsState(
            targetValue = targetCount,
            animationSpec = tween(
                durationMillis = 600,
                easing = LinearEasing
            ),
            label = "Image count"
        )

        LaunchedEffect(count) {
            targetCount = count
        }

        Surface(
            modifier = modifier.heightIn(min = 88.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = animatedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    /**
     * Asks the administrator to confirm removal of a selected image.
     *
     * @param message identifies the image being removed and explains the consequence
     * @param onCancel dismisses the confirmation without removing anything
     * @param onConfirm invokes the supplied removal action
     */
    @Composable
    private fun RemoveImageConfirmation(
        message: String,
        onCancel: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Remove image?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Remove image", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        )
    }

    /** Displays non-dismissible progress while removing an image and refreshing data. */
    @Composable
    private fun RemovalProgressDialog() {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Removing image...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text("Please wait while the image removal request completes.")
                }
            },
            confirmButton = {}
        )
    }

    /**
     * Displays the removal outcome and offers retry only when removal itself failed.
     *
     * @param result outcome reported by the controller
     * @param refreshFailed whether removal succeeded but data could not be refreshed
     * @param onClose dismisses the result dialog
     * @param onRetry retries a failed removal request, or null when unavailable
     */
    @Composable
    private fun RemovalResultDialog(
        result: ImageRemovalResult,
        refreshFailed: Boolean,
        onClose: () -> Unit,
        onRetry: (() -> Unit)?
    ) {
        val title = when (result) {
            ImageRemovalResult.SUCCESS -> "Image removed"
            ImageRemovalResult.FAILURE -> "Unable to remove image"
            ImageRemovalResult.NOT_IMPLEMENTED -> "Not implemented yet"
        }
        val message = when (result) {
            ImageRemovalResult.SUCCESS -> if (refreshFailed)
                "The image was removed, but the browsing data could not be refreshed. Close this dialog and retry loading the images."
            else "The image was removed successfully."
            ImageRemovalResult.FAILURE -> "The removal request failed. Please try again."
            ImageRemovalResult.NOT_IMPLEMENTED -> "Image removal is not implemented yet. No image was deleted."
        }

        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) { Text("Try again") }
                } else {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            },
            dismissButton = {
                if (onRetry != null) TextButton(onClick = onClose) { Text("Close") }
            }
        )
    }

    /**
     * Displays an overall count without changing it when search results
     * are filtered.
     *
     * @param label description of what is being counted
     * @param count formatted count or ratio
     * @param description additional information about the count
     */
    @Composable
    private fun CountCard(
        label: String,
        count: String,
        description: String
    ) {
        OrangeCard {
            CardLabel(label)

            Spacer(Modifier.height(6.dp))

            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(4.dp))

            MutedText(description)
        }
    }

    /**
     * Displays a searchable text field with a clear action.
     *
     * @param query current search text
     * @param onQueryChange updates the search text
     * @param placeholder hint describing searchable fields
     */
    @Composable
    private fun SearchField(
        query: String,
        onQueryChange: (String) -> Unit,
        placeholder: String
    ) {
        Column {
            CamplyTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "Search",
                placeholder = placeholder
            )

            if (query.isNotEmpty()) {
                TextButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear search")
                }
            }
        }
    }

    /**
     * Displays a selectable browsing entry with optional image content.
     *
     * @param title primary text
     * @param subtitle secondary text
     * @param description additional information or image counts
     * @param onClick opens the selected entry
     * @param leadingContent optional preview displayed before the text
     * @param onRemove optional action to request image removal
     * @param enabled whether browsing and removal controls can be pressed
     */
    @Composable
    private fun BrowseCard(
        title: String,
        subtitle: String,
        description: String,
        onClick: () -> Unit,
        leadingContent: (@Composable () -> Unit)? = null,
        onRemove: (() -> Unit)? = null,
        enabled: Boolean = true
    ) {
        OrangeCard(
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leadingContent?.invoke()

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    MutedText(subtitle)

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (onRemove != null) {
                    IconButton(onClick = onRemove, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove profile image",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    /**
     * Loads and displays a campsite promotional image preview.
     *
     * @param campsite campsite associated with the image
     * @param photoId ID of the promotional photo
     * @param controller loads the preview from the repository
     * @param modifier controls the preview layout and size
     */
    @Composable
    private fun PhotoPreview(
        campsite: Campsite,
        photoId: String,
        controller: AdminImageBrowseController,
        modifier: Modifier = Modifier
    ) {
        var bitmap by remember(campsite.id, photoId) {
            mutableStateOf<ImageBitmap?>(null)
        }

        var isLoading by remember(campsite.id, photoId) {
            mutableStateOf(true)
        }

        LaunchedEffect(campsite.id, photoId) {
            isLoading = true

            try {
                bitmap = decodeImage(
                    controller.loadPhotoPreview(campsite.id, photoId)
                )
            } finally {
                isLoading = false
            }
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = "Photo of ${campsite.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                else -> {
                    Text(
                        text = "No preview",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    /**
     * Displays a user's previously loaded profile image.
     *
     * @param bytes compressed profile image bytes
     * @param name user profile name used for accessibility
     */
    @Composable
    private fun ProfilePreview(
        bytes: ByteArray?,
        name: String
    ) {
        val bitmap by produceState<ImageBitmap?>(
            initialValue = null,
            key1 = bytes
        ) {
            value = decodeImage(bytes)
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "$name's profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    /**
     * Decodes compressed image bytes off the main thread.
     *
     * @param bytes compressed image data, or null if unavailable
     * @return decoded bitmap, or null if decoding fails
     */
    private suspend fun decodeImage(
        bytes: ByteArray?
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        bytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }

    /**
     * Displays secondary text using the active Camply theme.
     *
     * @param text content to display
     */
    @Composable
    private fun MutedText(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

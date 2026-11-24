package com.example.camply.campsite.view

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CamplyTextField
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import com.example.camply.ui.theme.CamplyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

// CampsiteFormView.kt - Campsite Registration and Editing UI Redesign
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help redesign the existing campsite registration and editing form
// to match the updated Camply interface. This included assistance with improving the
// form layout, adding promotional photo management, updating validation feedback, and
// improving the rules and cancellation policy editor.
//
// Prompt summary:
// "Can you help me redesign CampsiteFormView.kt to match the new Camply theme and reusable UI
// components? I want the form to have a dedicated optional photo section where campsite owners
// can select, replace, and remove up to five promotional photos. The form should maintain an
// ordered list of photo IDs with the first photo being the cover image. Owners should be able to
// select a different cover photo and preview both newly selected local images and existing
// Firestore image previews. Keep all photo changes local until the owner saves the campsite and
// pass the changes through the existing submission callback. I also want clearer required field
// validation, an improved rules and cancellation policy editor, and a saving indicator that
// prevents changes while the form is being submitted."
//
// Additional follow-up prompts were used to refine the photo gallery, form validation,
// policies dialog, layout styling, saving progress indicator, campsite location
// selection using an interactive map with place search, pin placement, and confirmation.

/**
 * US 02.01.01 - 02.01.03: Form for registering or editing a campsite.
 *
 * Owners can select up to five optional promotional photos, replace or remove
 * individual photos, and reorder their cover photo before saving. All changes
 * stay local to the form until the owner presses Save.
 */
class CampsiteFormView {

    /**
     * Displays the campsite form using the shared Camply theme and components.
     *
     * The final ordered photo IDs and pending photo URIs are submitted together
     * with the existing campsite fields. The form itself does not write to Firebase.
     *
     * @param campsite campsite to edit, or null when registering
     * @param errors validation or save errors from the owner controller
     * @param onSubmit submits all fields, ordered photo IDs and pending image URIs
     * @param onBack discards local edits and leaves the form
     * @param loadPhotoPreview retrieves the saved display JPEG for an existing photo
     * @param isSaving locks the form and shows progress while saving is in progress
     */
    @Composable
    fun Display(
        campsite: Campsite?,
        errors: List<String>,
        onSubmit: (
            String, String, String, String, String, String, String, String, String,
            List<String>, Map<String, Uri>, Double?, Double?
        ) -> Unit,
        onBack: () -> Unit,
        loadPhotoPreview: suspend (String, String) -> ByteArray?,
        isSaving: Boolean = false
    ) {
        var name by remember(campsite) { mutableStateOf(campsite?.name ?: "") }
        var location by remember(campsite) { mutableStateOf(campsite?.location ?: "") }
        var selectedLatitude by remember(campsite?.id) { mutableStateOf(campsite?.latitude) }
        var selectedLongitude by remember(campsite?.id) { mutableStateOf(campsite?.longitude) }
        var showMapPicker by remember(campsite?.id) { mutableStateOf(false) }
        var description by remember(campsite) { mutableStateOf(campsite?.description ?: "") }
        var amenities by remember(campsite) { mutableStateOf(campsite?.amenities?.joinToString(", ") ?: "") }
        var occupancy by remember(campsite) { mutableStateOf(campsite?.maxOccupancy?.toString() ?: "") }
        var price by remember(campsite) { mutableStateOf(campsite?.price?.toString() ?: "") }
        var tags by remember(campsite) { mutableStateOf(campsite?.tags?.joinToString(", ") ?: "") }
        var rules by remember(campsite) { mutableStateOf(campsite?.rules ?: "") }
        var cancellation by remember(campsite) { mutableStateOf(campsite?.cancellationPolicy ?: "") }
        var showPolicies by remember { mutableStateOf(false) }

        // Editing the dialog never changes the form until Save changes is pressed.
        var draftRules by remember(campsite) { mutableStateOf(rules) }
        var draftCancellation by remember(campsite) { mutableStateOf(cancellation) }

        // Highlight required fields after Save is attempted or validation fails.
        var submitAttempted by remember(campsite?.id) { mutableStateOf(false) }
        val showValidation = submitAttempted || errors.isNotEmpty()
        val nameError = showValidation && name.isBlank()
        val locationError = showValidation && location.isBlank()
        val descriptionError = showValidation && description.isBlank()
        val occupancyError = showValidation && (occupancy.toIntOrNull() ?: 0) <= 0
        val priceError = showValidation && (price.toDoubleOrNull() ?: 0.0) <= 0.0
        val rulesError = showValidation && rules.isBlank()
        val cancellationError = showValidation && cancellation.isBlank()
        val pinError = showValidation && (selectedLatitude == null || selectedLongitude == null)

        // The first photo ID is the cover image. Existing IDs stay unchanged.
        var photoIds by remember(campsite?.id) { mutableStateOf(campsite?.images.orEmpty()) }

        // Only added or replaced photos are processed when Save is pressed.
        val pendingPhotos = remember(campsite?.id) {
            mutableStateMapOf<String, Uri>()
        }
        val savedPreviews = remember(campsite?.id) {
            mutableStateMapOf<String, ImageBitmap>()
        }
        var replacingPhotoId by remember(campsite?.id) { mutableStateOf<String?>(null) }
        var selectedPhotoId by remember(campsite?.id) {
            mutableStateOf(campsite?.images?.firstOrNull())
        }
        var photoError by remember(campsite?.id) { mutableStateOf<String?>(null) }

        // Pick multiple photos, respecting the remaining number of slots.
        val addPhotoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(Campsite.MAX_IMAGES)
        ) { uris ->
            val remaining = Campsite.MAX_IMAGES - photoIds.size
            val availableUris = uris.filter { it !in pendingPhotos.values }
            for (uri in availableUris.take(remaining)) {
                val photoId = UUID.randomUUID().toString()
                photoIds = photoIds + photoId
                pendingPhotos[photoId] = uri
                if (selectedPhotoId == null) selectedPhotoId = photoId
            }
            photoError = if (availableUris.size > remaining) {
                "You can add up to ${Campsite.MAX_IMAGES} campsite photos."
            } else {
                null
            }
        }

        // Replacing a photo keeps its ID and gallery position.
        val replacePhotoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            val photoId = replacingPhotoId
            if (uri != null && photoId != null && photoId in photoIds) {
                val alreadySelected = pendingPhotos.any { (id, selectedUri) ->
                    id != photoId && selectedUri == uri
                }
                if (alreadySelected) {
                    photoError = "This photo has already been selected."
                } else {
                    pendingPhotos[photoId] = uri
                    photoError = null
                }
            }
            replacingPhotoId = null
        }

        // Read each existing preview once for both the cover and thumbnail views.
        LaunchedEffect(campsite?.id, photoIds) {
            val campsiteId = campsite?.id.orEmpty()
            if (campsiteId.isNotBlank()) {
                for (photoId in photoIds) {
                    if (photoId in pendingPhotos || photoId in savedPreviews) continue

                    val bytes = try {
                        loadPhotoPreview(campsiteId, photoId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }

                    val bitmap = withContext(Dispatchers.Default) {
                        bytes?.let {
                            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                        }
                    }
                    if (bitmap != null) savedPreviews[photoId] = bitmap
                }
            }
        }

        if (showMapPicker) {
            CampsiteLocationPicker(
                initialLatitude = selectedLatitude,
                initialLongitude = selectedLongitude,
                onDismiss = {
                    showMapPicker = false
                },
                onConfirm = { latitude, longitude, suggestedName ->
                    selectedLatitude = latitude
                    selectedLongitude = longitude

                    if (!suggestedName.isNullOrBlank()) {
                        location = suggestedName
                    }

                    showMapPicker = false
                }
            )
        }

        if (showPolicies) {
            CampsiteFormPoliciesDialog(
                draftRules = draftRules,
                onDraftRulesChange = { draftRules = it },
                draftCancellation = draftCancellation,
                onDraftCancellationChange = { draftCancellation = it },
                showValidation = showValidation,
                onDismiss = { showPolicies = false },
                onSave = {
                    rules = draftRules.trim()
                    cancellation = draftCancellation.trim()
                    showPolicies = false
                }
            )
        }

        CamplyScreen(
            title = if (campsite == null) "Add a campsite" else "Edit campsite"
        ) {
            Text(
                text = if (campsite == null) {
                    "Create a great first impression for your next guests."
                } else {
                    "Update your listing and keep your photos looking their best."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Scroll form content independently of the save actions.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CampsiteFormBasicsSection(
                    name = name,
                    onNameChange = { name = it },
                    location = location,
                    onLocationChange = { location = it },
                    hasMapPin = selectedLatitude != null && selectedLongitude != null,
                    onPickLocation = { showMapPicker = true },
                    nameError = nameError,
                    locationError = locationError,
                    pinError = pinError
                )

                CampsiteFormPhotosSection(
                    photoIds = photoIds,
                    pendingPhotos = pendingPhotos,
                    savedPreviews = savedPreviews,
                    selectedPhotoId = selectedPhotoId,
                    photoError = photoError,
                    onAddPhotos = {
                        addPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPhotoSelected = { selectedPhotoId = it },
                    onReplacePhoto = { photoId ->
                        replacingPhotoId = photoId
                        replacePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemovePhoto = { photoId ->
                        photoIds = photoIds.filterNot { it == photoId }
                        pendingPhotos.remove(photoId)
                        selectedPhotoId = photoIds.firstOrNull()
                        photoError = null
                    },
                    onSetCover = { photoId ->
                        photoIds = listOf(photoId) + photoIds.filterNot { it == photoId }
                    }
                )

                CampsiteFormAboutSection(
                    description = description,
                    onDescriptionChange = { description = it },
                    amenities = amenities,
                    onAmenitiesChange = { amenities = it },
                    tags = tags,
                    onTagsChange = { tags = it },
                    descriptionError = descriptionError
                )

                CampsiteFormStaySection(
                    occupancy = occupancy,
                    onOccupancyChange = { occupancy = it },
                    price = price,
                    onPriceChange = { price = it },
                    occupancyError = occupancyError,
                    priceError = priceError
                )

                CampsiteFormPoliciesSection(
                    rules = rules,
                    cancellation = cancellation,
                    rulesError = rulesError,
                    cancellationError = cancellationError,
                    onEditPolicies = {
                        draftRules = rules
                        draftCancellation = cancellation
                        showPolicies = true
                    }
                )

                if (submitAttempted && (description.isBlank() || cancellation.isBlank())) {
                    Column(modifier = Modifier.testTag("required_fields_error")) {
                        ErrorMessage("Please complete the highlighted required fields before saving.")
                    }
                }
                if (errors.isNotEmpty()) {
                    Column(modifier = Modifier.testTag("errors_text")) {
                        errors.forEach { ErrorMessage(it) }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(12.dp))
            CampsiteFormActions(
                isEditing = campsite != null,
                onBack = onBack,
                onSave = {
                    submitAttempted = true

                    val missingRequiredPin = selectedLatitude == null || selectedLongitude == null

                    if (description.isNotBlank() && cancellation.isNotBlank() && !missingRequiredPin)
                    {
                        onSubmit(
                            name, location, description, amenities, occupancy, price, tags, rules,
                            cancellation, photoIds, pendingPhotos.toMap(), selectedLatitude, selectedLongitude
                        )
                    }
                }
            )
        }

        // A modal dialog blocks all form interactions until the save finishes.
        // CamplyApp controls isSaving so this remains visible for the entire
        // asynchronous repository operation, not just the button click.
        if (isSaving) {
            CampsiteFormSavingDialog(isEditing = campsite != null)
        }
    }
}

/** Campsite name, map pin and readable location name. */
@Composable
private fun CampsiteFormBasicsSection(
    name: String,
    onNameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    hasMapPin: Boolean,
    onPickLocation: () -> Unit,
    nameError: Boolean,
    locationError: Boolean,
    pinError: Boolean
) {
    OrangeCard {
        CampsiteFormSectionHeading(
            title = "The essentials",
            subtitle = "Give your campsite a name and location."
        )
        Spacer(Modifier.height(18.dp))
        CamplyTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Campsite name *",
            placeholder = "e.g. Pine Ridge Retreat",
            isError = nameError,
            modifier = Modifier.testTag("name_field")
        )
        CampsiteFormRequiredFieldError(
            show = nameError,
            message = "Please enter a campsite name."
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Campsite location *",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPickLocation,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("map_picker_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                if (pinError) 2.dp else 1.dp,
                if (pinError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outlineVariant
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (pinError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (!hasMapPin) {
                    "Choose campsite location on map"
                } else {
                    "Change campsite map pin"
                }
            )
        }

        Text(
            text = if (hasMapPin) {
                "Map pin selected"
            } else {
                "Search for an area and tap the exact campsite location."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CampsiteFormRequiredFieldError(
            show = pinError,
            message = "Please select a campsite location on the map."
        )

        Spacer(Modifier.height(10.dp))

        CamplyTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "Location name *",
            placeholder = "e.g. Banff, Alberta",
            isError = locationError,
            modifier = Modifier.testTag("location_field")
        )

        Text(
            text = "Filled from the map when possible. You can correct this name.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CampsiteFormRequiredFieldError(
            show = locationError,
            message = "Please enter a campsite location name."
        )

    }
}

/** Optional promotional images and local cover-photo editing. */
@Composable
private fun CampsiteFormPhotosSection(
    photoIds: List<String>,
    pendingPhotos: Map<String, Uri>,
    savedPreviews: Map<String, ImageBitmap>,
    selectedPhotoId: String?,
    photoError: String?,
    onAddPhotos: () -> Unit,
    onPhotoSelected: (String) -> Unit,
    onReplacePhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSetCover: (String) -> Unit
) {
    OrangeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Promotional photos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Optional · Add up to five photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "${photoIds.size}/${Campsite.MAX_IMAGES}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (photoIds.isEmpty()) {
            OutlinedButton(
                onClick = onAddPhotos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("add_photo_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Add campsite photos", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Choose up to five from your library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // The first image is the cover, regardless of which thumbnail is selected.
            val coverId = photoIds.first()
            val selectedId = selectedPhotoId?.takeIf { it in photoIds } ?: coverId
            val selectedNumber = photoIds.indexOf(selectedId) + 1

            Box {
                CampsiteFormPhotoImage(
                    pendingUri = pendingPhotos[coverId],
                    savedPreview = savedPreviews[coverId],
                    contentDescription = "Campsite cover photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Text(
                        "Cover photo",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Your photos · Select a thumbnail to edit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))

            // Compact photo thumbnails; actions appear only once beneath the strip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                photoIds.forEachIndexed { index, photoId ->
                    key(photoId) {
                        CampsiteFormPhotoThumbnail(
                            photoId = photoId,
                            photoNumber = index + 1,
                            selected = photoId == selectedId,
                            pendingUri = pendingPhotos[photoId],
                            savedPreview = savedPreviews[photoId],
                            onSelect = { onPhotoSelected(photoId) }
                        )
                    }
                }
                if (photoIds.size < Campsite.MAX_IMAGES) {
                    OutlinedButton(
                        onClick = onAddPhotos,
                        modifier = Modifier
                            .size(90.dp)
                            .testTag("add_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.height(4.dp))
                            Text("Add photo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // A single editing panel for the selected thumbnail.
            // Keeping these actions separate from thumbnails avoids tall photo cards.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Photo $selectedNumber",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Selected for editing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedId == coverId) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Cover",
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Actual outlined buttons, sized comfortably for tapping.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReplacePhoto(selectedId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("replace_photo_$selectedId"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Replace",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = { onRemovePhoto(selectedId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("remove_photo_$selectedId"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "Remove",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Changing the cover only reorders photo IDs locally.
                if (selectedId != coverId) {
                    Button(
                        onClick = { onSetCover(selectedId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cover_photo_$selectedId"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Set as cover photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Your first photo is the cover shown on campsite cards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        photoError?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("photo_error")
            )
        }
    }
}

/** Description, optional amenities and searchable tags. */
@Composable
private fun CampsiteFormAboutSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    amenities: String,
    onAmenitiesChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    descriptionError: Boolean
) {
    OrangeCard {
        CampsiteFormSectionHeading(
            title = "About your space",
            subtitle = "Help campers understand what makes it special."
        )
        Spacer(Modifier.height(18.dp))
        CamplyTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description *",
            placeholder = "Tell campers about the site, surroundings, and experience",
            singleLine = false,
            isError = descriptionError,
            modifier = Modifier.testTag("description_field")
        )
        CampsiteFormRequiredFieldError(
            show = descriptionError,
            message = "Please describe your campsite."
        )
        Spacer(Modifier.height(14.dp))
        CamplyTextField(
            value = amenities,
            onValueChange = onAmenitiesChange,
            label = "Amenities",
            placeholder = "Fire pit, drinking water, parking...",
            modifier = Modifier.testTag("amenities_field")
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Optional · Separate amenities with commas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        CamplyTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = "Tags",
            placeholder = "Forest, lakeside, family friendly...",
            modifier = Modifier.testTag("tags_field")
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Optional · Separate tags with commas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Capacity and nightly price inputs. */
@Composable
private fun CampsiteFormStaySection(
    occupancy: String,
    onOccupancyChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    occupancyError: Boolean,
    priceError: Boolean
) {
    OrangeCard {
        CampsiteFormSectionHeading(
            title = "Stay details",
            subtitle = "Set the capacity and nightly price."
        )
        Spacer(Modifier.height(18.dp))
        CamplyTextField(
            value = occupancy,
            onValueChange = { onOccupancyChange(it.filter(Char::isDigit)) },
            label = "Maximum occupancy *",
            placeholder = "e.g. 4",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = occupancyError,
            modifier = Modifier.testTag("occupancy_field")
        )
        CampsiteFormRequiredFieldError(
            show = occupancyError,
            message = "Enter a number greater than zero."
        )
        Spacer(Modifier.height(14.dp))
        CamplyTextField(
            value = price,
            onValueChange = { onPriceChange(it.filter { character -> character.isDigit() || character == '.' }) },
            label = "Nightly price (CAD) *",
            placeholder = "e.g. 35.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = priceError,
            modifier = Modifier.testTag("price_field")
        )
        CampsiteFormRequiredFieldError(
            show = priceError,
            message = "Enter a price greater than zero."
        )
    }
}

/** A summary of the campsite rules and cancellation policy. */
@Composable
private fun CampsiteFormPoliciesSection(
    rules: String,
    cancellation: String,
    rulesError: Boolean,
    cancellationError: Boolean,
    onEditPolicies: () -> Unit
) {
    OrangeCard {
        CampsiteFormSectionHeading(
            title = "Rules & policies",
            subtitle = "Add your campsite rules and cancellation terms. Both are required."
        )
        Spacer(Modifier.height(16.dp))
        if (rules.isBlank() && cancellation.isBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = if (rulesError || cancellationError) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                } else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "No campsite rules added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add your campsite rules and cancellation policy before publishing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
                border = if (rulesError || cancellationError) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                } else null
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "CAMPSITE RULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        rules.ifBlank { "Not added yet" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rules.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "CANCELLATION POLICY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        cancellation.ifBlank { "Not added yet" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (cancellation.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        CampsiteFormRequiredFieldError(
            show = rulesError,
            message = "Add campsite rules before saving."
        )
        CampsiteFormRequiredFieldError(
            show = cancellationError,
            message = "Add a cancellation policy before saving."
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEditPolicies,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rules_policies_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (rules.isBlank() && cancellation.isBlank()) "Add rules & policies" else "Edit rules & policies")
        }
    }
}

/** Bottom actions stay outside the scrollable form. */
@Composable
private fun CampsiteFormActions(
    isEditing: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SecondaryButton(
            text = "Cancel",
            onClick = onBack,
            modifier = Modifier.weight(1f).testTag("cancel_button")
        )
        PrimaryButton(
            text = if (isEditing) "Save changes" else "Create campsite",
            onClick = onSave,
            modifier = Modifier.weight(1f).testTag("save_button")
        )
    }
}

/** Edits policy drafts without changing the form until Save changes is pressed. */
@Composable
private fun CampsiteFormPoliciesDialog(
    draftRules: String,
    onDraftRulesChange: (String) -> Unit,
    draftCancellation: String,
    onDraftCancellationChange: (String) -> Unit,
    showValidation: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag("policies_dialog"),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(22.dp)
                    )
                }
                Column {
                    Text(
                        "Rules & policies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Set expectations for your guests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "Clear guidelines help campers plan their stay and avoid surprises.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "01  ·  CAMPSITE RULES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = draftRules,
                        onValueChange = onDraftRulesChange,
                        label = { Text("Campsite rules *") },
                        placeholder = { Text("Enter your campsite rules") },
                        minLines = 3,
                        maxLines = 6,
                        isError = showValidation && draftRules.isBlank(),
                        colors = camplyTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rules_field")
                    )
                    Text(
                        "Example: Quiet hours after 10 PM. Keep fires attended and pets on a leash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showValidation && draftRules.isBlank()) {
                        Text(
                            "* Campsite rules are required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "02  ·  CANCELLATION POLICY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = draftCancellation,
                        onValueChange = onDraftCancellationChange,
                        label = { Text("Cancellation policy *") },
                        placeholder = { Text("Enter your cancellation policy") },
                        minLines = 3,
                        maxLines = 6,
                        isError = showValidation && draftCancellation.isBlank(),
                        colors = camplyTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancellation_field")
                    )
                    Text(
                        "Example: Free cancellation up to 48 hours before arrival. " +
                                "Cancellations after that are non-refundable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showValidation && draftCancellation.isBlank()) {
                        Text(
                            "* Cancellation policy is required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("policies_done_button")
            ) {
                Text("Save changes")
            }
        }
    )
}

/** Blocks the form while the campsite save is in progress. */
@Composable
private fun CampsiteFormSavingDialog(isEditing: Boolean) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("saving_campsite_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (!isEditing) {
                        "Creating campsite…"
                    } else {
                        "Saving changes…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Please keep this screen open while your campsite is saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


/** Shows a clear, local validation hint only for an invalid required field. */
@Composable
private fun CampsiteFormRequiredFieldError(show: Boolean, message: String) {
    if (show) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "* $message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/** Displays a section title and supporting instruction inside a themed card. */
@Composable
private fun CampsiteFormSectionHeading(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Displays either a pending local URI, a saved JPEG preview, or a placeholder. */
@Composable
private fun CampsiteFormPhotoImage(
    pendingUri: Uri?,
    savedPreview: ImageBitmap?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        when {
            pendingUri != null -> AsyncImage(
                model = pendingUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            savedPreview != null -> Image(
                bitmap = savedPreview,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Photo preview unavailable",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** A compact, selectable thumbnail. Photo actions are shown below the thumbnail strip. */
@Composable
private fun CampsiteFormPhotoThumbnail(
    photoId: String,
    photoNumber: Int,
    selected: Boolean,
    pendingUri: Uri?,
    savedPreview: ImageBitmap?,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .size(90.dp)
            .testTag("photo_$photoId"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            CampsiteFormPhotoImage(
                pendingUri = pendingUri,
                savedPreview = savedPreview,
                contentDescription = "Select campsite photo $photoNumber",
                modifier = Modifier.fillMaxSize()
            )
            if (photoNumber == 1) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Text(
                        "Cover",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


/**
 * Full-screen campsite location picker.
 *
 * Search moves the map camera without selecting a pin.
 * Tapping the map sets temporary coordinates until confirmed.
 *
 * @param initialLatitude Existing campsite latitude, if available.
 * @param initialLongitude Existing campsite longitude, if available.
 * @param onDismiss Discards the temporary map selection.
 * @param onConfirm Returns the coordinates and suggested location name.
 */
@Composable
private fun CampsiteLocationPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var matches by remember {
        mutableStateOf<List<Address>>(emptyList())
    }
    var searchMessage by remember {
        mutableStateOf<String?>(null)
    }
    var searching by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    // Keep map edits separate from the form until Confirm.
    var draftLatitude by remember {
        mutableStateOf(initialLatitude)
    }
    var draftLongitude by remember {
        mutableStateOf(initialLongitude)
    }

    // Used to move the map to a search result.
    var focusLatitude by remember {
        mutableStateOf<Double?>(null)
    }
    var focusLongitude by remember {
        mutableStateOf<Double?>(null)
    }

    Dialog(
        onDismissRequest = {
            if (!confirming) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !confirming,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose campsite location",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Search for an area, then tap the exact campsite location to set the pin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Location search.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CamplyTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Search area",
                        placeholder = "e.g. Jasper, Alberta",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_field")
                    )

                    Button(
                        enabled = query.isNotBlank() &&
                                !searching && !confirming,
                        onClick = {
                            scope.launch {
                                searching = true
                                searchMessage = null

                                try {
                                    matches = withTimeoutOrNull(6.seconds) {
                                        findMapPlaces(context, query.trim())
                                    }.orEmpty()

                                    if (matches.size == 1) {
                                        val result = matches.first()

                                        // Automatically move the map when there is only one result.
                                        focusLatitude = result.latitude
                                        focusLongitude = result.longitude

                                        matches = emptyList()
                                        searchMessage = "Area found. Tap the exact spot to place your pin."
                                    } else if (matches.isEmpty()) {
                                        searchMessage =
                                            "No results found. You can still move the map manually."
                                    }
                                } finally {
                                    searching = false
                                }
                            }
                        }
                    ) {
                        if (searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Search")
                        }
                    }
                }

                searchMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Results move the camera; they don't select a pin.
                if (matches.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 144.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        matches.forEach { address ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    focusLatitude = address.latitude
                                    focusLongitude = address.longitude

                                    matches = emptyList()
                                    searchMessage =
                                        "Area found. Tap the exact spot to place your pin."
                                }
                            ) {
                                Text(
                                    text = address.getAddressLine(0)
                                        ?: "${address.latitude}, ${address.longitude}",
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Shared MapLibre component.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    CampsiteMap(
                        latitude = draftLatitude,
                        longitude = draftLongitude,
                        editable = true,
                        focusLatitude = focusLatitude,
                        focusLongitude = focusLongitude,
                        onLocationSelected = { latitude, longitude ->
                            draftLatitude = latitude
                            draftLongitude = longitude
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = if (
                        draftLatitude != null &&
                        draftLongitude != null
                    ) {
                        "Pin: ${
                            String.format(Locale.US, "%.5f", draftLatitude)
                        }, ${
                            String.format(Locale.US, "%.5f", draftLongitude)
                        }"
                    } else {
                        "No pin selected yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = {
                            if (!confirming) onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        enabled = draftLatitude != null &&
                                draftLongitude != null && !confirming,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_confirm_button"),
                        onClick = {
                            val latitude = draftLatitude
                                ?: return@Button
                            val longitude = draftLongitude
                                ?: return@Button

                            scope.launch {
                                confirming = true

                                try {
                                    // Try to suggest a readable place name.
                                    val suggestedName =
                                        withTimeoutOrNull(4.seconds) {
                                            findMapPlaceName(
                                                context,
                                                latitude,
                                                longitude
                                            )
                                        }

                                    onConfirm(
                                        latitude,
                                        longitude,
                                        suggestedName
                                    )
                                } finally {
                                    confirming = false
                                }
                            }
                        }
                    ) {
                        if (confirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Confirm location")
                        }
                    }
                }
            }
        }
    }
}


/**
 * Searches for general locations using Android's Geocoder.
 * Returns an empty list when geocoding is unavailable.
 */
private suspend fun findMapPlaces(
    context: Context,
    query: String
): List<Address> {
    if (!Geocoder.isPresent()) return emptyList()

    val geocoder = Geocoder(context, Locale.getDefault())

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    query,
                    5,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(
                            addresses: MutableList<Address>
                        ) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5).orEmpty()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }
}


/**
 * Attempts to turn selected coordinates into a readable
 * location name for the campsite form.
 */
private suspend fun findMapPlaceName(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    if (!Geocoder.isPresent()) return null

    val geocoder = Geocoder(context, Locale.getDefault())

    val addresses = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(
                            addresses: MutableList<Address>
                        ) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                ).orEmpty()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    val address = addresses.firstOrNull() ?: return null

    return listOfNotNull(
        address.locality,
        address.adminArea,
        address.countryName
    )
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
}



@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteFormViewAddPreview() {
    CamplyTheme {
        CampsiteFormView().Display(
            campsite = null,
            errors = emptyList(),
            onSubmit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onBack = {},
            loadPhotoPreview = { _, _ -> null }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteFormViewEditPreview() {
    CamplyTheme {
        CampsiteFormView().Display(
            campsite = Campsite(
                id = "preview-site",
                name = "Lakeview Pines",
                location = "Jasper, Alberta",
                description = "A peaceful lakeside campground",
                amenities = listOf("Water", "Parking"),
                maxOccupancy = 6,
                price = 45.0,
                tags = listOf("Lake", "Quiet"),
                rules = "Quiet hours after 10 PM",
                cancellationPolicy = "Free cancellation up to 48 hours before arrival"
            ),
            errors = emptyList(),
            onSubmit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onBack = {},
            loadPhotoPreview = { _, _ -> null }
        )
    }
}


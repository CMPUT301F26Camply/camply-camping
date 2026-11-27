package com.example.camply.campsite.view

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.view.DangerButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

// CampsitePhotoGallery.kt - Campsite Photos & Full-Screen Viewer
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT (GPT-6)
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI helped implement a reusable campsite photo gallery component using the
// existing Firestore photo storage system. Assistance included loading image
// previews and full resolution photos, displaying selectable thumbnails,
// implementing zoom gestures, and designing full screen photo navigation
// controls using the existing Camply theme. AI was also used to extend the
// component to support optional image removal controls and direct viewing of
// individual profile images for admin image browsing (US 03.06.01 and US 03.03.01).
//
// Starting Prompt:
// "I want to create a reusable CampsitePhotoGallery.kt component for displaying campsite photos.
// The gallery should display a large main photo with smaller selectable thumbnails below it,
// using the existing Firestore photo IDs and loading functions. Tapping the main photo should
// open a full screen viewer with pinch zoom and double tap zoom support. I want the viewer to
// look clean and modern, with left and right arrow buttons overlaid at the middle left and right
// center of the photo. Add an X icon in the top right corner to close the viewer and a small photo
// count chip in the top left showing something like Photo 1 of 4. Preserve the saved photo order,
// load full resolution images only when needed, and use our existing Camply theme and styling
// rather than hardcoded colors. Keep the component independent so it can be reused across
// different screens."

/**
 * Displays campsite promotional images with selectable previews and a full screen photo viewer.
 *
 * @param campsite campsite containing the promotional photo IDs
 * @param loadPhotoPreview loads a preview using the campsite and photo IDs
 * @param loadPhoto loads a full-resolution photo using the campsite and photo IDs
 * @param onRemovePhoto optional removal action for the selected photo ID
 * @param isRemoving whether an image removal operation is in progress
 */
@Composable
fun CampsitePhotoGallery(
    campsite: Campsite,
    loadPhotoPreview: suspend (String, String) -> ByteArray?,
    loadPhoto: suspend (String, String) -> ByteArray?,
    onRemovePhoto: ((String) -> Unit)? = null,
    isRemoving: Boolean = false
) {
    // Images are Firestore document IDs, not URLs, and the first photo is the cover.
    val photoIds = campsite.images
    if (photoIds.isEmpty()) return

    var selectedPhotoId by remember(campsite.id, photoIds) { mutableStateOf(photoIds.first()) }
    var showingFullPhoto by remember(campsite.id, photoIds) { mutableStateOf(false) }
    val previews = remember(campsite.id, photoIds) { mutableStateMapOf<String, ImageBitmap>() }
    val previewFinished = remember(campsite.id, photoIds) { mutableStateMapOf<String, Boolean>() }
    val fullPhotos = remember(campsite.id, photoIds) { mutableStateMapOf<String, ImageBitmap>() }

    LaunchedEffect(campsite.id, photoIds) {
        photoIds.forEach { photoId ->
            try {
                val bitmap = decodePhoto(loadPhotoPreview(campsite.id, photoId))
                if (bitmap != null) previews[photoId] = bitmap
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Show the placeholder when a photo is missing or unavailable.
            } finally {
                previewFinished[photoId] = true
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !isRemoving) { showingFullPhoto = true },
            contentAlignment = Alignment.Center
        ) {
            val preview = previews[selectedPhotoId]
            when {
                preview != null -> Image(
                    bitmap = preview,
                    contentDescription = "Photo of ${campsite.name}. Tap to enlarge.",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                previewFinished[selectedPhotoId] != true -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                else -> Text(
                    "Photo unavailable. Tap to view full image.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Display the removal control when a removal action is provided.
            if (onRemovePhoto != null) {
                RemovePhotoIcon(
                    onClick = { onRemovePhoto(selectedPhotoId) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    enabled = !isRemoving
                )
            }

            if (photoIds.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${photoIds.indexOf(selectedPhotoId) + 1} / ${photoIds.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }

        if (photoIds.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photoIds, key = { it }) { photoId ->
                    val selected = photoId == selectedPhotoId
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clickable(enabled = !isRemoving) { selectedPhotoId = photoId },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val thumbnail = previews[photoId]
                            if (thumbnail != null) {
                                Image(
                                    bitmap = thumbnail,
                                    contentDescription = "Select photo ${photoIds.indexOf(photoId) + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "${photoIds.indexOf(photoId) + 1}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
        }
    }

    if (showingFullPhoto) {
        FullPhotoDialog(
            imageOwnerId = campsite.id,
            imageDescription = "Enlarged photo of ${campsite.name}",
            photoIds = photoIds,
            selectedPhotoId = selectedPhotoId,
            onSelectPhoto = { selectedPhotoId = it },
            onClose = { showingFullPhoto = false },
            previews = previews,
            fullPhotos = fullPhotos,
            loadPhoto = loadPhoto,
            onRemovePhoto = onRemovePhoto,
            isRemoving = isRemoving
        )
    }
}

/**
 * Displays an optional removal control for a gallery photo.
 *
 * @param onClick requests removal of the associated photo
 * @param modifier controls placement of the removal button
 * @param enabled whether the removal button can be pressed
 */
@Composable
private fun RemovePhotoIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
        shape = CircleShape
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove image",
                tint = MaterialTheme.colorScheme.inverseOnSurface.copy(
                    alpha = if (enabled) 1f else 0.38f
                )
            )
        }
    }
}

/**
 * Displays a full-screen image viewer with zoom, photo navigation
 * and an optional image removal action.
 *
 * @param imageOwnerId ID associated with the image collection
 * @param imageDescription accessibility description of the image
 * @param photoIds ordered IDs of the available photos
 * @param selectedPhotoId ID of the currently displayed photo
 * @param onSelectPhoto selects another photo
 * @param onClose closes the viewer
 * @param previews cached preview images indexed by photo ID
 * @param fullPhotos cached full-resolution images indexed by photo ID
 * @param loadPhoto loads image bytes using the owner and photo IDs
 * @param onRemovePhoto optional removal action for the displayed photo ID
 * @param isRemoving whether an image removal operation is in progress
 */
@Composable
private fun FullPhotoDialog(
    imageOwnerId: String,
    imageDescription: String,
    photoIds: List<String>,
    selectedPhotoId: String,
    onSelectPhoto: (String) -> Unit,
    onClose: () -> Unit,
    previews: Map<String, ImageBitmap>,
    fullPhotos: MutableMap<String, ImageBitmap>,
    loadPhoto: suspend (String, String) -> ByteArray?,
    onRemovePhoto: ((String) -> Unit)? = null,
    isRemoving: Boolean = false
) {
    var loadingPhoto by remember(imageOwnerId, selectedPhotoId) { mutableStateOf(false) }
    var finishedLoading by remember(imageOwnerId, selectedPhotoId) { mutableStateOf(false) }
    var photoScale by remember(selectedPhotoId) { mutableStateOf(1f) }
    var photoOffset by remember(selectedPhotoId) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (photoScale * zoomChange).coerceIn(1f, 5f)
        photoOffset = if (newScale == 1f) Offset.Zero else photoOffset + panChange
        photoScale = newScale
    }

    LaunchedEffect(imageOwnerId, selectedPhotoId) {
        if (fullPhotos[selectedPhotoId] == null) {
            loadingPhoto = true
            try {
                val bitmap = decodePhoto(loadPhoto(imageOwnerId, selectedPhotoId))
                if (bitmap != null) fullPhotos[selectedPhotoId] = bitmap
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fall back to the smaller preview if the full image is unavailable.
            } finally {
                loadingPhoto = false
                finishedLoading = true
            }
        } else {
            finishedLoading = true
        }
    }

    val position = photoIds.indexOf(selectedPhotoId)
    Dialog(
        onDismissRequest = {
            if (!isRemoving) onClose()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isRemoving,
            dismissOnClickOutside = !isRemoving
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim),
            contentAlignment = Alignment.Center
        ) {
            val image = fullPhotos[selectedPhotoId] ?: previews[selectedPhotoId]
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = imageDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = photoScale
                            scaleY = photoScale
                            translationX = photoOffset.x
                            translationY = photoOffset.y
                        }
                        .transformable(
                            state = transformState,
                            enabled = !isRemoving
                        )
                        .pointerInput(selectedPhotoId, isRemoving) {
                            if (!isRemoving) {
                                detectTapGestures(onDoubleTap = {
                                    photoScale = if (photoScale > 1f) 1f else 2f
                                    photoOffset = Offset.Zero
                                })
                            }
                        }
                )
            } else if (finishedLoading) {
                Text(
                    "Photo unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
            if (loadingPhoto) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            if (photoIds.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "${position + 1} / ${photoIds.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }

            // Hide viewer controls while an image removal operation is running.
            if (!isRemoving) {
                PhotoControlButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close photo viewer",
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )

                if (onRemovePhoto != null) {
                    DangerButton(
                        text = "Remove image",
                        onClick = { onRemovePhoto(selectedPhotoId) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(20.dp)
                    )
                }

                if (photoIds.size > 1 && photoScale == 1f) {
                    if (position > 0) {
                        PhotoControlButton(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous photo",
                            onClick = { onSelectPhoto(photoIds[position - 1]) },
                            modifier = Modifier.align(Alignment.CenterStart).padding(12.dp)
                        )
                    }
                    if (position < photoIds.lastIndex) {
                        PhotoControlButton(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next photo",
                            onClick = { onSelectPhoto(photoIds[position + 1]) },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp)
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun PhotoControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
        shape = CircleShape
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

/**
 * Opens a single image directly in the full screen photo viewer, without displaying a gallery or thumbnail selection.
 *
 * Used by the admin image browser to inspect user profile images. Reuses the existing full screen viewer.
 *
 * The image is loaded from previously retrieved bytes rather than making an additional Firestore request.
 *
 * @param imageId unique identifier of the image
 * @param imageDescription accessibility description of the image
 * @param imageBytes compressed image data to display
 * @param onClose called when the viewer is dismissed
 * @param onRemoveImage optional removal action; if null, the removal button is hidden
 * @param isRemoving whether an image removal operation is in progress
 */
@Composable
fun SingleImageViewer(
    imageId: String,
    imageDescription: String,
    imageBytes: ByteArray,
    onClose: () -> Unit,
    onRemoveImage: (() -> Unit)? = null,
    isRemoving: Boolean = false
) {
    val photoIds = remember(imageId) { listOf(imageId) }
    val fullPhotos = remember(imageId, imageBytes) {
        mutableStateMapOf<String, ImageBitmap>()
    }

    FullPhotoDialog(
        imageOwnerId = imageId,
        imageDescription = imageDescription,
        photoIds = photoIds,
        selectedPhotoId = imageId,
        onSelectPhoto = {},
        onClose = onClose,
        previews = emptyMap(),
        fullPhotos = fullPhotos,
        loadPhoto = { _, _ -> imageBytes },
        onRemovePhoto = onRemoveImage?.let { action ->
            { _: String -> action() }
        },
        isRemoving = isRemoving
    )
}

/** Decodes photo bytes off the main thread to keep the gallery responsive. */
private suspend fun decodePhoto(bytes: ByteArray?): ImageBitmap? =
    withContext(Dispatchers.Default) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

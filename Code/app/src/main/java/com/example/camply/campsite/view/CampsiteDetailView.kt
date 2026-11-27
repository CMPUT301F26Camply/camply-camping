package com.example.camply.campsite.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camply.R
import com.example.camply.campsite.controller.CampsiteDetailController
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch
import kotlin.math.floor
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.camply.shared.view.DestructiveConfirmationDialog

// CampsiteDetailView.kt - Campsite Details Redesign
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT (GPT-6)
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to redesign the campsite details UI using existing Camply components
// and MaterialTheme, display Firestore photo previews and full images, implement
// a zoomable photo viewer, reuse the read only campsite map, and move the owner's
// Edit action next to the campsite name while preserving the lottery content slot.
//
// Starting Prompt:
// "Can you fully redesign CampsiteDetailView.kt so it matches our updated Camply
// screens, shared UI components, and MaterialTheme styling? Make the layout feel
// more polished and organized, showing the campsite name, location, nightly
// price, maximum occupancy, rating, description, amenities, tags, rules, and
// cancellation policy clearly. Fix the photo display because campsite.images
// contains Firestore photo document IDs, not image URLs. Add a gallery that
// loads the smaller photo previews, lets users navigate between campsite photos,
// and opens the full resolution image when tapped, with pinch/double tap zoom,
// and a way to navigate or close the viewer. Display the saved latitude and
// longitude on our existing CampsiteMap in read only mode, allowing users to
// explore and zoom the map without changing the campsite pin. For campsite
// owners move the Edit action to a small icon beside the campsite name instead
// of a button at the bottom while keeping it hidden from other users. Keep
// the existing lottery/entrant content through extraContent, along with the
// loading, error, retry, and Back behavior. Reuse existing models, controllers,
// repository callbacks, components, and theme colors wherever possible rather
// than adding duplicate logic or hardcoded styling."

/**
 * Campsite details (US 01.01.05): name, location, description, maximum occupancy, map location,
 * image, price, tags, rating, rules and cancellation policy.
 *
 * The lottery join/leave button belongs to phase 4 (US 01.01.06, 01.02.01, 01.02.02). It plugs in through
 * [Display]'s `extraContent` slot, so that phase never needs to edit this file's layout.
 * The QR scanner (US 01.12.01) can open this screen with any scanned campsite id.
 */
class CampsiteDetailView(
    private val model: CampsiteDetailModel,
    private val controller: CampsiteDetailController
) {

    /**
     * Displays a campsite's details and promotional photo gallery.
     *
     * @param campsiteId ID of the campsite to display.
     * @param onBack Called when returning to the previous screen.
     * @param extraContent Additional content such as owner or entrant actions.
     * @param loadPhotoPreview Retrieves a smaller Firestore photo preview.
     * @param loadPhoto Retrieves the full photo when the user enlarges it.
     * @param currentUserId Current user ID, used to show Edit only to the owner.
     * @param onEdit Opens the existing owner edit form when the owner taps Edit.
     */
    @Composable
    fun Display(
        campsiteId: String,
        onBack: () -> Unit,
        showQRbutton: Boolean = false,
        extraContent: @Composable (Campsite) -> Unit = {},
        loadPhotoPreview: suspend (String, String) -> ByteArray? = { _, _ -> null },
        loadPhoto: suspend (String, String) -> ByteArray? = { _, _ -> null },
        currentUserId: String? = null,
        onEdit: ((Campsite) -> Unit)? = null,
        onRemove: (suspend (String) -> String?)? = null
    ) {
        val scope = rememberCoroutineScope()
        var confirmRemoval by remember { mutableStateOf(false) }
        var removalError by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(campsiteId) { controller.loadCampsite(campsiteId) }

        if (confirmRemoval) {
            DestructiveConfirmationDialog(
                title = "Remove campsite",
                message = "Remove this campsite? This cannot be undone. Entrants in its lotteries will be notified, and future reservations will be canceled.",
                confirmText = "Remove campsite",
                onConfirm = {
                    confirmRemoval = false
                    val remove = onRemove ?: return@DestructiveConfirmationDialog
                    scope.launch {
                        removalError = remove(campsiteId)
                        if (removalError == null) onBack()
                    }
                },
                onDismiss = { confirmRemoval = false }
            )
        }
        CamplyScreen(
            title = "Campsite details",
            trailing = if (onRemove != null) {
                {
                    IconButton(
                        onClick = { confirmRemoval = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove campsite",
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            } else {
                null
            }
        ) {
            // Avoid displaying details for the previous campsite while a new one loads.
            val campsite = model.campsite?.takeIf { it.id == campsiteId }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    model.isLoading || (model.errorMessage == null && campsite == null) -> Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                    model.errorMessage != null -> {
                        CenteredMessage(model.errorMessage!!, "Try again") {
                            scope.launch { controller.loadCampsite(campsiteId) }
                        }
                    }

                    campsite != null -> {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SummarySection(campsite, currentUserId, onEdit)
                            DescriptionSection(campsite)
                            CampsitePhotoGallery(campsite, loadPhotoPreview, loadPhoto)
                            LocationSection(campsite)
                            extraContent(campsite)
                            RulesSection(campsite)
                            Spacer(Modifier.height(16.dp))
                        }

                        if (showQRbutton) {
                            FloatingActionButton(
                                onClick = { controller.generateQRcode(campsite) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(15.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code),
                                    contentDescription = "Generate QR code"
                                )
                            }
                        }

                        val qrCode = model.qrCode
                        if (qrCode != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { controller.closeQRcode() },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .clickable(enabled = false) {},
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    BackHandler(enabled = true) {
                                        controller.closeQRcode()
                                    }
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = campsite.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Image(
                                            bitmap = qrCode,
                                            contentDescription = "QR code for ${campsite.name}",
                                            modifier = Modifier.size(256.dp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        TextButton(
                                            onClick = { controller.closeQRcode() }
                                        ) {
                                            Text("Close")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SecondaryButton(
                text = "Back",
                onClick = onBack
            )
        }
    }

    @Composable
    private fun SummarySection(
        campsite: Campsite,
        currentUserId: String?,
        onEdit: ((Campsite) -> Unit)?
    ) = OrangeCard {
        val locale = LocalConfiguration.current.locales[0]

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                campsite.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (!currentUserId.isNullOrBlank() && campsite.ownerId == currentUserId && onEdit != null) {
                IconButton(onClick = { onEdit(campsite) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit campsite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        CardLabel(campsite.location)

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Up to ${campsite.maxOccupancy} guests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (campsite.rating > 0.0 && campsite.rating <= 5.0) {
                    val fullStars = floor(campsite.rating).toInt().coerceIn(0, 5)
                    Text(
                        text = "★".repeat(fullStars) + "☆".repeat(5 - fullStars) +
                                "  ${String.format(locale, "%.1f", campsite.rating)} / 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "Not yet rated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val priceText = if (campsite.price % 1.0 == 0.0) {
                    String.format(locale, "%.0f", campsite.price)
                } else {
                    String.format(locale, "%.2f", campsite.price)
                }
                Text(
                    "$$priceText",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "per night",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun DescriptionSection(campsite: Campsite) {
        OrangeCard {
            CardLabel("Description & amenities")
            Spacer(Modifier.height(10.dp))
            Text(
                campsite.description.ifBlank { "No description provided." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (campsite.amenities.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Amenities", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campsite.amenities.forEach { amenity -> DetailChip(amenity) }
                }
            }

            if (campsite.tags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campsite.tags.forEach { tag -> DetailChip(tag, highlighted = true) }
                }
            }
        }
    }

    @Composable
    private fun LocationSection(campsite: Campsite) = OrangeCard {
        CardLabel("Location")
        Spacer(Modifier.height(6.dp))
        Text(
            campsite.location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (campsite.latitude != null && campsite.longitude != null) {
            CampsiteMap(
                latitude = campsite.latitude,
                longitude = campsite.longitude,
                editable = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        } else {
            Text(
                "Map location has not been set for this campsite.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun RulesSection(campsite: Campsite) = OrangeCard {
        CardLabel("Rules & cancellation policy")
        Spacer(Modifier.height(10.dp))
        Text("Campsite rules", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            campsite.rules.ifBlank { "No rules listed." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Text("Cancellation policy", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            campsite.cancellationPolicy.ifBlank { "No cancellation policy listed." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    @Composable
    private fun DetailChip(value: String, highlighted: Boolean = false) {
        Surface(
            color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}
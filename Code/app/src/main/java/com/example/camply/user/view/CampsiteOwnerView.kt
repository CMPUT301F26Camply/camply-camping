package com.example.camply.user.view

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.camply.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.shared.view.DestructiveConfirmationDialog
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.ui.theme.CamplyTheme
import com.example.camply.ui.theme.CamplyThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

// CampsiteOwnerView.kt - Campsite Owner Dashboard Redesign
//
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help redesign the existing campsite owner
// dashboard to match the updated Camply interface.
// This included assistance with improving the dashboard
// layout, creating reusable management action cards,
// updating campsite listings, and applying the new
// Material 3 theme colors and typography.
//
// Prompt:
// "Given all the redesign changes we have made to Camply,
// how can we update CampsiteOwnerView.kt to make the owner
// dashboard consistent with our new theme and reusable UI
// components? I want to improve the layout, replace the
// old background and buttons with a cleaner design,
// and make the campsite cards more modern and readable.
// The reservations and lottery options should be displayed
// as separate management cards with icons. Make sure the
// screen uses our new Material 3 colors and typography,
// while keeping all existing owner functionality working."
//
// Changes from the original implementation:
// - Replaced the original background with the
//   active Material 3 theme background.
// - Updated the owner greeting to display their first name.
// - Redesigned the header with improved spacing and typography.
// - Moved the add campsite button to the upper right corner
//   and updated it to a 52dp themed icon button.
// - Replaced the original reservation and lottery buttons
//   with two reusable OwnerActionCard components.
// - Added icons, titles and subtitles to management cards.
// - Added a "Registered Campsites" section heading.
// - Redesigned campsite cards with themed surfaces,
//   rounded corners and subtle borders.
// - Reduced campsite image sizes and added rounded thumbnails.
// - Updated campsite information to use Material 3 typography
//   and colors for improved readability.
// - Updated the edit campsite button to a compact themed icon.
// - Replaced the original lottery registration button with
//   the shared PrimaryButton component.
// - Added documentation for the updated components.
//
// The existing CampsiteListView class, campsite data,
// and navigation callbacks were retained.
// Changes mainly focused on improving dashboard layout,
// usability and consistency with the Camply redesign.

/**
 * This class displays the list of all campsites that are registered
 * by a campsite owner
 *
 * Covering US 02.01.04 and US 02.03.01
 */
class CampsiteListView {

    /**
     * Displays the campsite owner's registered campsites.
     *
     * Provides access to campsite creation, editing,
     * reservations, and lottery management.
     *
     * @param ownerName Name of the campsite owner.
     * @param campsites Campsites registered by the owner.
     * @param addCampsite Called when adding a campsite.
     * @param editCampsite Called when editing a campsite.
     * @param viewCampsite Called when viewing campsite details.
     * @param lotteryRegistration Called when listing a campsite in a lottery.
     * @param reservationList Called when viewing reservations.
     * @param viewOrCreateLottery Called when managing lotteries.
     * @param loadPhotoPreview Loads the Firestore preview image bytes for a given campsite ID and photo ID, or null if unavailable.
     * @param readOnly Hides creation, editing, reservations, and lottery controls. Used when an admin is viewing another owner's campsites.
     * @param onBack Returns to the previous screen. The Back button is shown only when this is provided.
     */
    @Composable
    fun Display(
        ownerName: String,
        campsites: List<Campsite>,
        addCampsite: () -> Unit,
        editCampsite: (Campsite) -> Unit,
        viewCampsite: (Campsite) -> Unit,
        lotteryRegistration: (Campsite) -> Unit,
        reservationList: () -> Unit,
        viewOrCreateLottery: () -> Unit,
        loadPhotoPreview: suspend (String, String) -> ByteArray?,
        // Admin browse reuses this page. readOnly hides create, edit, and lottery controls
        // because an admin-only account is looking at someone else's campsites.
        readOnly: Boolean = false,
        onBack: (()-> Unit)?=null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Owner header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val firstName = ownerName.trim().substringBefore(" ").ifBlank { "Owner" }
                    Text(
                        text = "$firstName's Campsites",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )


                    Text(
                        text = "Manage campsite listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!readOnly) {
                    IconButton(
                        onClick = addCampsite,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add campsite",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if(!readOnly){
            // Owner management actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OwnerActionCard(
                    title = "Reservations",
                    subtitle = "View reservation list",
                    iconRes = R.drawable.reservation_icon,
                    onClick = reservationList,
                    modifier = Modifier.weight(1f)
                )

                OwnerActionCard(
                    title = "Lotteries",
                    subtitle = "Create / View Lottery",
                    iconRes = R.drawable.lottery_icon,
                    onClick = viewOrCreateLottery,
                    modifier = Modifier.weight(1f)
                )
            }
                }


            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Registered Campsites",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campsite list
            if (campsites.isEmpty()) {
                Text(
                    text = if (readOnly) {
                        "This owner has no campsites."
                    } else {
                        "No Registered Campsites. Press the '+' button to add campsites"
                    },                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(campsites, key = { it.id }) { campsite ->
                        CampsiteCard(
                            campsite = campsite,
                            viewInfo = viewCampsite,
                            onEdit = editCampsite,
                            addInLottery = lotteryRegistration,
                            loadPhotoPreview = loadPhotoPreview,
                            readOnly = readOnly
                        )
                    }

                }
            }
            if (onBack != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryButton(
                    text = "Back",
                    onClick = onBack
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

        }
    }


    /**
     * Displays a campsite belonging to the current owner.
     *
     * Allows the owner to view details, edit the campsite,
     * or list it in a lottery.
     *
     * @param campsite Campsite displayed in the card.
     * @param viewInfo Called when the card is selected.
     * @param onEdit Called when the edit icon is selected.
     * @param addInLottery Called when listing the campsite in a lottery.
     * @param loadPhotoPreview Loads the Firestore preview image bytes for a given campsite ID and photo ID, or null if unavailable.
     */
    @Composable
    private fun CampsiteCard(
        campsite: Campsite,
        viewInfo: (Campsite) -> Unit,
        onEdit: (Campsite) -> Unit,
        addInLottery: (Campsite) -> Unit,
        loadPhotoPreview: suspend (String, String) -> ByteArray?,
        readOnly: Boolean =false

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable { viewInfo(campsite) }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Load the cover photo's smaller Firestore preview.
                val photoId = campsite.images.firstOrNull()

                var previewImage by remember(campsite.id, photoId) {
                    mutableStateOf<ImageBitmap?>(null)
                }

                LaunchedEffect(campsite.id, photoId) {
                    if (photoId != null && campsite.id.isNotBlank()) {
                        try {
                            val bytes = loadPhotoPreview(campsite.id, photoId)

                            previewImage = withContext(Dispatchers.Default) {
                                bytes?.let {
                                    BitmapFactory.decodeByteArray(
                                        it, 0, it.size
                                    )?.asImageBitmap()
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            previewImage = null
                        }
                    }
                }

                val image = previewImage

                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Photo of ${campsite.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 104.dp, height = 112.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Campsite image",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(width = 104.dp, height = 112.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(30.dp)
                    )
                }

                // Campsite details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = campsite.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = campsite.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = campsite.amenities.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Max occupancy: ${campsite.maxOccupancy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!readOnly) {
                    // Edit campsite
                    IconButton(
                        onClick = { onEdit(campsite) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${campsite.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (!readOnly) {
                // Lottery registration
                PrimaryButton(
                    text = "List Campsite in Lottery",
                    onClick = { addInLottery(campsite) }
                )
            }
        }
    }


    /**
     * Displays a clickable management action on the owner dashboard.
     *
     * Uses the active Camply theme for its icon, background,
     * border, and typography.
     *
     * @param title Main action title.
     * @param subtitle Description displayed below the title.
     * @param iconRes Drawable resource used for the action icon.
     * @param onClick Called when the action is selected.
     * @param modifier Modifier applied to the card.
     */
    @Composable
    private fun OwnerActionCard(
        title: String,
        subtitle: String,
        iconRes: Int,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.heightIn(min = 140.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp,Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // This function covers the US 02.06.01.
    // Using the updated UI style
    @Composable
    fun BookedDatesView(campsite: Campsite, bookedDates: List<DateRange>) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { //section for dates booked. Using the DateRange class
            Text(
                text = "Booked Dates",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (bookedDates.isEmpty()) {
                Text(
                    text = "No Booked Dates for this campsite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            bookedDates.forEach { range ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Booked from ${range.from} to ${range.to}",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Text( //section to show all available dates
                text = "Available dates",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (campsite.availability.isEmpty()) {
                Text(
                    text = "No availability dates have been set for this campsite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            campsite.availability.forEach { range ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Dates Still Available from ${range.from} to ${range.to}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

}



@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteListViewPreview() {
    CamplyTheme(
        preset = CamplyThemePreset.ORANGE,
        darkTheme = false
    ) {

        CampsiteListView().Display(
            ownerName = "Owner 1",
            campsites = listOf(
                Campsite(
                    id = "a",
                    name = "Campsite A",
                    location = "Location, A",
                    amenities = listOf("Fireplace", "Trash disposal"),
                    maxOccupancy = 8,
                    rules = "Quiet hours 9pm"
                ),
                Campsite(
                    id = "b",
                    name = "Campsite B",
                    location = "Location, B",
                    amenities = listOf("Washrooms"),
                    maxOccupancy = 5,
                    rules = "No pets"
                ),
                Campsite(
                    id = "c",
                    name = "Campsite C",
                    location = "Location C",
                    amenities = listOf("Picnic Table"),
                    maxOccupancy = 10,
                    rules = "No loud music"
                )
            ),
            addCampsite = {},
            editCampsite = {},
            viewCampsite = {},
            lotteryRegistration = {},
            reservationList = {},
            viewOrCreateLottery = {},
            loadPhotoPreview = { _, _ -> null }
        )
    }
}
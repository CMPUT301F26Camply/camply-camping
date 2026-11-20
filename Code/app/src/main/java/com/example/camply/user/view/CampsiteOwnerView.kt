package com.example.camply.user.view

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.camply.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.ui.theme.CamplyTheme
import com.example.camply.ui.theme.CamplyThemePreset

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
        viewOrCreateLottery: () -> Unit
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
                        text = "Manage your campsite listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                    text = "No Registered Campsites. Press the '+' button to add campsites",
                    style = MaterialTheme.typography.bodyMedium,
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
                            addInLottery = lotteryRegistration
                        )
                    }
                }
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
     */
    @Composable
    private fun CampsiteCard(
        campsite: Campsite,
        viewInfo: (Campsite) -> Unit,
        onEdit: (Campsite) -> Unit,
        addInLottery: (Campsite) -> Unit
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
                // Campsite image or placeholder
                val image = campsite.images.firstOrNull()

                if (image.isNullOrBlank()) {
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
                } else {
                    AsyncImage(
                        model = image,
                        contentDescription = "photo of campsite",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 104.dp, height = 112.dp)
                            .clip(RoundedCornerShape(12.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

            // Lottery registration
            PrimaryButton(
                text = "List Campsite in Lottery",
                onClick = { addInLottery(campsite) }
            )
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
            viewOrCreateLottery = {}
        )
    }
}
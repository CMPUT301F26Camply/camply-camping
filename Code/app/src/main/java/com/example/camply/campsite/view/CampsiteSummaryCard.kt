package com.example.camply.campsite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.OrangeCard


// CampsiteSummaryCard.kt - Campsite Card UI Redesign
//
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help redesign the existing campsite summary
// cards to match the updated Camply interface.
// This included assistance with displaying campsite images,
// adding a placeholder for missing images, improving layout
// spacing, and applying the new Material 3 theme colors
// and typography.
//
// Prompt:
// "Given all the redesign changes we have made to Camply,
// how can we update CampsiteSummaryCard.kt to make the
// campsite cards more modern and consistent with our new
// theme and reusable UI components? I want to display a
// campsite image at the top of each card, improve the
// layout and spacing of campsite information, and use
// our new Material 3 colors and typography. If a campsite
// has no image, display a placeholder instead. Keep the
// existing campsite selection functionality."
//
// Changes from the original implementation:
// - Redesigned the original compact, text only campsite card.
// - Added AsyncImage using Coil to display campsite images.
// - Added a placeholder with a home icon when no image exists.
// - Added rounded corners and a 160dp image display area.
// - Updated campsite name, location, occupancy and rating
//   to use Material 3 theme colors and typography.
// - Separated the location and guest count into individual
//   text elements for improved readability.
// - Increased spacing between elements for a cleaner layout.
// - Added documentation for the Display function.
//
// The existing CampsiteSummaryCard class, OrangeCard
// component, campsite information and onClick callback
// were retained. Changes mainly focused on improving the
// appearance of campsite cards and consistency with the
// redesigned Camply theme.

/**
 * One campsite summary row: name, location, maximum occupancy, rating (US 01.01.04).
 * Stateless, so other screens (owner list, admin browse) can reuse it.
 */
class CampsiteSummaryCard {

    /**
     * Displays the campsite information and handles selection.
     *
     * @param campsite The campsite being displayed.
     * @param onClick Called when the campsite is selected.
     */
    @Composable
    fun Display(
        campsite: Campsite,
        onClick: () -> Unit
    ) {
        OrangeCard(
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            val image = campsite.images.firstOrNull()

            // Campsite image or placeholder
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = "Photo of ${campsite.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            CardLabel("Campsite")

            Spacer(Modifier.height(4.dp))

            Text(
                text = campsite.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = campsite.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Up to ${campsite.maxOccupancy} guests",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Rating ${"%.1f".format(campsite.rating)} / 5",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

}
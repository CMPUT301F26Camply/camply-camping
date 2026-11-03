package com.example.camply.campsite.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.OrangeCard

/**
 * One campsite summary row: name, location, maximum occupancy, rating (US 01.01.04).
 * Stateless, so other screens (owner list, admin browse) can reuse it.
 */
class CampsiteSummaryCard {

    @Composable
    fun Display(campsite: Campsite, onClick: () -> Unit) {
        OrangeCard(Modifier.clickable(onClick = onClick)) {
            CardLabel("Campsite")
            Text(campsite.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CamplyPalette.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "${campsite.location} · Up to ${campsite.maxOccupancy} guests",
                fontSize = 13.sp, color = CamplyPalette.Ink
            )
            Text("Rating ${"%.1f".format(campsite.rating)} / 5", fontSize = 13.sp, color = CamplyPalette.Ink)
        }
    }
}
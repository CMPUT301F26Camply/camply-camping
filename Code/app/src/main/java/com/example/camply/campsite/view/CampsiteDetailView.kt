package com.example.camply.campsite.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camply.campsite.controller.CampsiteDetailController
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch
import androidx.core.net.toUri

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

    @Composable
    fun Display(
        campsiteId: String,
        onBack: () -> Unit,
        extraContent: @Composable (Campsite) -> Unit = {}
    ) {
        val scope = rememberCoroutineScope()
        LaunchedEffect(campsiteId) { controller.loadCampsite(campsiteId) }

        CamplyScreen(title = "Campsite details") {
            val campsite = model.campsite
            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color.White) }

                model.errorMessage != null -> {
                    CenteredMessage(model.errorMessage!!, "Try again") {
                        scope.launch { controller.loadCampsite(campsiteId) }
                    }
                    SecondaryButton("Back", onBack)
                }

                campsite != null -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CampsiteImage(campsite)
                    SummarySection(campsite)
                    DescriptionSection(campsite)
                    RulesSection(campsite)
                    extraContent(campsite)
                    SecondaryButton("Back", onBack)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun CampsiteImage(campsite: Campsite) {
        if (campsite.imageUrl.isNullOrBlank()) return
        AsyncImage(
            model = campsite.imageUrl,
            contentDescription = "Photo of ${campsite.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
        )
    }

    @Composable
    private fun SummarySection(campsite: Campsite) = OrangeCard {
        CardLabel(campsite.location)
        Text(campsite.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = CamplyPalette.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Up to ${campsite.maxOccupancy} guests · Rating ${"%.1f".format(campsite.rating)} / 5",
            fontSize = 13.sp, color = CamplyPalette.Ink
        )
        Text("$${"%.0f".format(campsite.price)} / night", fontSize = 13.sp, color = CamplyPalette.Ink)
        if (campsite.tags.isNotEmpty()) {
            Text(campsite.tags.joinToString(" · "), fontSize = 12.sp, color = CamplyPalette.Ink)
        }
    }

    @Composable
    private fun DescriptionSection(campsite: Campsite) {
        val context = LocalContext.current
        OrangeCard {
            CardLabel("Description & amenities")
            Text(
                campsite.description.ifBlank { "No description provided." },
                fontSize = 14.sp, color = CamplyPalette.Ink
            )
            val lat = campsite.latitude
            val lng = campsite.longitude
            if (lat != null && lng != null) {
                Spacer(Modifier.height(8.dp))
                CardLabel("Map location")
                Text("%.4f, %.4f".format(lat, lng), fontSize = 13.sp, color = CamplyPalette.Ink)
                Spacer(Modifier.height(6.dp))
                SecondaryButton("Open in Maps", {
                    val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(campsite.name)})".toUri()
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                })
            }
        }
    }

    @Composable
    private fun RulesSection(campsite: Campsite) = OrangeCard {
        CardLabel("Rules & cancellation policy")
        Text(campsite.rules.ifBlank { "No rules listed." }, fontSize = 14.sp, color = CamplyPalette.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            campsite.cancellationPolicy.ifBlank { "No cancellation policy listed." },
            fontSize = 14.sp, color = CamplyPalette.Ink
        )
    }
}
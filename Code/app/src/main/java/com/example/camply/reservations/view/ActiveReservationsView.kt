package com.example.camply.reservations.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.reservations.controller.ActiveReservationsController
import com.example.camply.reservations.model.ActiveReservationItem
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import kotlinx.coroutines.launch

/**
 * Displays the entrant's active reservations (US 01.06.01).
 *
 * @param model The model containing the screen state.
 * @param controller The controller used to load active reservations.
 */
class ActiveReservationsView(
    private val model: ActiveReservationsModel,
    private val controller: ActiveReservationsController
) {

    /**
     * Loads and displays an entrant's active reservations.
     *
     * @param entrantId The entrant's Firebase user ID.
     */
    @Composable
    fun Display(entrantId: String) {
        val scope = rememberCoroutineScope()
        val error = model.errorMessage

        LaunchedEffect(entrantId) { controller.loadReservations(entrantId) }

        CamplyScreen(title = "My Reservations") {
            // Loading and errors come first
            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }

                error != null -> CenteredMessage(error, "Try again") {
                    scope.launch { controller.loadReservations(entrantId) }
                }

                model.reservations.isEmpty() -> CenteredMessage("You have no active reservations.")

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(model.reservations, key = { it.reservation.id }) { item ->
                        ReservationCard(item)
                    }
                }
            }
        }
    }

    // Shows one reservation's campsite, dates, and status.
    @Composable
    private fun ReservationCard(item: ActiveReservationItem) = OrangeCard {
        val dates = item.reservation.dates

        Text(
            item.campsite?.name ?: "Campsite unavailable",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CamplyPalette.Ink
        )

        item.campsite?.let { Text(it.location, fontSize = 13.sp, color = CamplyPalette.Ink) }

        Text("${dates.from} to ${dates.to}", fontSize = 13.sp, color = CamplyPalette.Ink)
        Text("Active", fontSize = 13.sp, color = CamplyPalette.Ink)
    }
}

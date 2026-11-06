package com.example.camply.reservations.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.campsite.model.Campsite
import com.example.camply.reservations.controller.OwnerReservationsController
import com.example.camply.reservations.model.OwnerCampsiteReservations
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.FilterChips
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch

/**
 * Displays the owner's campsites and their reservations.
 *
 * US 02.07.01.
 */
class OwnerReservationsView(
    private val model: OwnerReservationsModel,
    private val controller: OwnerReservationsController
) {

    /**
     * Displays the owner's reservation management screen.
     *
     * @param ownerId The current owner's user ID.
     * @param onBack Called when returning to the owner campsite screen.
     */
    @Composable
    fun Display(ownerId: String, onBack: () -> Unit) {
        val scope = rememberCoroutineScope()

        LaunchedEffect(ownerId) {
            controller.loadAllReservations(ownerId)
        }

        BackHandler {
            if (model.selectedCampsiteId == null) {
                onBack()
            } else {
                controller.onBack()
            }
        }

        val selectedCampsite = model.campsites.firstOrNull {
            it.campsite.id == model.selectedCampsiteId
        }

        val title = when {
            model.selectedReservation != null -> "Reservation Details"
            selectedCampsite != null -> selectedCampsite.campsite.name
            else -> "Owner Reservations"
        }

        CamplyScreen(title = title) {
            when {
                model.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                model.errorMessage != null -> {
                    CenteredMessage(
                        model.errorMessage ?: "Could not load reservations.",
                        "Try again"
                    ) {
                        scope.launch {
                            controller.loadAllReservations(ownerId)
                        }
                    }
                }

                model.selectedReservation != null -> {
                    ReservationDetails(
                        model.selectedReservation!!,
                        selectedCampsite?.campsite
                    )
                }

                selectedCampsite == null -> {
                    CampsitesWithReservationsList()

                    Spacer(Modifier.height(12.dp))

                    SecondaryButton(
                        text = "Back",
                        onClick = onBack
                    )
                }

                else -> {
                    FilterChips(
                        categories = listOf(
                            "All", "Active", "Past", "Cancelled", "Rejected"
                        ),
                        selectedCategories = if (model.selectedFilters.isEmpty()) {
                            setOf("All")
                        } else {
                            model.selectedFilters
                        },
                        onCategoryToggle = controller::onFilterSelected
                    )

                    Spacer(Modifier.height(12.dp))

                    ReservationList(selectedCampsite)

                    Spacer(Modifier.height(12.dp))

                    SecondaryButton(
                        text = "Back to Campsites",
                        onClick = controller::onBack
                    )
                }
            }
        }
    }

    /**
     * Displays owned campsites that have reservations,
     * including their active and total reservation counts.
     */
    @Composable
    private fun ColumnScope.CampsitesWithReservationsList() {
        val campsites = controller.getCampsitesWithReservations()

        if (campsites.isEmpty()) {
            if (model.campsites.isEmpty()) {
                CenteredMessage(
                    "You have no registered campsites."
                )

                Text(
                    "Register a campsite to start managing its reservations.",
                    fontSize = 14.sp,
                    color = CamplyPalette.Cream,
                    textAlign = TextAlign.Center
                )
            } else {
                CenteredMessage(
                    "None of your campsites have reservations."
                )

                Text(
                    "Reservations will appear here once entrants complete their bookings.",
                    fontSize = 14.sp,
                    color = CamplyPalette.Cream,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(campsites, key = { it.campsite.id }) { entry ->
                    OrangeCard(
                        Modifier.clickable {
                            controller.onCampsiteSelected(entry.campsite.id)
                        }
                    ) {
                        Text(
                            entry.campsite.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CamplyPalette.Ink
                        )

                        Text(
                            entry.campsite.location,
                            fontSize = 13.sp,
                            color = CamplyPalette.Ink
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Active reservations: ${controller.getActiveCount(entry)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CamplyPalette.Ink
                        )

                        Text(
                            "Total reservations: ${entry.reservations.size}",
                            fontSize = 13.sp,
                            color = CamplyPalette.Ink
                        )
                    }
                }
            }
        }
    }

    /**
     * Displays reservations belonging to the selected campsite.
     *
     * @param entry The selected campsite and its reservations.
     */
    @Composable
    private fun ColumnScope.ReservationList(entry: OwnerCampsiteReservations) {
        val reservations = controller.getFilteredReservations(entry.reservations)

        if (reservations.isEmpty()) {
            CenteredMessage("No reservations match the selected filters.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservations, key = { it.id }) { reservation ->
                    OrangeCard(
                        Modifier.clickable {
                            controller.onReservationSelected(reservation)
                        }
                    ) {
                        ReservationSummary(reservation)
                    }
                }
            }
        }
    }

    /**
     * Displays the entrant, stay dates, and reservation status.
     *
     * @param reservation The reservation to summarize.
     */
    @Composable
    private fun ReservationSummary(reservation: Reservation) {
        val category = controller.getReservationCategory(reservation)

        val statusColor = when (category) {
            "Active" -> Color(0xFF1B5E20)
            "Past" -> Color.Gray
            "Cancelled", "Rejected" -> Color.Red
            else -> CamplyPalette.Ink
        }

        Text(
            "Entrant: ${reservation.entrantId}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CamplyPalette.Ink
        )

        Text(
            "${reservation.dates.from} to ${reservation.dates.to}",
            fontSize = 13.sp,
            color = CamplyPalette.Ink
        )

        Text(
            category,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }

    /**
     * Displays information about the selected reservation.
     *
     * @param reservation The selected reservation.
     * @param campsite The campsite associated with the reservation.
     */
    @Composable
    private fun ColumnScope.ReservationDetails(
        reservation: Reservation,
        campsite: Campsite?
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {            OrangeCard {
                Text(
                    "Campsite",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CamplyPalette.Ink
                )

                Text(
                    campsite?.name ?: "Campsite unavailable",
                    color = CamplyPalette.Ink
                )

                campsite?.let {
                    Text(
                        it.location,
                        color = CamplyPalette.Ink
                    )
                }
            }

            OrangeCard {
                Text(
                    "Reservation information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CamplyPalette.Ink
                )

                Spacer(Modifier.height(8.dp))

                ReservationSummary(reservation)

                Spacer(Modifier.height(8.dp))

                Text(
                    "Reservation ID: ${reservation.id}",
                    fontSize = 12.sp,
                    color = CamplyPalette.Ink
                )
            }

            campsite?.let {
                OrangeCard {
                    Text(
                        "Rules & cancellation policy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CamplyPalette.Ink
                    )

                    Text(
                        it.rules,
                        color = CamplyPalette.Ink
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        it.cancellationPolicy,
                        color = CamplyPalette.Ink
                    )
                }
            }
        }

        SecondaryButton(
            text = "Back to Reservations",
            onClick = controller::onBack
        )
    }
}

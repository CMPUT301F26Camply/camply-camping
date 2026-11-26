package com.example.camply.reservations.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.reservations.controller.ActiveReservationsController
import com.example.camply.reservations.model.ActiveReservationItem
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.shared.view.FilterChips
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.DangerButton
import com.example.camply.shared.view.DestructiveConfirmationDialog
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch

/**
 * Displays, filters and allows cancellation of the entrant's reservations (US 01.06.01, US 01.06.02, US 01.07.01).
 *
 * @param model The model containing the screen state.
 * @param controller The controller used to load, filter and cancel reservations.
 */
class ActiveReservationsView(
    private val model: ActiveReservationsModel,
    private val controller: ActiveReservationsController
) {

    /**
     * Loads and displays an entrant's reservations, or the selected reservation's details.
     *
     * @param entrantId The entrant's Firebase user ID.
     * @param onMessageOwner Opens the selected reservation's conversation.
     */
    @Composable
    fun Display(entrantId: String, onMessageOwner: ((String) -> Unit)? = null) {
        val scope = rememberCoroutineScope()
        val error = model.errorMessage
        val selected = model.selectedReservation

        LaunchedEffect(entrantId) { controller.loadReservations(entrantId) }

        CamplyScreen(title = if (selected != null) "Reservation details" else "My Reservations") {
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

                selected != null -> ReservationDetails(selected, entrantId, onMessageOwner)


                else -> {
                    FilterChips(
                        categories = listOf("All", "Active", "Past", "Cancelled", "Rejected"),
                        selectedCategories =
                            if (model.selectedFilters.isEmpty()) {
                                setOf("All")
                            } else {
                                model.selectedFilters
                            },
                        onCategoryToggle = controller::onFilterSelected
                    )


                    Spacer(Modifier.height(12.dp))

                    when {
                        model.allReservations.isEmpty() ->
                            CenteredMessage("You have no reservations.")

                        model.filteredReservations.isEmpty() ->
                            CenteredMessage("No reservations match the selected filters.")

                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(model.filteredReservations, key = { it.reservation.id }) { item ->
                                ReservationCard(item)
                            }
                        }
                    }
                }

            }
        }
    }

    // Shows one reservation's campsite, dates, and status.
    @Composable
    private fun ReservationCard(item: ActiveReservationItem) =
        OrangeCard(Modifier.clickable { controller.onReservationSelected(item) }) {
            ReservationSummary(item)
        }

    // Shows one reservation's full details, with the campsite's rules and policy if it still exists.
    @Composable
    private fun ReservationDetails(
        item: ActiveReservationItem,
        entrantId: String,
        onMessageOwner: ((String) -> Unit)?
    ) {

        val scope = rememberCoroutineScope()
        var showCancellationDialog by remember(item.reservation.id) { mutableStateOf(false) }

        BackHandler { if (!model.isCancelling) controller.onBackToList() }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrangeCard { ReservationSummary(item) }

            if (onMessageOwner != null && item.reservation.id.isNotBlank() &&
                item.reservation.entrantId == entrantId && !model.isCancelling
            ) {
                SecondaryButton("Message owner", { onMessageOwner(item.reservation.id) })
            }

            item.campsite?.let { campsite ->
                OrangeCard {
                    CardLabel("Rules & cancellation policy")
                    Text(campsite.rules, fontSize = 14.sp, color = CamplyPalette.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(campsite.cancellationPolicy, fontSize = 14.sp, color = CamplyPalette.Ink)
                }
            }

            model.cancellationError?.let { ErrorMessage(it) }

            if (controller.getReservationCategory(item.reservation) == "Active") {
                if (model.isCancelling) {
                    Box(
                        Modifier.fillMaxWidth().padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    DangerButton("Cancel reservation", {
                        showCancellationDialog = true
                    })
                }
            }

            if (!model.isCancelling) {
                SecondaryButton("Back", controller::onBackToList)
            }
        }

        if (showCancellationDialog) {
            DestructiveConfirmationDialog(
                title = "Cancel reservation?",
                message = "Are you sure you want to cancel this reservation? This action cannot be undone.",
                confirmText = "Cancel reservation",
                dismissText = "Keep reservation",
                onConfirm = {
                    showCancellationDialog = false
                    scope.launch { controller.cancelReservation(entrantId) }
                },
                onDismiss = {
                    showCancellationDialog = false
                }
            )
        }
    }

    // Shows the campsite, dates, and status used by both the list and the details.
    @Composable
    private fun ReservationSummary(item: ActiveReservationItem) {
        val dates = item.reservation.dates

        Text(
            item.campsite?.name ?: "Campsite unavailable",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CamplyPalette.Ink
        )

        item.campsite?.let { Text(it.location, fontSize = 13.sp, color = CamplyPalette.Ink) }


        Text("${dates.from} to ${dates.to}", fontSize = 13.sp, color = CamplyPalette.Ink)

        val category = controller.getReservationCategory(item.reservation)

        val statusColor = when (category) {
            "Active" -> Color(0xFF1B5E20)
            "Past" -> Color.Gray
            "Cancelled", "Rejected" -> Color.Red
            else -> CamplyPalette.Ink
        }

        Text(
            category,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )

    }
}

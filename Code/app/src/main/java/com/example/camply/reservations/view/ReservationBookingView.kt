package com.example.camply.reservations.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.controller.ReservationBookingController
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import kotlinx.coroutines.launch

/**
 * Displays the campsite reservation date selection screen.
 *
 * @param model The model containing the booking screen state.
 * @param controller The controller used to validate reservation dates.
 */
class ReservationBookingView(
    private val model: ReservationBookingModel,
    private val controller: ReservationBookingController
) {

    /**
     * Allows an entrant to select reservation dates from a campsite's availability.
     *
     * @param campsite The campsite being reserved.
     * @param onBack Returns to the previous screen.
     */
    @Composable
    fun Display(campsite: Campsite, onBack: () -> Unit) {
        val scope = rememberCoroutineScope()
        var startDate by remember(campsite.id) { mutableStateOf("") }
        var endDate by remember(campsite.id) { mutableStateOf("") }
        var checkedDates by remember(campsite.id) { mutableStateOf<DateRange?>(null) }
        val selectedDates = DateRange(startDate, endDate)
        val showingResult = checkedDates == selectedDates
        var rulesAcknowledged by remember(campsite.id) { mutableStateOf(false) }


        CamplyScreen(title = "Book Campsite") {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OrangeCard {
                    Text(campsite.name, color = CamplyPalette.Ink)
                    Spacer(Modifier.height(12.dp))

                    Text("Available dates", color = CamplyPalette.Ink)

                    if (campsite.availability.isEmpty()) {
                        Text("No available dates.", color = CamplyPalette.Ink)
                    } else {
                        for (dates in campsite.availability) {
                            Text("${dates.from} to ${dates.to}", color = CamplyPalette.Ink)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OrangeCard {
                    Text("Select your dates", color = CamplyPalette.Ink)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start date (yyyy-MM-dd)") },
                        colors = camplyTextFieldColors()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End date (yyyy-MM-dd)") },
                        colors = camplyTextFieldColors()
                    )
                }

                Spacer(Modifier.height(16.dp))

                OrangeCard {
                    Text("Campsite Rules & Policies", color = CamplyPalette.Ink)
                    Spacer(Modifier.height(8.dp))

                    if (campsite.rules.isNotBlank()) {
                        Text(campsite.rules, color = CamplyPalette.Ink)
                    } else {
                        Text("Campsite rules are not available.", color = CamplyPalette.Ink)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(campsite.cancellationPolicy, color = CamplyPalette.Ink)

                    Spacer(Modifier.height(8.dp))

                    Row {
                        Checkbox(
                            checked = rulesAcknowledged,
                            onCheckedChange = { rulesAcknowledged = it },
                            enabled = campsite.rules.isNotBlank()
                        )

                        Text(
                            "I have read and agree to the campsite rules and policies.",
                            color = CamplyPalette.Ink
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                PrimaryButton(
                    text = "Check Dates",
                    onClick = {
                        checkedDates = selectedDates
                        scope.launch { controller.checkDates(campsite, selectedDates) }
                    },
                    enabled = !model.isLoading && startDate.isNotBlank() && endDate.isNotBlank()
                )

                if (showingResult && model.isLoading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                }

                if (showingResult && model.errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(model.errorMessage ?: "", color = CamplyPalette.Ink)
                }

                if (showingResult && model.datesValid) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Selected dates are available. Reservation is not yet confirmed.",
                        color = CamplyPalette.Ink
                    )
                }

                Spacer(Modifier.height(8.dp))

                SecondaryButton("Back", onBack)
            }
        }
    }
}
package com.example.camply.campsite.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.campsite.controller.CampsiteBrowseController
import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors

/** The filter table: location, dates, guests, Apply (US 01.01.02). Hidden until the Filter button is pressed. */
class CampsiteFilterPanel(
    private val model: CampsiteBrowseModel,
    private val controller: CampsiteBrowseController
) {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Display() {
        val draft = model.filterDraft
        OrangeCard {
            CardLabel("Filter")
            Spacer(Modifier.height(6.dp))

            Text("Location", fontWeight = FontWeight.Medium, color = CamplyPalette.Ink)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(model.locations) { location ->
                    val selected = draft.location == location
                    FilterChip(
                        selected = selected,
                        onClick = { controller.onLocationSelected(if (selected) null else location) },
                        label = { Text(location) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CamplyPalette.Cream,
                            selectedContainerColor = CamplyPalette.Black,
                            labelColor = CamplyPalette.Ink,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Date (YYYY-MM-DD)", fontWeight = FontWeight.Medium, color = CamplyPalette.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.dateFrom, onValueChange = controller::onDateFromChanged,
                    placeholder = { Text("From") }, singleLine = true,
                    colors = camplyTextFieldColors(), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.dateTo, onValueChange = controller::onDateToChanged,
                    placeholder = { Text("To") }, singleLine = true,
                    colors = camplyTextFieldColors(), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Number of guests", fontWeight = FontWeight.Medium, color = CamplyPalette.Ink)
            OutlinedTextField(
                value = draft.guests, onValueChange = controller::onGuestsChanged,
                placeholder = { Text("e.g. 4") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = camplyTextFieldColors(), modifier = Modifier.fillMaxWidth()
            )

            model.filterError?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = CamplyPalette.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            PrimaryButton("Apply", controller::onApplyFilter)
            Spacer(Modifier.height(6.dp))
            SecondaryButton("Clear filters", controller::onClearFilters)
        }
    }
}
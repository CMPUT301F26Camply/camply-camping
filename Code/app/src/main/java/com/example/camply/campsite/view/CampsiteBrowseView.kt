package com.example.camply.campsite.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.campsite.controller.CampsiteBrowseController
import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.camplyTextFieldColors
import kotlinx.coroutines.launch

/**
 * Explore campsites screen: search with dropdown (US 01.01.01), filter panel (01.01.02),
 * filtered list (01.01.03) of summary cards (01.01.04).
 */
class CampsiteBrowseView(
    private val model: CampsiteBrowseModel,
    private val controller: CampsiteBrowseController
) {
    private val filterPanel = CampsiteFilterPanel(model, controller)
    private val summaryCard = CampsiteSummaryCard()

    /** [onCampsiteClick] is called with the campsite id when a dropdown row or list card is tapped. */
    @Composable
    fun Display(onCampsiteClick: (String) -> Unit) {
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) { controller.loadCampsites() }

        CamplyScreen(title = "Explore campsites") {
            SearchRow(onCampsiteClick)

            if (model.isFilterPanelVisible) {
                Spacer(Modifier.height(8.dp))
                filterPanel.Display()
            }

            Spacer(Modifier.height(12.dp))

            when {
                model.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color.White) }

                model.errorMessage != null -> CenteredMessage(
                    model.errorMessage!!, "Try again"
                ) { scope.launch { controller.loadCampsites() } }

                model.results.isEmpty() -> CenteredMessage(
                    "No campsites match your search.",
                    if (!model.appliedFilter.isEmpty) "Clear filters" else null,
                    controller::onClearFilters
                )

                else -> {
                    val count = model.results.size
                    Text(
                        if (count == 1) "1 campsite matches your search" else "$count campsites match your search",
                        color = Color.White, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(model.results, key = { it.id }) { campsite ->
                            summaryCard.Display(campsite) { onCampsiteClick(campsite.id) }
                        }
                    }
                }
            }
        }
    }

    /**US 01.01.01: search text field plus the dropdown of matching names. Tapping a dropdown row opens that campsite's details page.*/
    @Composable
    private fun SearchRow(onCampsiteClick: (String) -> Unit) {
        var dropdownOpen by remember { mutableStateOf(false) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = model.searchQuery,
                onValueChange = { controller.onSearchQueryChanged(it); dropdownOpen = true },
                placeholder = { Text("Search campsites") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = camplyTextFieldColors(),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            val filtersActive = !model.appliedFilter.isEmpty
            //US 01.01.02: the Filter button that shows or hides the filter panel
            FilledTonalButton(
                onClick = controller::onFilterButtonClicked,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (filtersActive) CamplyPalette.Orange else CamplyPalette.Cream,
                    contentColor = CamplyPalette.Ink
                )
            ) { Text(if (filtersActive) "Filter •" else "Filter") }
        }

        if (dropdownOpen && model.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CamplyPalette.Cream),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(model.suggestions.take(8), key = { it.id }) { campsite ->
                        Text(
                            text = "${campsite.name} · ${campsite.location}",
                            color = CamplyPalette.Ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownOpen = false; onCampsiteClick(campsite.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
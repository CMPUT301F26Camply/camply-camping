package com.example.camply.campsite.view

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.camply.campsite.controller.CampsiteBrowseController
import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.camplyTextFieldColors
import kotlinx.coroutines.launch

// CampsiteBrowseView.kt - Campsite Browsing UI Redesign
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help update the existing campsite browsing
// screen to match the redesigned Camply interface.
// This included assistance with applying the new Material 3
// theme colors, improving layout spacing, and updating the
// appearance of search results, filters, and dropdowns.
//
// Prompt:
// "Given all the redesign changes we have made to Camply,
// what do we need to update in CampsiteBrowseView.kt to
// make it consistent with our new theme and reusable UI
// components? I want the browsing screen to use the new
// Material 3 colors, improve spacing and readability,
// and match the styling of the other redesigned screens.
// Keep the existing functionality working as before."
//
// Changes from the original implementation:
// - Updated the loading spinner to use the theme primary color
//   instead of a fixed white color.
// - Updated the campsite result count to use Material 3
//   typography and secondary text colors.
// - Added weight(1f) to the results LazyColumn to improve
//   its use of available screen space.
// - Increased spacing between campsite results from
//   10dp to 12dp.
// - Updated the filter button to use theme colors for
//   active and inactive states.
// - Redesigned the autocomplete dropdown with a themed
//   surface background and a subtle border.
// - Reformatted sections of code for better readability.
//
// The existing campsite search, filtering, autocomplete,
// loading, error handling, and navigation callbacks were
// retained. Changes mainly focused on visual consistency
// with the redesigned Camply theme.

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

    /**
     * Displays the campsite browsing screen.
     *
     * @param onCampsiteClick Called with the campsite ID when a campsite is selected.
     * @param loadPhotoPreview Fetches the preview image bytes for a campsite ID and photo ID,
     * or returns null if unavailable.
     */
    @Composable
    fun Display(
        onCampsiteClick: (String) -> Unit,
        loadPhotoPreview: suspend (String, String) -> ByteArray? = { _, _ -> null }
    ) {
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
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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
                        text = if (count == 1) {
                            "1 campsite matches your search"
                        } else {
                            "$count campsites match your search"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(model.results, key = { it.id }) { campsite ->
                            summaryCard.Display(
                                campsite = campsite,
                                loadPhotoPreview = loadPhotoPreview,
                                onClick = { onCampsiteClick(campsite.id) }
                            )
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
                    containerColor = if (filtersActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (filtersActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            ) {
                Text(if (filtersActive) "Filter •" else "Filter")
            }

        }

        if (dropdownOpen && model.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            )
            {
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(model.suggestions.take(8), key = { it.id }) { campsite ->
                        Text(
                            text = "${campsite.name} · ${campsite.location}",
                            color = MaterialTheme.colorScheme.onSurface,
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
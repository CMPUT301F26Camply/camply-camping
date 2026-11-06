package com.example.camply.shared.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// AI Assistance:
// OpenAI, ChatGPT (GPT-6), 2026-11-06.
// Prompt: "In android kotlin jetpack compose I would like to have a
// filtering button view, so essentially have buttons at the top
// such as all, active, past, cancelled or rejected, and then a
// list underneath that by default shows all and the user can
// select multiple buttons at a time."
//
// ChatGPT assisted with understanding the structure of this reusable
// filtering component, Material3 FilterChip. The implementation was
// reviewed and modified for Camply use.

/**
 * Displays a row of selectable filter buttons.
 *
 * Multiple filters can be selected at once. An empty selection indicates
 * that no filtering should be applied.
 *
 * @param categories The available filter categories.
 * @param selectedCategories The currently selected categories.
 * @param onCategoryToggle Called when a category is selected or deselected.
 * @param modifier Optional modifier for the filter row.
 */
@Composable
fun FilterChips(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(categories) { category ->
            val selected = category in selectedCategories

            FilterChip(
                selected = selected,
                onClick = { onCategoryToggle(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CamplyPalette.Cream,
                    selectedContainerColor = CamplyPalette.Black,
                    labelColor = CamplyPalette.Ink,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

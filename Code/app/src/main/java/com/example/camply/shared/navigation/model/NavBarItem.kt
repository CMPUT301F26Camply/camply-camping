package com.example.camply.shared.navigation.model

/**
 * Represents an item displayed in the bottom navigation bar.
 *
 * @property id the unique identifier for the navigation item
 * @property label the text displayed for the navigation item
 * @property iconId the drawable resource ID for the item's icon
 */
data class NavBarItem(
    val id: String,
    val label: String,
    val iconId: Int
)
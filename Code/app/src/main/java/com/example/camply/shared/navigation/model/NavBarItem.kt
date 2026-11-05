package com.example.camply.shared.navigation.model

/**
 * Represents an item displayed in the bottom navigation bar.
 *
 * @property id The navigation destination associated with the item.
 * @property label The text displayed for the navigation item.
 * @property iconId The drawable resource ID for the item's icon.
 */
data class NavBarItem(
    val id: Screen,
    val label: String,
    val iconId: Int
)
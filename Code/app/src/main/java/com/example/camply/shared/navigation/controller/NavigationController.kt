package com.example.camply.shared.navigation.controller

import com.example.camply.shared.navigation.model.NavigationModel

/**
 * Handles navigation requests from the application's Views.
 *
 * @property navigationModel the model that stores navigation state
 */
class NavigationController(
    private val navigationModel: NavigationModel
) {

    /**
     * Navigates to the specified screen.
     *
     * @param screen the identifier of the destination screen
     * @param showBar whether the bottom navbar should be displayed
     */
    fun navigateTo(screen: String, showBar: Boolean = true) {
        navigationModel.navigateTo(screen, showBar)
    }

    /**
     * Handles clicks on bottom navigation bar items.
     *
     * @param itemId the identifier of the selected navbar item
     */
    fun onNavItemClick(itemId: String) {
        when (itemId) {
            "back" -> {
                // TODO: Implement navigation history and Back functionality.
            }

            else -> {
                navigateTo(itemId)
            }
        }
    }
}
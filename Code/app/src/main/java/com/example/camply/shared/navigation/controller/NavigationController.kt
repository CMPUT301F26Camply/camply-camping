package com.example.camply.shared.navigation.controller

import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.model.Screen

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
     * @param screen The destination screen.
     * @param showBar Whether the navbar should be visible.
     */
    fun navigateTo(screen: Screen, showBar: Boolean = true) {
        navigationModel.navigateTo(screen, showBar)
    }

    /**
     * Handles clicks on bottom navigation bar items.
     *
     * @param itemId The selected navigation destination.
     */
    fun onNavItemClick(itemId: Screen) {
        when (itemId) {
            Screen.BACK -> {
                // TODO: Implement navigation history and Back functionality.
            }
            else -> {
                navigateTo(itemId)
            }
        }
    }
}
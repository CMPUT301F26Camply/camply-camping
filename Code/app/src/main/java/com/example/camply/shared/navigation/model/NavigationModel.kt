package com.example.camply.shared.navigation.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.R

/**
 * Stores and manages the navigation state of Camply.
 *
 * Keeps track of the current screen, navbar visibility,
 * and the navigation items available to the user.
 */
class NavigationModel {

    private var selectedScreen by mutableStateOf("browse")
    private var isNavBarVisible by mutableStateOf(true)

    // TODO: Implement navigation history for the Back button.
    private val entrantNavBarItems = listOf(
        NavBarItem("back", "Back", R.drawable.back_arrow),
        NavBarItem("browse", "Browse Campsites", R.drawable.campsite_icon)
    )

    private val ownerNavBarItems = emptyList<NavBarItem>()
    private val adminNavBarItems = emptyList<NavBarItem>()

    /**
     * Returns the navigation bar items for a specific user role.
     *
     * @param role the role whose navigation items are requested
     * @return the navigation items available for that role
     */
    fun getNavBarItems(role: String): List<NavBarItem> {
        return when (role) {
            "entrant" -> entrantNavBarItems
            "owner" -> ownerNavBarItems
            "admin" -> adminNavBarItems
            else -> emptyList()
        }
    }

    /**
     * Returns whether the bottom navbar should be displayed.
     *
     * @return true if the navbar should be visible, false otherwise
     */
    fun getShowNavBar(): Boolean {
        return isNavBarVisible
    }

    /**
     * Returns the currently selected screen.
     *
     * @return the identifier of the current screen
     */
    fun getCurrentScreen(): String {
        return selectedScreen
    }

    /**
     * Updates the current screen and navbar visibility.
     *
     * @param screen the identifier of the screen to display
     * @param showBar whether the navbar should be visible
     */
    fun navigateTo(screen: String, showBar: Boolean = true) {
        selectedScreen = screen
        isNavBarVisible = showBar
    }
}
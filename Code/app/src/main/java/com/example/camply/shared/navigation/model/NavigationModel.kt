package com.example.camply.shared.navigation.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.R


/**
 * Defines the available navigation destinations in Camply.
 */
enum class Screen {
    BACK,
    BROWSE,
    LOTTERIES,
    RESERVATIONS,
    CAMPSITE_LIST,
    WAIT_LIST,
    INBOX
}

/**
 * Stores and manages the navigation state of Camply.
 *
 * Keeps track of the current screen, navbar visibility,
 * and the navigation items available to the user.
 */
class NavigationModel {

    private var selectedScreen by mutableStateOf(Screen.BROWSE)
    private var isNavBarVisible by mutableStateOf(true)

    // TODO: Implement navigation history for the Back button.
    private val entrantNavBarItems = listOf(
        NavBarItem(Screen.BACK, "Back", R.drawable.back_arrow_icon),
        NavBarItem(Screen.BROWSE, "Browse Campsites", R.drawable.campsite_icon),
        NavBarItem(Screen.LOTTERIES, "My Lotteries", R.drawable.lottery_icon),
        NavBarItem(Screen.RESERVATIONS, "My Reservations", R.drawable.reservation_icon),
    )

    private val ownerNavBarItems = listOf(
        NavBarItem(Screen.BACK, "Back", R.drawable.back_arrow_icon),
        NavBarItem(Screen.WAIT_LIST, "Waitlist", R.drawable.waitlist_icon),
        NavBarItem(Screen.RESERVATIONS, "Campsite Reservations", R.drawable.reservation_icon),
        NavBarItem(Screen.INBOX, "Owner Inbox", R.drawable.inbox_icon),
        NavBarItem(Screen.CAMPSITE_LIST, "Campsite List", R.drawable.camp_list)

    )
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
     * @return The current navigation destination.
     */
    fun getCurrentScreen(): Screen {
        return selectedScreen
    }

    /**
     * Updates the current screen and navbar visibility.
     *
     * @param screen The destination screen.
     * @param showBar Whether the navbar should be visible.
     */
    fun navigateTo(screen: Screen, showBar: Boolean = true) {
        selectedScreen = screen
        isNavBarVisible = showBar
    }
}
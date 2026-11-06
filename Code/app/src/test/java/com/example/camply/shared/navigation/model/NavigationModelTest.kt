package com.example.camply.shared.navigation.model

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Unit tests for the navigation items available to each user role (US 01.06.01, US 02.10.02).
 */
class NavigationModelTest {

    private val model = NavigationModel()

    /**
     * Checks that entrants can reach their reservations from the navigation bar.
     */
    @Test
    fun getNavBarItems_entrant_includesReservations() {
        assertTrue(model.getNavBarItems("entrant").any { it.id == Screen.RESERVATIONS })
    }

    /**
     * Checks that campsite owners do not get the entrant reservations item.
     */
    @Test
    fun getNavBarItems_owner_excludesReservations() {
        assertFalse(model.getNavBarItems("owner").any { it.id == Screen.RESERVATIONS })
    }

    /**
     * Checks that every role can reach the shared notification screen.
     */
    @Test
    fun getNavBarItems_allRoles_includeNotifications() {
        for (role in listOf("entrant", "owner", "admin")) {
            assertTrue(role, model.getNavBarItems(role).any { it.id == Screen.NOTIFICATIONS })
        }
    }
}

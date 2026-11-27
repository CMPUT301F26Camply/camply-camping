package com.example.camply.user.controller

import com.example.camply.user.model.AdminPanelModel
import com.example.camply.user.model.AdminPanelPage
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.Profile
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Local tests for the admin panel pages.
 *
 * Covering US 03.04.01, US 03.05.01 and US 03.06.01.
 */
class AdminPanelControllerTest {

    private lateinit var model: AdminPanelModel
    private lateinit var controller: AdminPanelController

    @Before
    fun setUp() {
        model = AdminPanelModel()
        controller = AdminPanelController(model)
    }

    @Test
    fun start_showsMenu() {
        assertNull(model.page)
    }

    @Test
    fun openCampsites_opensOwnerListAndClearsSelection() {
        controller.selectOwner(owner())
        controller.selectCampsite("pine")

        controller.openCampsites()

        assertEquals(AdminPanelPage.CAMPSITES, model.page)
        assertNull(model.selectedOwner)
        assertNull(model.detailCampsiteId)
    }

    @Test
    fun openProfiles_opensProfileListAndClearsSelection() {
        controller.selectProfile(entrant())

        controller.openProfiles()

        assertEquals(AdminPanelPage.PROFILES, model.page)
        assertNull(model.selectedProfile)
    }

    @Test
    fun campsiteBack_returnsOneLevel() {
        controller.openCampsites()
        controller.selectOwner(owner())
        controller.selectCampsite("pine")

        controller.backFromDetails()
        assertEquals("pine-cleared", null, model.detailCampsiteId)
        assertEquals(owner().getUid(), model.selectedOwner?.getUid())

        controller.backFromCampsites()
        assertNull(model.selectedOwner)
        assertEquals(AdminPanelPage.CAMPSITES, model.page)

        controller.backToMenu()
        assertNull(model.page)
    }

    @Test
    fun profileBack_returnsToListThenMenu() {
        controller.openProfiles()
        controller.selectProfile(entrant())

        controller.backFromProfile()
        assertNull(model.selectedProfile)
        assertEquals(AdminPanelPage.PROFILES, model.page)

        controller.backToMenu()
        assertNull(model.page)
    }

    /**
     * Verifies that the admin image browser can be opened from the
     * admin panel and that navigating back returns to the main menu.
     */
    @Test
    fun openImages_opensImageBrowserAndReturnsToMenu() {
        controller.openImages()

        assertEquals(AdminPanelPage.IMAGES, model.page)

        controller.backToMenu()

        assertNull(model.page)
    }

    private fun owner() = CampsiteOwnerUser("owner-1", Profile("Demo Owner", "owner@example.com", "7801111111"))

    private fun entrant() = EntrantUser("entrant-1", Profile("Jane Doe", "jane@example.com", "7802222222"))
}
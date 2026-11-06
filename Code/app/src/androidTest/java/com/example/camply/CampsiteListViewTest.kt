package com.example.camply

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import com.example.camply.campsite.model.Campsite
import com.example.camply.user.view.CampsiteListView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CampsiteListViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    /*Case: When there are no registered campsites for the owner
     so the screen should show the empty screen message*/
    @Test
    fun emptyList_showsEmptyMessage() {
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = emptyList(),
                addCampsite = {}, viewCampsite = {}, editCampsite = {}, lotteryRegistration = {},
                reservationList = {}, viewOrCreateLottery = {}
            )
        }
        composeRule.onNodeWithText("No Registered Campsites", substring = true).assertIsDisplayed()
    }

    /*This testcase checks that the name and location for the campsite are displayed*/
    @Test
    fun campsites_showNameAndLocation() {
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = listOf(
                    Campsite(id = "a", name = "Campsite A", location = "Location A"),
                    Campsite(id = "b", name = "Campsite B", location = "Location B")
                ),
                addCampsite = {}, viewCampsite = {}, editCampsite = {}, lotteryRegistration = {},
                reservationList = {}, viewOrCreateLottery = {}
            )
        }
        composeRule.onNodeWithText("Campsite A").assertIsDisplayed()
        composeRule.onNodeWithText("Location A").assertIsDisplayed()
        composeRule.onNodeWithText("Campsite B").assertIsDisplayed()
        composeRule.onNodeWithText("Location B").assertIsDisplayed()
    }

    /*This testcase checks that the "+"(add campsite) button works*/
    @Test
    fun addButton_callsAddCampsite() {
        var clicked = false
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = emptyList(),
                addCampsite = { clicked = true }, viewCampsite = {}, editCampsite = {}, lotteryRegistration = {},
                reservationList = {}, viewOrCreateLottery = {}
            )
        }
        composeRule.onNodeWithContentDescription("Add campsite").performClick()
        assertEquals(true, clicked)
    }

    /*This testcase checks that the edit button works correctly, i.e., enable edits for
    * that specific campsite*/
    @Test
    fun editButton_EditWithCorrectCampsite() {
        var edited: Campsite? = null
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = listOf(
                    Campsite(id = "a", name = "Campsite A", location = "Banff, AB"),
                    Campsite(id = "b", name = "Campsite B", location = "Jasper, AB")
                ),
                addCampsite = {}, viewCampsite = {}, editCampsite = { edited = it }, lotteryRegistration = {},
                reservationList = {}, viewOrCreateLottery = {}
            )
        }
        composeRule.onNodeWithContentDescription("Edit Campsite B").performClick()
        assertEquals("b", edited?.id)
    }

    /*This testcase checks that we can have a scrollable campsite list*/
    @Test
    fun longList_scrollsToLastCampsite() {
        val many = (1..20).map {
            Campsite(id = "id$it", name = "Site $it", location = "Place $it")
        }
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = many,
                addCampsite = {}, viewCampsite = {}, editCampsite = {}, lotteryRegistration = {},
                reservationList = {}, viewOrCreateLottery = {}
            )
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Site 20"))
        composeRule.onNodeWithText("Site 20").assertIsDisplayed()
    }
}
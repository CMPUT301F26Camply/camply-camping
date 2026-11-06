package com.example.camply

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.camply.campsite.model.Campsite
import com.example.camply.user.view.CampsiteListView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CampsiteListViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        campsites: List<Campsite>,
        addCampsite: () -> Unit = {},
        viewCampsite: (Campsite) -> Unit = {},
        editCampsite: (Campsite) -> Unit = {}
    ) {
        composeRule.setContent {
            CampsiteListView().Display(
                ownerName = "Owner 1",
                campsites = campsites,
                addCampsite = addCampsite,
                editCampsite = editCampsite,
                viewCampsite = viewCampsite,
                lotteryRegistration = {},
                reservationList = {},
                viewOrCreateLottery = {}
            )
        }
    }

    // 1. The screen header renders even with no campsites
    @Test
    fun header_isDisplayed() {
        show(emptyList())
        composeRule.onNodeWithText("Owner 1’s Campsites").assertIsDisplayed()
        composeRule.onNodeWithText("View Reservation List").assertIsDisplayed()
        composeRule.onNodeWithText("Create/View Lottery").assertIsDisplayed()
    }

    // 2. Edge: empty list shows the empty message
    @Test
    fun emptyList_showsEmptyMessage() {
        show(emptyList())
        composeRule.onNodeWithText("No Registered Campsites", substring = true).assertIsDisplayed()
    }

    // 3. Each campsite shows its name and location
    @Test
    fun campsites_showNameAndLocation() {
        show(
            listOf(
                Campsite(id = "a", name = "Campsite A", location = "Banff, AB"),
                Campsite(id = "b", name = "Campsite B", location = "Jasper, AB")
            )
        )
        composeRule.onNodeWithText("Campsite A").assertIsDisplayed()
        composeRule.onNodeWithText("Banff, AB").assertIsDisplayed()
        composeRule.onNodeWithText("Campsite B").assertIsDisplayed()
        composeRule.onNodeWithText("Jasper, AB").assertIsDisplayed()
    }

    // 4. Tapping a card and the pencil pass the correct campsite
    @Test
    fun tappingCardAndPencil_passCorrectCampsite() {
        var viewed: Campsite? = null
        var edited: Campsite? = null
        show(
            listOf(
                Campsite(id = "a", name = "Campsite A", location = "Banff, AB"),
                Campsite(id = "b", name = "Campsite B", location = "Jasper, AB")
            ),
            viewCampsite = { viewed = it },
            editCampsite = { edited = it }
        )
        composeRule.onNodeWithContentDescription("Edit Campsite B").performClick()
        assertEquals("b", edited?.id)

        composeRule.onNodeWithText("Jasper, AB").performClick()
        assertEquals("b", viewed?.id)
    }

    // 5. Edge: a long list scrolls to the last campsite
    @Test
    fun longList_scrollsToLastCampsite() {
        val many = (1..50).map { Campsite(id = "id$it", name = "Site $it", location = "Place $it") }
        show(many)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Site 50"))
        composeRule.onNodeWithText("Site 50").assertIsDisplayed()
    }
}
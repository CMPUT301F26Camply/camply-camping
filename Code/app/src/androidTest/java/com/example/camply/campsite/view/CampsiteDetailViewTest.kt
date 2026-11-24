package com.example.camply.campsite.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.camply.campsite.controller.CampsiteDetailController
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests the redesigned campsite details screen using a fake repository.
 */
class CampsiteDetailViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val campsite = Campsite(
        id = "site1",
        name = "Pine Ridge",
        location = "Banff, AB",
        description = "A peaceful forest campsite.",
        amenities = listOf("Fire pit", "Water"),
        maxOccupancy = 6,
        price = 45.0,
        tags = listOf("Forest", "Family"),
        rules = "Quiet hours after 10 PM.",
        cancellationPolicy = "Free cancellation up to 48 hours before arrival.",
        ownerId = "owner123"
    )

    private lateinit var model: CampsiteDetailModel
    private lateinit var repository: TestCampsiteRepository
    private lateinit var view: CampsiteDetailView

    @Before
    fun setUp() {
        model = CampsiteDetailModel()
        repository = TestCampsiteRepository(campsite)
        view = CampsiteDetailView(model, CampsiteDetailController(model, repository))
    }

    /**
     * Displays campsite details and the additional lottery content.
     */
    @Test
    fun display_showsCampsiteDetailsAndLotteryContent() {
        showView(showLotteryContent = true)

        compose.onNodeWithText("Pine Ridge").assertIsDisplayed()
        compose.onNodeWithText("Up to 6 guests").assertIsDisplayed()
        compose.onNodeWithText("$45").assertIsDisplayed()

        compose.onNodeWithText("A peaceful forest campsite.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fire pit").assertIsDisplayed()
        compose.onNodeWithText("Forest").assertIsDisplayed()

        compose.onNodeWithText("Lottery actions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Quiet hours after 10 PM.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Free cancellation up to 48 hours before arrival.")
            .performScrollTo().assertIsDisplayed()
    }

    /**
     * The campsite owner can open the edit screen for their campsite.
     */
    @Test
    fun display_ownerCanEditCampsite() {
        var editedCampsite: Campsite? = null
        showView(currentUserId = "owner123", onEdit = { editedCampsite = it })

        compose.onNodeWithContentDescription("Edit campsite")
            .assertIsDisplayed()
            .performClick()

        assertEquals("site1", editedCampsite?.id)
    }

    /**
     * Entrants cannot see the owner only edit action.
     */
    @Test
    fun display_nonOwnerCannotEditCampsite() {
        showView(currentUserId = "entrant123", onEdit = {})

        compose.onNodeWithText("Pine Ridge").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit campsite").assertDoesNotExist()
    }

    /**
     * The Back button remains available after scrolling through details.
     */
    @Test
    fun display_backButtonRemainsVisibleAfterScrolling() {
        var wentBack = false
        showView(onBack = { wentBack = true })

        compose.onNodeWithText("Quiet hours after 10 PM.").performScrollTo()
        compose.onNodeWithText("Back").assertIsDisplayed().performClick()

        assertEquals(true, wentBack)
    }

    /**
     * Displays a fallback message when an older campsite has no map pin.
     */
    @Test
    fun display_missingMapPin_showsFallbackMessage() {
        showView()

        compose.onNodeWithText("Map location has not been set for this campsite.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun showView(
        currentUserId: String? = null,
        onEdit: ((Campsite) -> Unit)? = null,
        onBack: () -> Unit = {},
        showLotteryContent: Boolean = false
    ) {
        compose.setContent {
            MaterialTheme {
                view.Display(
                    campsiteId = "site1",
                    onBack = onBack,
                    currentUserId = currentUserId,
                    onEdit = onEdit,
                    extraContent = {
                        if (showLotteryContent) Text("Lottery actions")
                    }
                )
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Pine Ridge").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Supplies campsite details without accessing Firebase.
     */
    private class TestCampsiteRepository(
        private val campsite: Campsite
    ) : CampsiteRepository {

        override suspend fun getCampsite(id: String): Campsite? =
            campsite.takeIf { it.id == id }

        override suspend fun getCampsites(): List<Campsite> = emptyList()

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> = emptyList()

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = campsite.id

        override suspend fun deleteCampsite(id: String) {}
    }
}

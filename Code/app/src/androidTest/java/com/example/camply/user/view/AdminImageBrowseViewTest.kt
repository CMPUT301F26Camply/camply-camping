
package com.example.camply.user.view

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.user.controller.AdminImageBrowseController
import com.example.camply.user.model.AdminImageBrowseModel
import com.example.camply.user.model.AdminUser
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.Profile
import com.example.camply.user.model.User
import com.example.camply.user.repository.UserRepository
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Tests administrative image browsing (US 03.06.01) and
 * the image removal interface (US 03.03.01).
 */
class AdminImageBrowseViewTest {

    // Compose rule for displaying and interacting with the view.
    @get:Rule
    val composeRule = createComposeRule()

    // Image browsing state.
    private val model = AdminImageBrowseModel()

    // Owner with uploaded promotional and profile images.
    private val owner = CampsiteOwnerUser(
        "owner-1", Profile("Daniel Smith", "daniel@example.com", "7801111111")
    )

    // Admin with an uploaded profile image.
    private val admin = AdminUser(
        "admin-1", Profile("Alex Brown", "alex@example.com", "7802222222")
    )

    // Entrant without an uploaded profile image.
    private val entrant = EntrantUser(
        "entrant-1", Profile("Chris Wilson", "chris@example.com", "7803333333")
    )

    // Campsite with two promotional images.
    private val campsite = Campsite(
        id = "campsite-1",
        name = "Pine Retreat",
        location = "Banff",
        ownerId = owner.getUid(),
        images = listOf("photo1", "photo2")
    )

    // Campsite without promotional images.
    private val emptyCampsite = Campsite(
        id = "campsite-2",
        name = "Lake Retreat",
        location = "Jasper",
        ownerId = owner.getUid()
    )

    // Valid image bytes for displaying test images.
    private val imageBytes by lazy { createImageBytes() }

    /**
     * Verifies that the dashboard displays the image categories
     * and correct uploaded image statistics.
     */
    @Test
    fun dashboard_showsImageStatistics() {
        show()

        composeRule.onNodeWithText("Uploaded Images").assertIsDisplayed()

        composeRule.onNodeWithText("TOTAL").assertIsDisplayed()
        composeRule.onNodeWithText("PROMOTIONAL").assertIsDisplayed()
        composeRule.onNodeWithText("PROFILE").assertIsDisplayed()

        composeRule.onNodeWithText("4").assertIsDisplayed()
        composeRule.onAllNodesWithText("2").assertCountEquals(2)

        scrollToText("Campsite promotional images")

        composeRule.onNodeWithText("Campsite promotional images")
            .assertIsDisplayed()

        scrollToText("Profile images")

        composeRule.onNodeWithText("Profile images").assertIsDisplayed()
    }

    /**
     * Verifies that an admin can search for an owner, browse their
     * campsites and inspect an uploaded promotional image.
     */
    @Test
    fun promotionalImages_canBrowseAndInspect() {
        show()

        scrollToText("Campsite promotional images")
        composeRule.onNodeWithText("Campsite promotional images").performClick()

        composeRule.onNodeWithText("Campsite Owners").assertIsDisplayed()

        scrollToSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Daniel")

        scrollToText("Daniel Smith")
        composeRule.onNodeWithText("Daniel Smith").performClick()

        composeRule.onNodeWithText("Owner Campsites").assertIsDisplayed()

        scrollToSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Banff")

        scrollToText("Pine Retreat")
        composeRule.onNodeWithText("Lake Retreat").assertDoesNotExist()
        composeRule.onNodeWithText("Pine Retreat").performClick()

        composeRule.onNodeWithText("Promotional Images").assertIsDisplayed()

        scrollToText("1 / 2")

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithContentDescription(
                "Photo of Pine Retreat. Tap to enlarge."
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(
            "Photo of Pine Retreat. Tap to enlarge."
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithContentDescription(
                "Enlarged photo of Pine Retreat"
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(
            "Enlarged photo of Pine Retreat"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close photo viewer")
            .performClick()

        composeRule.onNodeWithText("Back").performClick()

        composeRule.onNodeWithText("Owner Campsites").assertIsDisplayed()

        scrollToSearch()
        composeRule.onNode(hasSetTextAction()).assertTextContains("Banff")
    }

    /**
     * Verifies that profile images from different account roles can
     * be browsed, searched and inspected in the full screen viewer.
     */
    @Test
    fun profileImages_canBrowseAndInspect() {
        show()
        openProfiles()

        scrollToText("Alex Brown")
        composeRule.onNodeWithText("Role: Admin").assertIsDisplayed()

        scrollToText("Daniel Smith")
        composeRule.onNodeWithText("Role: Owner").assertIsDisplayed()

        composeRule.onNodeWithText("Chris Wilson").assertDoesNotExist()

        scrollToSearch()

        composeRule.onNode(hasSetTextAction())
            .performTextInput("alex@example.com")

        scrollToText("Alex Brown")

        composeRule.onNodeWithText("Daniel Smith").assertDoesNotExist()
        composeRule.onNodeWithText("Alex Brown").performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithContentDescription(
                "Enlarged profile image of Alex Brown"
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(
            "Enlarged profile image of Alex Brown"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close photo viewer")
            .performClick()

        composeRule.onNodeWithText("Profile Images").assertIsDisplayed()
    }

    /**
     * Verifies that cancelling image removal does not invoke
     * the removal callback or change the browsing data.
     */
    @Test
    fun cancelRemoval_preservesImage() {
        show(
            onRemoveProfileImage = {
                throw AssertionError("Removal callback should not be invoked.")
            }
        )

        requestProfileRemoval()

        composeRule.onNodeWithText("Remove image?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Remove image?").assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(2, model.usersWithImagesCount)
        }

        composeRule.onNodeWithText("Alex Brown").assertIsDisplayed()
    }

    /**
     * Verifies that failed image removal preserves the browsing
     * data and allows retrying the same removal request.
     */
    @Test
    fun failedRemoval_canRetry() {
        val removalRequests = mutableListOf<String>()

        show(
            onRemoveProfileImage = { user ->
                removalRequests.add(user.getUid())
                ImageRemovalResult.FAILURE
            }
        )

        requestProfileRemoval()

        composeRule.onNodeWithText("Remove image").performClick()
        composeRule.onNodeWithText("Unable to remove image").assertIsDisplayed()

        composeRule.onNodeWithText("Try again").performClick()
        composeRule.onNodeWithText("Unable to remove image").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(
                listOf(admin.getUid(), admin.getUid()),
                removalRequests
            )
            assertEquals(2, model.usersWithImagesCount)
        }

        composeRule.onNodeWithText("Close").performClick()
    }

    /**
     * Displays the image browsing view with preloaded test data.
     *
     * @param onRemoveProfileImage optional profile image removal callback
     */
    private fun show(
        onRemoveProfileImage: (suspend (User) -> ImageRemovalResult)? = null
    ) {
        model.showUsers(listOf(owner, admin, entrant))
        model.showCampsites(listOf(campsite, emptyCampsite))
        model.showProfileImages(
            mapOf(
                owner.getUid() to imageBytes,
                admin.getUid() to imageBytes
            )
        )

        val controller = AdminImageBrowseController(
            model,
            UserRepository(),
            FakeCampsiteRepository()
        )

        composeRule.setContent {
            AdminImageBrowseView().Display(
                controller = controller,
                model = model,
                onBack = {},
                onRemoveProfileImage = onRemoveProfileImage,
                loadData = { true }
            )
        }

        composeRule.waitForIdle()
    }

    /**
     * Locates the vertical browsing list rather than the horizontal
     * gallery thumbnail list.
     *
     * @return semantics interaction for the vertical browsing list
     */
    private fun browsingList(): SemanticsNodeInteraction =
        composeRule.onNode(
            hasScrollAction() and hasScrollToIndexAction() and
                    SemanticsMatcher.keyIsDefined(
                        SemanticsProperties.VerticalScrollAxisRange
                    )
        )

    /**
     * Scrolls the current browsing list to the specified text.
     *
     * @param text text to locate
     */
    private fun scrollToText(text: String) {
        browsingList().performScrollToNode(hasText(text))
    }

    /**
     * Scrolls the current browsing list to its search field.
     */
    private fun scrollToSearch() {
        browsingList().performScrollToNode(hasSetTextAction())
    }

    /**
     * Opens the profile image browsing category.
     */
    private fun openProfiles() {
        scrollToText("Profile images")
        composeRule.onNodeWithText("Profile images").performClick()
    }

    /**
     * Searches for the example admin and requests removal
     * of their uploaded profile image.
     */
    private fun requestProfileRemoval() {
        openProfiles()

        scrollToSearch()
        composeRule.onNode(hasSetTextAction()).performTextInput("Alex")

        scrollToText("Alex Brown")

        composeRule.onNodeWithContentDescription("Remove profile image")
            .performClick()
    }

    /**
     * Creates a valid JPEG image for testing previews and inspection.
     *
     * @return compressed JPEG image bytes
     */
    private fun createImageBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(
            32, 32, Bitmap.Config.ARGB_8888
        )

        val output = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()

        return output.toByteArray()
    }

    /**
     * Provides fake campsite and promotional image data
     * without accessing Firestore.
     */
    private inner class FakeCampsiteRepository : CampsiteRepository {

        /**
         * Returns the example campsites.
         *
         * @return list of example campsites
         */
        override suspend fun getCampsites(): List<Campsite> =
            listOf(campsite, emptyCampsite)

        /**
         * Retrieves an example campsite by ID.
         *
         * @param id campsite identifier
         * @return matching campsite or null
         */
        override suspend fun getCampsite(id: String): Campsite? =
            getCampsites().find { it.id == id }

        /**
         * Retrieves campsites belonging to the specified owner.
         *
         * @param ownerId owner identifier
         * @return matching campsites
         */
        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            getCampsites().filter { it.ownerId == ownerId }

        /**
         * Returns fake promotional image preview bytes.
         *
         * @param campsiteId campsite identifier
         * @param photoId photo identifier
         * @return compressed image bytes
         */
        override suspend fun getCampsitePreview(
            campsiteId: String,
            photoId: String
        ): ByteArray = imageBytes

        /**
         * Returns fake full resolution promotional image bytes.
         *
         * @param campsiteId campsite identifier
         * @param photoId photo identifier
         * @return compressed image bytes
         */
        override suspend fun getCampsitePhoto(
            campsiteId: String,
            photoId: String
        ): ByteArray = imageBytes

        /**
         * Rejects unexpected campsite saving requests.
         *
         * @param campsite campsite to save
         * @param photosToSave images to upload
         * @param photosToRemove image IDs to remove
         * @return campsite identifier
         */
        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = error("Unexpected campsite save")

        /**
         * Rejects unexpected campsite deletion requests.
         *
         * @param id campsite identifier
         */
        override suspend fun deleteCampsite(id: String) {
            error("Unexpected campsite deletion")
        }
    }
}

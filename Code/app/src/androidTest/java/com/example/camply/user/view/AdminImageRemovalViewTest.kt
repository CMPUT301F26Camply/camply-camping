package com.example.camply.user.view

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.user.controller.AdminImageBrowseController
import com.example.camply.user.controller.AdminImageRemovalController
import com.example.camply.user.model.AdminImageBrowseModel
import com.example.camply.user.model.AdminUser
import com.example.camply.user.model.Profile
import com.example.camply.user.repository.AdminImageRepository
import com.example.camply.user.repository.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Compose UI test checking that a confirmed removal in the admin image browser
 * reaches the image removal controller and repository (US 03.03.01).
 */
class AdminImageRemovalViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val model = AdminImageBrowseModel()
    private val repository = TestAdminImageRepository()

    private val admin = AdminUser("admin-1", Profile("Alex Brown", "alex@example.com", "7802222222"))

    /**
     * Checks that confirming a profile image removal sends it to the repository,
     * and that a failed removal is shown to the admin.
     */
    @Test
    fun confirmRemoval_reachesRepository_andShowsFailure() {
        show()

        browsingList().performScrollToNode(hasText("Profile images"))
        composeRule.onNodeWithText("Profile images").performClick()
        browsingList().performScrollToNode(hasSetTextAction())
        composeRule.onNode(hasSetTextAction()).performTextInput("Alex")
        browsingList().performScrollToNode(hasText("Alex Brown"))
        composeRule.onNodeWithContentDescription("Remove profile image").performClick()

        composeRule.onNodeWithText("Remove image?").assertIsDisplayed()
        composeRule.onNodeWithText("Remove image").performClick()

        composeRule.onNodeWithText("Unable to remove image").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(admin.getUid()), repository.attemptedProfiles)
        }
    }

    // A failed removal keeps the test from reloading the browser from Firestore.
    private fun show() {
        model.showUsers(listOf(admin))
        model.showCampsites(emptyList())
        model.showProfileImages(mapOf(admin.getUid() to createImageBytes()))

        val browseController = AdminImageBrowseController(model, UserRepository(), TestCampsiteRepository())
        val removalController = AdminImageRemovalController(repository)

        composeRule.setContent {
            AdminImageBrowseView().Display(
                controller = browseController,
                model = model,
                onBack = {},
                onRemoveCampsitePhoto = removalController::removeCampsitePhoto,
                onRemoveProfileImage = removalController::removeProfileImage,
                loadData = { true }
            )
        }
        composeRule.waitForIdle()
    }

    // The vertical browsing list, not the horizontal photo strip.
    private fun browsingList() = composeRule.onNode(
        hasScrollAction() and hasScrollToIndexAction() and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    )

    private fun createImageBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    /**
     * Records removal attempts without accessing Firestore, and always fails.
     */
    private class TestAdminImageRepository : AdminImageRepository {
        val attemptedProfiles = mutableListOf<String>()

        override suspend fun removeCampsitePhoto(campsiteId: String, photoId: String) {
            throw Exception("Could not delete.")
        }

        override suspend fun removeProfileImage(userId: String) {
            attemptedProfiles.add(userId)
            throw Exception("Could not delete.")
        }
    }

    /**
     * Provides no campsites without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        override suspend fun getCampsites(): List<Campsite> = emptyList()

        override suspend fun getCampsite(id: String): Campsite? = null

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> = emptyList()

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = error("Not used by image removal tests")

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun deleteCampsite(id: String) = error("Not used by image removal tests")
    }
}

package com.example.camply.campsite.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.model.Campsite
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests the campsite photo gallery using fake Firestore photo IDs
 * without loading images from Firebase.
 */
class CampsitePhotoGalleryTest {

    @get:Rule
    val compose = createComposeRule()

    private val campsite = Campsite(
        id = "campsite1",
        name = "Pine Ridge",
        images = listOf("photo1", "photo2")
    )

    /**
     * The gallery requests previews in photo order without loading full images.
     */
    @Test
    fun gallery_loadsPreviewsWithoutLoadingFullImages() {
        val previewRequests = mutableListOf<Pair<String, String>>()
        var fullImageLoads = 0

        compose.setContent {
            MaterialTheme {
                CampsitePhotoGallery(
                    campsite = campsite,
                    loadPhotoPreview = { campsiteId, photoId ->
                        previewRequests.add(campsiteId to photoId)
                        null
                    },
                    loadPhoto = { _, _ ->
                        fullImageLoads++
                        null
                    }
                )
            }
        }

        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals(
                listOf("campsite1" to "photo1", "campsite1" to "photo2"),
                previewRequests
            )
            assertEquals(0, fullImageLoads)
        }

        compose.onNodeWithText("1 / 2").assertIsDisplayed()
    }

    /**
     * Selecting another thumbnail opens its full photo and allows closing the viewer.
     */
    @Test
    fun gallery_selectsPhotoAndOpensFullViewer() {
        var requestedFullPhoto: String? = null

        compose.setContent {
            MaterialTheme {
                CampsitePhotoGallery(
                    campsite = campsite,
                    loadPhotoPreview = { _, _ -> null },
                    loadPhoto = { campsiteId, photoId ->
                        requestedFullPhoto = "$campsiteId/$photoId"
                        null
                    }
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("2 / 2").assertIsDisplayed()

        compose.onNodeWithText("Photo unavailable. Tap to view full image.")
            .performClick()

        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("campsite1/photo2", requestedFullPhoto)
        }

        compose.onNodeWithContentDescription("Close photo viewer")
            .assertIsDisplayed()
            .performClick()

        compose.onNodeWithContentDescription("Close photo viewer")
            .assertDoesNotExist()
    }
}
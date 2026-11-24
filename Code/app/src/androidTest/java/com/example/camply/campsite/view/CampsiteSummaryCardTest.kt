package com.example.camply.campsite.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.model.Campsite
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tests campsite summary cards and cover-photo selection without Firebase.
 */
class CampsiteSummaryCardTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Displays campsite details and invokes the selection callback.
     */
    @Test
    fun display_showsDetailsAndHandlesClick() {
        val campsite = Campsite(
            id = "site1",
            name = "Pine Ridge",
            location = "Banff, AB",
            maxOccupancy = 6,
            rating = 4.5
        )
        var clicked = false

        compose.setContent {
            MaterialTheme {
                CampsiteSummaryCard().Display(
                    campsite = campsite,
                    loadPhotoPreview = { _, _ -> null },
                    onClick = { clicked = true }
                )
            }
        }

        compose.onNodeWithText("Pine Ridge").assertIsDisplayed()
        compose.onNodeWithText("Banff, AB").assertIsDisplayed()
        compose.onNodeWithText("Up to 6 guests").assertIsDisplayed()
        compose.onNodeWithText("Rating 4.5 / 5").assertIsDisplayed()

        compose.onNode(hasClickAction()).performClick()
        assertTrue(clicked)
    }

    /**
     * A campsite without photos displays normally without requesting a preview.
     */
    @Test
    fun display_noPhotos_doesNotLoadPreview() {
        val campsite = Campsite(id = "site1", name = "Pine Ridge")
        var previewRequests = 0

        compose.setContent {
            MaterialTheme {
                CampsiteSummaryCard().Display(
                    campsite = campsite,
                    loadPhotoPreview = { _, _ ->
                        previewRequests++
                        null
                    },
                    onClick = {}
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText("Pine Ridge").assertIsDisplayed()
        compose.onNodeWithText("Not yet rated").assertIsDisplayed()
        assertEquals(0, previewRequests)
    }

    /**
     * The card requests the first photo's preview and loads a new
     * preview when the campsite's cover photo changes.
     */
    @Test
    fun display_coverPhotoChange_loadsNewPreview() {
        val coverId = mutableStateOf("photo1")
        val requests = CopyOnWriteArrayList<Pair<String, String>>()

        compose.setContent {
            MaterialTheme {
                CampsiteSummaryCard().Display(
                    campsite = Campsite(
                        id = "site1",
                        name = "Pine Ridge",
                        images = listOf(coverId.value)
                    ),
                    loadPhotoPreview = { campsiteId, photoId ->
                        requests.add(campsiteId to photoId)
                        null
                    },
                    onClick = {}
                )
            }
        }

        compose.waitUntil(5_000) { requests.contains("site1" to "photo1") }

        compose.runOnIdle { coverId.value = "photo2" }

        compose.waitUntil(5_000) { requests.contains("site1" to "photo2") }

        assertEquals(
            listOf("site1" to "photo1", "site1" to "photo2"),
            requests.toList()
        )
    }
}
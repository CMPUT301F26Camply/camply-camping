
package com.example.camply

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.view.CampsiteFormView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CampsiteFormViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val existing = Campsite(
        id = "a",
        name = "Campsite A",
        location = "Location, A",
        description = "Lakeside site",
        amenities = listOf("Fireplace", "Trash disposal"),
        maxOccupancy = 8,
        price = 45.0,
        tags = listOf("Lake", "Quiet"),
        rules = "Quiet hours 9pm",
        cancellationPolicy = "Free until 48h before",
        latitude = 53.3855,
        longitude = -107.9513
    )

    private fun setForm(
        campsite: Campsite? = null,
        onSubmit: (List<String>) -> Unit = {},
        onPhotos: (List<String>) -> Unit = {},
        onCoordinates: (Double?, Double?) -> Unit = { _, _ -> }
    ) {
        composeRule.setContent {
            MaterialTheme {
                CampsiteFormView().Display(
                    campsite = campsite,
                    errors = emptyList(),
                    onSubmit = {
                            name,
                            location,
                            description,
                            amenities,
                            occupancy,
                            price,
                            tags,
                            rules,
                            cancellationPolicy,
                            images,
                            _,
                            latitude,
                            longitude ->

                        onSubmit(
                            listOf(
                                name,
                                location,
                                description,
                                amenities,
                                occupancy,
                                price,
                                tags,
                                rules,
                                cancellationPolicy
                            )
                        )

                        onPhotos(images)
                        onCoordinates(latitude, longitude)
                    },
                    onBack = {},
                    loadPhotoPreview = { _, _ -> null }
                )
            }
        }
    }

    // Tests that the rules and policies dialog (pop-up) opens and closes correctly.
    @Test
    fun rulesAndPoliciesButton_opensAndClosesPopup() {
        setForm()

        composeRule.onNodeWithTag("rules_policies_button")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("policies_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("rules_field").assertIsDisplayed()
        composeRule.onNodeWithTag("cancellation_field").assertIsDisplayed()

        composeRule.onNodeWithTag("policies_done_button")
            .performClick()

        composeRule.onNodeWithTag("policies_dialog")
            .assertDoesNotExist()
    }

    // Tests that existing campsite details are prefilled and edited values are submitted correctly.
    @Test
    fun editCampsite_prefillsAndSubmitsChanges() {
        var submitted: List<String>? = null

        setForm(
            campsite = existing,
            onSubmit = { submitted = it }
        )

        formTextField("name_field")
            .assertTextContains("Campsite A")

        formTextField("price_field")
            .performScrollTo()
            .assertTextContains("45.0")

        formTextField("name_field")
            .performTextReplacement("Campsite A Renamed")

        composeRule.onNodeWithTag("save_button")
            .performClick()

        val result = requireNotNull(submitted)

        assertEquals("Campsite A Renamed", result[0])
        assertEquals("Location, A", result[1])
        assertEquals("Fireplace, Trash disposal", result[3])
        assertEquals("8", result[4])
        assertEquals("45.0", result[5])
        assertEquals("Quiet hours 9pm", result[7])
        assertEquals("Free until 48h before", result[8])
    }

    // Tests that a new campsite can be added with all required information.
    @Test
    fun addCampsite_withoutMapPin_doesNotSubmit() {
        var submitted: List<String>? = null

        setForm(onSubmit = { submitted = it })

        formTextField("name_field")
            .performTextInput("Pine Ridge")

        formTextField("location_field")
            .performScrollTo()
            .performTextInput("Banff, AB")

        formTextField("description_field")
            .performScrollTo()
            .performTextInput("Forest site")

        formTextField("amenities_field")
            .performScrollTo()
            .performTextInput("Fire pit, Water")

        formTextField("occupancy_field")
            .performScrollTo()
            .performTextInput("6")

        formTextField("price_field")
            .performScrollTo()
            .performTextInput("30.5")

        formTextField("tags_field")
            .performScrollTo()
            .performTextInput("Forest, Family")

        composeRule.onNodeWithTag("rules_policies_button")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("rules_field")
            .performTextInput("No pets")

        composeRule.onNodeWithTag("cancellation_field")
            .performTextInput("Free for 24h")

        composeRule.onNodeWithTag("policies_done_button")
            .performClick()

        composeRule.onNodeWithTag("save_button")
            .performClick()

        assertEquals(null, submitted)

        composeRule.onNodeWithTag("map_picker_button")
            .performScrollTo()

        composeRule.onNodeWithText(
            "* Please select a campsite location on the map."
        ).assertIsDisplayed()
    }

    // Tests that updated campsite information appears correctly on the screen after saving.
    @Test
    fun saveChanges_updatedValuesAppearOnScreen() {
        composeRule.setContent {
            var current by remember { mutableStateOf<Campsite?>(existing) }

            MaterialTheme {
                CampsiteFormView().Display(
                    campsite = current,
                    errors = emptyList(),
                    onSubmit = {
                            name,
                            location,
                            description,
                            amenities,
                            occupancy,
                            price,
                            tags,
                            rules,
                            cancellationPolicy,
                            images,
                            _,
                            latitude,
                            longitude ->

                        current = Campsite(
                            id = "a",
                            name = name,
                            location = location,
                            description = description,
                            amenities = amenities.split(",").map { it.trim() },
                            maxOccupancy = occupancy.toInt(),
                            price = price.toDouble(),
                            tags = tags.split(",").map { it.trim() },
                            rules = rules,
                            cancellationPolicy = cancellationPolicy,
                            images = images,
                            latitude = latitude,
                            longitude = longitude
                        )
                    },
                    onBack = {},
                    loadPhotoPreview = { _, _ -> null }
                )
            }
        }

        formTextField("name_field")
            .performTextReplacement("Lakeview Deluxe")

        formTextField("location_field")
            .performScrollTo()
            .performTextReplacement("Jasper, AB")

        composeRule.onNodeWithTag("save_button")
            .performClick()

        formTextField("name_field")
            .assertTextContains("Lakeview Deluxe")

        formTextField("location_field")
            .performScrollTo()
            .assertTextContains("Jasper, AB")

        formTextField("description_field")
            .performScrollTo()
            .assertTextContains("Lakeside site")

        formTextField("occupancy_field")
            .performScrollTo()
            .assertTextContains("8")

        formTextField("price_field")
            .performScrollTo()
            .assertTextContains("45.0")
    }

    // Tests that updating a campsite preserves its existing information.
    @Test
    fun updateCampsite_preservesExistingData() {
        val repository = TestCampsiteRepository()
        repository.add(existing)

        val updated = Campsite(
            id = existing.id,
            name = "Updated Campsite",
            location = "Banff, AB",
            description = existing.description,
            amenities = existing.amenities,
            maxOccupancy = existing.maxOccupancy,
            price = existing.price,
            tags = existing.tags,
            rules = existing.rules,
            cancellationPolicy = existing.cancellationPolicy
        )

        repository.update(updated)

        assertEquals(1, repository.campsites.size)
        assertEquals("Updated Campsite", repository.campsites[0].name)
        assertEquals("Banff, AB", repository.campsites[0].location)
        assertEquals("Lakeside site", repository.campsites[0].description)
        assertEquals(listOf("Fireplace", "Trash disposal"), repository.campsites[0].amenities)
        assertEquals(8, repository.campsites[0].maxOccupancy)
        assertEquals(45.0, repository.campsites[0].price, 0.0)
        assertEquals("owner123", repository.ownerId)
        assertTrue(repository.campsites.isNotEmpty())
    }


    /**
     * Editing a campsite preserves its photo IDs and map coordinates.
     */
    @Test
    fun editCampsite_preservesPhotosAndCoordinates() {
        existing.images = listOf("photo1", "photo2")

        var savedPhotos: List<String>? = null
        var savedLatitude: Double? = null
        var savedLongitude: Double? = null

        setForm(
            campsite = existing,
            onPhotos = { savedPhotos = it },
            onCoordinates = { latitude, longitude ->
                savedLatitude = latitude
                savedLongitude = longitude
            }
        )

        composeRule.onNodeWithTag("save_button").performClick()

        assertEquals(listOf("photo1", "photo2"), savedPhotos)
        assertEquals(53.3855, savedLatitude)
        assertEquals(-107.9513, savedLongitude)
    }


    /**
     * Choosing a new cover photo moves its ID to the front of the list.
     */
    @Test
    fun editCampsite_changeCoverPhoto_reordersImages() {
        existing.images = listOf("photo1", "photo2")

        var savedPhotos: List<String>? = null

        setForm(
            campsite = existing,
            onPhotos = { savedPhotos = it }
        )

        // Scroll the form vertically until the photo controls are visible.
        composeRule.onNodeWithText(
            "Your first photo is the cover shown on campsite cards."
        ).performScrollTo()

        // Select the second photo.
        composeRule.onNodeWithTag("photo_photo2")
            .performClick()

        composeRule.waitForIdle()

        // Verify photo2 is selected.
        composeRule.onNodeWithTag("replace_photo_photo2")
            .assertExists()

        // Make photo2 the cover.
        composeRule.onNodeWithTag("cover_photo_photo2")
            .assertExists()
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("save_button")
            .performClick()

        assertEquals(listOf("photo2", "photo1"), savedPhotos)
    }



    // In-memory fake repository used to test campsite updates without Firebase.
    private class TestCampsiteRepository {

        val campsites = mutableListOf<Campsite>()
        var ownerId = "owner123"

        fun add(campsite: Campsite) {
            campsites.add(campsite)
        }

        fun update(campsite: Campsite) {
            val index = campsites.indexOfFirst {
                it.id == campsite.id
            }

            require(index != -1) {
                "Campsite not found"
            }

            campsites[index] = campsite
        }
    }

    // AI Assistance Reference:
    // OpenAI ChatGPT (GPT-6)
    // https://chatgpt.com/
    // Filip Kosmajac - fkosmajac
    // Prompt:
    // "Help me fix my Compose UI tests failing to input text because the test tags are attached
    // to the shared CamplyTextField wrapper rather than the actual editable text field."
    //
    // AI helped create this helper to find the editable child inside a tagged CamplyTextField
    private fun formTextField(tag: String) = composeRule.onNode(
        hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true
    )
}

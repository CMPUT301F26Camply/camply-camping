
package com.example.camply

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.user.view.CampsiteFormView
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
        cancellationPolicy = "Free until 48h before"
    )

    private fun setForm(
        campsite: Campsite? = null,
        onSubmit: (List<String>) -> Unit = {}
    ) {
        val model = NavigationModel()

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
                            cancellationPolicy ->

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
                    },
                    onBack = {},
                    navigationModel = model,
                    navigationController = NavigationController(model)
                )
            }
        }
    }

    // Tests that the rules and policies dialog opens and closes correctly.
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

        composeRule.onNodeWithTag("name_field")
            .assertTextContains("Campsite A")

        composeRule.onNodeWithTag("price_field")
            .performScrollTo()
            .assertTextContains("45.0")

        composeRule.onNodeWithTag("name_field")
            .performTextReplacement("Campsite A Renamed")

        composeRule.onNodeWithTag("save_button")
            .performScrollTo()
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
    fun addCampsite_submitsEnteredValues() {
        var submitted: List<String>? = null

        setForm(onSubmit = { submitted = it })

        composeRule.onNodeWithTag("name_field")
            .performTextInput("Pine Ridge")

        composeRule.onNodeWithTag("location_field")
            .performScrollTo()
            .performTextInput("Banff, AB")

        composeRule.onNodeWithTag("description_field")
            .performScrollTo()
            .performTextInput("Forest site")

        composeRule.onNodeWithTag("amenities_field")
            .performScrollTo()
            .performTextInput("Fire pit, Water")

        composeRule.onNodeWithTag("occupancy_field")
            .performScrollTo()
            .performTextInput("6")

        composeRule.onNodeWithTag("price_field")
            .performScrollTo()
            .performTextInput("30.5")

        composeRule.onNodeWithTag("tags_field")
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
            .performScrollTo()
            .performClick()

        assertEquals(
            listOf(
                "Pine Ridge",
                "Banff, AB",
                "Forest site",
                "Fire pit, Water",
                "6",
                "30.5",
                "Forest, Family",
                "No pets",
                "Free for 24h"
            ),
            submitted
        )
    }

    // Tests that updated campsite information appears correctly on the screen after saving.
    @Test
    fun saveChanges_updatedValuesAppearOnScreen() {
        composeRule.setContent {
            var current by remember { mutableStateOf<Campsite?>(existing) }
            val model = NavigationModel()

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
                            cancellationPolicy ->

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
                            cancellationPolicy = cancellationPolicy
                        )
                    },
                    onBack = {},
                    navigationModel = model,
                    navigationController = NavigationController(model)
                )
            }
        }

        composeRule.onNodeWithTag("name_field")
            .performTextReplacement("Lakeview Deluxe")

        composeRule.onNodeWithTag("location_field")
            .performScrollTo()
            .performTextReplacement("Jasper, AB")

        composeRule.onNodeWithTag("save_button")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("name_field")
            .assertTextContains("Lakeview Deluxe")

        composeRule.onNodeWithTag("location_field")
            .performScrollTo()
            .assertTextContains("Jasper, AB")

        composeRule.onNodeWithTag("description_field")
            .performScrollTo()
            .assertTextContains("Lakeside site")

        composeRule.onNodeWithTag("occupancy_field")
            .performScrollTo()
            .assertTextContains("8")

        composeRule.onNodeWithTag("price_field")
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
}

package com.example.camply.reservations.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.user.view.CampsiteListView
import org.junit.Rule
import org.junit.Test

class OwnerBookingDetailViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val campsite = Campsite(
        id = "1",
        name = "Campsite A",
        location = "Banff",
        availability = listOf(DateRange("2026-12-01", "2026-12-30"))
    )

    @Test
    fun bookedDates_showsBookedAndAvailableRanges() {
        composeRule.setContent {
            MaterialTheme {
                CampsiteListView().BookedDatesView(
                    campsite,
                    listOf(DateRange("2026-12-01", "2026-12-20"))
                )
            }
        }

        composeRule.onNodeWithText("Booked from 2026-12-01 to 2026-12-20").assertIsDisplayed()
        composeRule.onNodeWithText("Dates Still Available from 2026-12-01 to 2026-12-30").assertIsDisplayed()
    }

    @Test
    fun noBookedDates_showsEmptyMessage() {
        composeRule.setContent {
            MaterialTheme {
                CampsiteListView().BookedDatesView(campsite, emptyList())
            }
        }

        composeRule.onNodeWithText("No Booked Dates for this campsite.").assertIsDisplayed()
    }
}
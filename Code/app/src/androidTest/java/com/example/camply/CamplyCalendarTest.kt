package com.example.camply

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.model.DateRange
import com.example.camply.shared.view.CamplyCalendar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test


// AI Usage Declaration
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Provider: OpenAI
// Tool: ChatGPT 6
// Date Used: November 27, 2026
//
// Prompt:
// "Given my CamplyCalendar.kt implementation help me write instrumented tests for the
// shared calendar component. The tests should verify month navigation, date range selection,
// previously selected dates, allowed and blocked dates, clearing selections, and confirming
// valid date ranges. Follow the existing testing conventions, avoid duplicate tests and unnecessary
// dependencies"
//
// Purpose:
// Used ChatGPT to assist with writing instrumented tests for CamplyCalendar.kt
// to verify calendar navigation, date selection, availability restrictions,
// and confirmation behavior.
//
// Extent of Use:
// Substantive assistance with implementing Compose UI test cases using the
// existing calendar component, accessibility descriptions, and DateRange model.


/**
 * Instrumented UI tests for the shared CamplyCalendar component.
 *
 * Verifies month navigation, date range selection, initial selections,
 * allowed and blocked dates, clearing selections and confirmation.
 */
class CamplyCalendarTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Fixed date range to keep tests independent of the current date.
    private val allowedRange = DateRange(
        "2027-06-01",
        "2027-06-30"
    )

    /**
     * Displays the calendar with optional initial, allowed and blocked dates.
     */
    private fun setCalendar(
        initialDates: DateRange = DateRange(),
        allowedDates: List<DateRange>? = listOf(allowedRange),
        blockedDates: List<DateRange> = emptyList(),
        onConfirm: (DateRange) -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                CamplyCalendar(
                    initialDates = initialDates,
                    allowedDates = allowedDates,
                    blockedDates = blockedDates,
                    onConfirm = onConfirm
                )
            }
        }
    }

    /**
     * The calendar should initially display the month containing
     * the first allowed date.
     */
    @Test
    fun initialMonth_showsAllowedMonth() {
        setCalendar()

        composeRule.onNodeWithText("June 2027")
            .assertIsDisplayed()
    }

    /**
     * The previous and next month buttons should update
     * the displayed month.
     */
    @Test
    fun monthNavigation_changesDisplayedMonth() {
        setCalendar()

        composeRule.onNodeWithContentDescription("Next month")
            .performClick()

        composeRule.onNodeWithText("July 2027")
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Previous month")
            .performClick()

        composeRule.onNodeWithText("June 2027")
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Previous month")
            .performClick()

        composeRule.onNodeWithText("May 2027")
            .assertIsDisplayed()
    }

    /**
     * Confirmation should remain disabled until both
     * a start and an end date have been selected.
     */
    @Test
    fun incompleteSelection_disablesConfirmation() {
        setCalendar()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }

    /**
     * Selecting a valid range should highlight its dates,
     * enable confirmation and return the selected DateRange.
     */
    @Test
    fun validRangeSelection_returnsSelectedDates() {
        var confirmedDates: DateRange? = null

        setCalendar(
            onConfirm = { confirmedDates = it }
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-07, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                DateRange("2027-06-05", "2027-06-10"),
                confirmedDates
            )
        }
    }

    /**
     * Selecting the same date twice should allow a date range
     * containing only one day.
     */
    @Test
    fun sameDaySelection_returnsSingleDayRange() {
        var confirmedDates: DateRange? = null

        setCalendar(
            onConfirm = { confirmedDates = it }
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).performClick()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                DateRange("2027-06-05", "2027-06-05"),
                confirmedDates
            )
        }
    }

    /**
     * Previously selected dates should be displayed when
     * opening the calendar with an existing date range.
     */
    @Test
    fun initialDates_displaysExistingSelection() {
        setCalendar(
            initialDates = DateRange(
                "2027-06-05",
                "2027-06-10"
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsEnabled()
    }

    /**
     * Clearing a selection should remove its selected dates
     * and disable confirmation.
     */
    @Test
    fun clearSelection_removesSelectedDates() {
        setCalendar(
            initialDates = DateRange(
                "2027-06-05",
                "2027-06-10"
            )
        )

        composeRule.onNodeWithText("Clear")
            .performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, selected"
        ).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }

    /**
     * Dates outside the allowed date range should be disabled.
     */
    @Test
    fun outsideAllowedRange_datesAreDisabled() {
        setCalendar(
            allowedDates = listOf(
                DateRange("2027-06-05", "2027-06-20")
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-04, unavailable"
        ).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-21, unavailable"
        ).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).assertIsEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-20, available"
        ).assertIsEnabled()
    }

    /**
     * Blocked dates should remain disabled even when they
     * fall within an allowed date range.
     */
    @Test
    fun blockedDates_areDisabled() {
        setCalendar(
            blockedDates = listOf(
                DateRange("2027-06-10", "2027-06-15")
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-10, blocked"
        ).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-15, blocked"
        ).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-09, available"
        ).assertIsEnabled()

        composeRule.onNodeWithContentDescription(
            "2027-06-16, available"
        ).assertIsEnabled()
    }

    /**
     * A range containing blocked dates should display an error
     * and prevent confirmation.
     */
    @Test
    fun rangeOverlapsBlockedDates_preventsConfirmation() {
        setCalendar(
            blockedDates = listOf(
                DateRange("2027-06-10", "2027-06-15")
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-20, available"
        ).performClick()

        composeRule.onNodeWithText(
            "The selected range contains unavailable dates."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }

    /**
     * A selection should not span a gap between two separate
     * allowed date ranges.
     */
    @Test
    fun rangeSpansUnavailableDates_preventsConfirmation() {
        setCalendar(
            allowedDates = listOf(
                DateRange("2027-06-01", "2027-06-10"),
                DateRange("2027-06-15", "2027-06-30")
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-20, available"
        ).performClick()

        composeRule.onNodeWithText(
            "The selected range contains unavailable dates."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }

    /**
     * Selecting a date earlier than the current start date
     * should replace the start date.
     */
    @Test
    fun earlierDateSelection_replacesStartDate() {
        setCalendar()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, available"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }

    /**
     * Selecting another date after completing a range
     * should begin a new selection.
     */
    @Test
    fun completedRange_nextSelection_startsNewRange() {
        setCalendar(
            initialDates = DateRange(
                "2027-06-05",
                "2027-06-10"
            )
        )

        composeRule.onNodeWithContentDescription(
            "2027-06-15, available"
        ).performClick()

        composeRule.onNodeWithContentDescription(
            "2027-06-15, selected"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-05, available"
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "2027-06-10, available"
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Confirm dates")
            .assertIsNotEnabled()
    }
}
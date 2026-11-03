package com.example.camply.campsite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for search (US 01.01.01) and filtering (US 01.01.02). Plain JVM tests, no Firebase needed. */
class CampsiteFilterTest {

    private val banff = Campsite(
        id = "1", name = "Banff Pines", location = "Banff", maxOccupancy = 6,
        availability = listOf(DateRange("2027-05-01", "2027-05-31"))
    )
    private val jasper = Campsite(
        id = "2", name = "Jasper Lakeside", location = "Jasper", maxOccupancy = 2,
        availability = listOf(DateRange("2027-06-01", "2027-06-30"))
    )

    // --- search ---

    @Test fun search_isPartialAndCaseInsensitive() {
        assertTrue(banff.matchesName("banff"))
        assertTrue(banff.matchesName("PINES"))
        assertFalse(banff.matchesName("jasper"))
    }

    @Test fun search_blankMatchesEverything() {
        assertTrue(banff.matchesName(""))
        assertTrue(banff.matchesName("   "))
    }

    // --- filter ---

    @Test fun emptyFilter_matchesEverything() {
        assertTrue(CampsiteFilter().matches(banff))
        assertTrue(CampsiteFilter().isEmpty)
    }

    @Test fun filter_byLocation_ignoresCase() {
        assertTrue(CampsiteFilter(location = "banff").matches(banff))
        assertFalse(CampsiteFilter(location = "Banff").matches(jasper))
    }

    @Test fun filter_byGuests_requiresEnoughRoom() {
        assertTrue(CampsiteFilter(guests = 6).matches(banff))
        assertFalse(CampsiteFilter(guests = 7).matches(banff))
    }

    @Test fun filter_byDates_requiresStayInsideAnAvailableRange() {
        assertTrue(CampsiteFilter(dateFrom = "2027-05-10", dateTo = "2027-05-12").matches(banff))
        assertFalse(CampsiteFilter(dateFrom = "2027-05-30", dateTo = "2027-06-02").matches(banff))
        assertFalse(CampsiteFilter(dateFrom = "2027-05-10", dateTo = "2027-05-12").matches(jasper))
    }

    // --- draft validation ---

    @Test fun draft_validWithNoInput() {
        assertNull(FilterDraft().validate())
        assertTrue(FilterDraft().toFilter().isEmpty)
    }

    @Test fun draft_needsBothDates() {
        assertNotNull(FilterDraft(dateFrom = "2027-05-10").validate())
    }

    @Test fun draft_rejectsBadFormatAndBackwardsDates() {
        assertNotNull(FilterDraft(dateFrom = "May 10", dateTo = "May 12").validate())
        assertNotNull(FilterDraft(dateFrom = "2027-13-01", dateTo = "2027-13-02").validate())
        assertNotNull(FilterDraft(dateFrom = "2027-05-12", dateTo = "2027-05-10").validate())
    }

    @Test fun draft_rejectsZeroGuests() {
        assertNotNull(FilterDraft(guests = "0").validate())
    }

    @Test fun draft_convertsToFilter() {
        val filter = FilterDraft("Banff", "2027-05-10", "2027-05-12", "4").toFilter()
        assertEquals(CampsiteFilter("Banff", "2027-05-10", "2027-05-12", 4), filter)
    }
}
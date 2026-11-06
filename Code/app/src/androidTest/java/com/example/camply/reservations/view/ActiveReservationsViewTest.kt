package com.example.camply.reservations.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.controller.ActiveReservationsController
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the active reservations list and details (US 01.06.01).
 */
class ActiveReservationsViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var campsiteRepository: TestCampsiteRepository

    private val campsite = Campsite(
        id = CAMPSITE_ID,
        name = "Banff Pines",
        location = "Banff",
        rules = "Quiet hours are from 10 PM to 7 AM.",
        cancellationPolicy = "Cancel at least 7 days before arrival."
    )

    /**
     * Creates the fake repositories before each test.
     */
    @Before
    fun setUp() {
        reservationRepository = TestReservationRepository()
        campsiteRepository = TestCampsiteRepository()
    }

    /**
     * Checks that a reservation shows its campsite name, location, dates, and status.
     */
    @Test
    fun display_activeReservation_showsAllDetails() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(reservation("first", "2027-06-12", "2027-06-15"))

        showScreen()

        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText("Banff").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onAllNodesWithText("Active").assertCountEquals(2)
    }

    /**
     * Checks that every reservation is shown, including two at the same campsite.
     */
    @Test
    fun display_multipleReservations_showsAll() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("first", "2027-06-12", "2027-06-15"),
            reservation("second", "2027-07-01", "2027-07-03")
        )

        showScreen()

        compose.onAllNodesWithText("Banff Pines").assertCountEquals(2)
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-07-01 to 2027-07-03").assertIsDisplayed()
    }

    /**
     * Checks that a reservation is still shown when its campsite no longer exists.
     */
    @Test
    fun display_missingCampsite_showsUnavailable() {
        reservationRepository.reservations = listOf(reservation("first", "2027-06-12", "2027-06-15"))

        showScreen()

        compose.onNodeWithText("Campsite unavailable").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
    }

    /**
     * Checks that an entrant with no reservations sees the empty message.
     */
    @Test
    fun display_noReservations_showsEmptyMessage() {
        showScreen()

        compose.onNodeWithText("You have no reservations.").assertIsDisplayed()
    }

    /**
     * Checks that selecting a reservation shows its details, including the rules and policy.
     */
    @Test
    fun reservationCard_click_showsDetails() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(reservation("first", "2027-06-12", "2027-06-15"))

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("Reservation details").assertIsDisplayed()
        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText(campsite.rules).assertIsDisplayed()
        compose.onNodeWithText(campsite.cancellationPolicy).assertIsDisplayed()
    }

    /**
     * Checks that the back button returns from the details to the list.
     */
    @Test
    fun detailsBackButton_click_returnsToList() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(reservation("first", "2027-06-12", "2027-06-15"))

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("My Reservations").assertIsDisplayed()
        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText(campsite.rules).assertDoesNotExist()
    }

    /**
     * Checks that a reservation's details leave out the rules when its campsite no longer exists.
     */
    @Test
    fun reservationCard_missingCampsite_showsDetailsWithoutRules() {
        reservationRepository.reservations = listOf(reservation("first", "2027-06-12", "2027-06-15"))

        showScreen()
        compose.onNodeWithText("Campsite unavailable").performClick()

        compose.onNodeWithText("Reservation details").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("Rules & cancellation policy").assertDoesNotExist()
    }

    /**
     * Checks that selecting the Active filter displays only active reservations.
     */
    @Test
    fun filterChips_selectActive_showsOnlyActiveReservations() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("active", "2027-06-12", "2027-06-15"),
            reservation("past", "2027-06-01", "2027-06-05")
        )

        showScreen()

        // Both reservations appear before selecting a filter.
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()

        // Select Active using the actual filter chip.
        compose.onAllNodesWithText("Active").onFirst().performClick()

        // Only the active reservation remains visible.
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertDoesNotExist()
    }


    /**
     * Checks that an empty filter result displays the appropriate message.
     */
    @Test
    fun filterChips_noMatchingReservations_showsMessage() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("past", "2027-06-01", "2027-06-05")
        )

        showScreen()

        // The entrant has a past reservation.
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()

        // No active reservations match the selected filter.
        compose.onNodeWithText("Active").performClick()

        compose.onNodeWithText("No reservations match the selected filters.").assertIsDisplayed()
    }


    /**
     * Checks that a confirmed reservation with an end date before today
     * is automatically shown as Past.
     */
    @Test
    fun display_completedReservation_showsPastStatus() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("past", "2027-06-01", "2027-06-05")
        )

        // The test date is 2027-06-10, so this trip has ended.
        showScreen()

        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
        compose.onAllNodesWithText("Past").assertCountEquals(2)
    }


    /**
     * Checks that an old cancelled reservation stays Cancelled
     * instead of being treated as a past completed trip.
     */
    @Test
    fun filterChips_cancelledReservation_notShownUnderPast() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation(
                "cancelled",
                "2027-06-01",
                "2027-06-05",
                ReservationStatus.CANCELLED
            )
        )

        showScreen()

        // The cancelled reservation is visible under All.
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
        compose.onAllNodesWithText("Cancelled").assertCountEquals(2)

        // Selecting Past should not include cancelled reservations.
        compose.onNodeWithText("Past").performClick()

        compose.onNodeWithText("No reservations match the selected filters.")
            .assertIsDisplayed()

        // All restores the cancelled reservation.
        compose.onNodeWithText("All").performClick()

        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
    }


    /**
     * Checks that selecting All clears the active filter
     * and displays every reservation again.
     */
    @Test
    fun filterChips_selectAll_restoresAllReservations() {
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("active", "2027-06-12", "2027-06-15"),
            reservation("past", "2027-06-01", "2027-06-05")
        )

        showScreen()

        // All is selected by default, so both reservations appear.
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()

        // Select Active and check that Past disappears.
        compose.onAllNodesWithText("Active").onFirst().performClick()

        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertDoesNotExist()

        // Select All and check that both return.
        compose.onNodeWithText("All").performClick()

        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
    }


    private fun showScreen() {
        val model = ActiveReservationsModel()
        val controller = ActiveReservationsController(
            model,
            reservationRepository,
            campsiteRepository,
            today = { "2027-06-10" }
        )
        val view = ActiveReservationsView(model, controller)

        compose.setContent { view.Display(ENTRANT_ID) }
    }

    private fun reservation(
        id: String,
        from: String,
        to: String,
        status: ReservationStatus = ReservationStatus.CONFIRMED
    ) = Reservation(
        id = id,
        entrantId = ENTRANT_ID,
        campsiteId = CAMPSITE_ID,
        dates = DateRange(from, to),
        status = status
    )

    /**
     * Provides reservation data without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {
        var reservations: List<Reservation> = emptyList()

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
            reservations.filter { it.entrantId == entrantId }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"
    }

    /**
     * Provides campsite data without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? = campsites.firstOrNull { it.id == id }

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            campsites.filter { it.ownerId == ownerId }

        override suspend fun saveCampsite(campsite: Campsite): String = campsite.id

        override suspend fun deleteCampsite(id: String) = Unit
    }

    private companion object {
        const val ENTRANT_ID = "entrant1"
        const val CAMPSITE_ID = "campsite1"
    }
}


package com.example.camply.reservations.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.controller.OwnerReservationsController
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.collections.filter

/**
 * Compose UI tests for owner reservation management (US 02.07.01).
 */
class OwnerReservationsViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val ownerId = "test-owner"

    private lateinit var campsiteRepository: FakeCampsiteRepository
    private lateinit var reservationRepository: FakeReservationRepository

    @Before
    fun setUp() {
        campsiteRepository = FakeCampsiteRepository()
        reservationRepository = FakeReservationRepository()
    }

    /**
     * Shows a message when the owner has no registered campsites.
     */
    @Test
    fun noCampsites_showsEmptyMessage() {
        showScreen()

        compose.onNodeWithText("You have no registered campsites.").assertIsDisplayed()
        compose.onNodeWithText("Register a campsite to start managing its reservations.").assertIsDisplayed()
    }

    /**
     * Shows a different message when campsites have no reservations.
     */
    @Test
    fun noReservations_showsEmptyMessage() {
        campsiteRepository.campsites = listOf(
            campsite("empty", "Empty Campsite")
        )

        showScreen()

        compose.onNodeWithText("None of your campsites have reservations.").assertIsDisplayed()
        compose.onNodeWithText("Empty Campsite").assertDoesNotExist()
    }

    /**
     * Shows only campsites with reservations and the correct counts.
     */
    @Test
    fun overview_showsCampsitesAndCounts() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines"),
            campsite("empty", "Empty Campsite")
        )

        reservationRepository.reservations = listOf(
            reservation("active", "banff", "2027-06-12", "2027-06-15"),
            reservation("past", "banff", "2027-06-01", "2027-06-05"),
            reservation(
                "cancelled",
                "banff",
                "2027-07-01",
                "2027-07-05",
                ReservationStatus.CANCELLED
            )
        )

        showScreen()

        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText("Active reservations: 1").assertIsDisplayed()
        compose.onNodeWithText("Total reservations: 3").assertIsDisplayed()
        compose.onNodeWithText("Empty Campsite").assertDoesNotExist()
    }

    /**
     * Clicking a campsite shows its reservations and filter chips.
     */
    @Test
    fun campsiteClick_opensReservationList() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines")
        )

        reservationRepository.reservations = listOf(
            reservation("active", "banff", "2027-06-12", "2027-06-15")
        )

        showScreen()

        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("All").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("Back to Campsites").assertIsDisplayed()
    }

    /**
     * Multiple filters work and All resets the selection.
     */
    @Test
    fun filters_multiSelectAndReset() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines")
        )

        reservationRepository.reservations = listOf(
            reservation("active", "banff", "2027-06-12", "2027-06-15"),
            reservation("past", "banff", "2027-06-01", "2027-06-05"),
            reservation(
                "cancelled",
                "banff",
                "2027-07-01",
                "2027-07-05",
                ReservationStatus.CANCELLED
            )
        )

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        // Both the chip and reservation status can display "Active".
        compose.onAllNodesWithText("Active").onFirst().performClick()

        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertDoesNotExist()

        compose.onNodeWithText("Past").performClick()

        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
        compose.onNodeWithText("2027-07-01 to 2027-07-05").assertDoesNotExist()

        compose.onNodeWithText("All").performClick()

        compose.onNodeWithText("2027-07-01 to 2027-07-05").assertIsDisplayed()
    }

    /**
     * A filter without matching reservations displays an empty message.
     */
    @Test
    fun filters_noMatchesShowsMessage() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines")
        )

        reservationRepository.reservations = listOf(
            reservation("past", "banff", "2027-06-01", "2027-06-05")
        )

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("Active").performClick()

        compose.onNodeWithText("No reservations match the selected filters.").assertIsDisplayed()
    }

    /**
     * Reservation details are displayed and Back returns to the list.
     */
    @Test
    fun reservationClick_showsDetailsAndBack() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines")
        )

        reservationRepository.reservations = listOf(
            reservation("booking-1", "banff", "2027-06-12", "2027-06-15")
        )

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").performClick()

        compose.onNodeWithText("Reservation information").assertIsDisplayed()
        compose.onNodeWithText("Reservation ID: booking-1").assertIsDisplayed()

        compose.onNodeWithText("Back to Reservations").performClick()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()

        compose.onNodeWithText("Back to Campsites").performClick()
        compose.onNodeWithText("Total reservations: 1").assertIsDisplayed()
    }

    /**
     * Selecting another campsite resets the reservation filters.
     */
    @Test
    fun changingCampsite_resetsFilters() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines"),
            campsite("jasper", "Jasper Lake")
        )

        reservationRepository.reservations = listOf(
            reservation("banff-active", "banff", "2027-06-12", "2027-06-15"),
            reservation("jasper-past", "jasper", "2027-06-01", "2027-06-05")
        )

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onAllNodesWithText("Active").onFirst().performClick()

        compose.onNodeWithText("Back to Campsites").performClick()
        compose.onNodeWithText("Jasper Lake").performClick()

        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
    }

    /**
     * Displays the screen with in-memory repositories.
     */
    private fun showScreen() {
        val model = OwnerReservationsModel()

        val controller = OwnerReservationsController(
            model = model,
            campsiteRepository = campsiteRepository,
            reservationRepository = reservationRepository,
            today = { "2027-06-10" }
        )

        val view = OwnerReservationsView(model, controller)

        compose.setContent {
            view.Display(ownerId = ownerId, onBack = {})
        }

        compose.waitForIdle()
    }

    /**
     * Creates a campsite for UI testing.
     */
    private fun campsite(
        id: String,
        name: String
    ) = Campsite(
        id = id,
        name = name,
        location = "Alberta",
        ownerId = ownerId,
        rules = "Quiet hours after 10 PM",
        cancellationPolicy = "Cancel seven days ahead"
    )

    /**
     * Creates a reservation for UI testing.
     */
    private fun reservation(
        id: String,
        campsiteId: String,
        from: String,
        to: String,
        status: ReservationStatus = ReservationStatus.CONFIRMED
    ) = Reservation(
        id = id,
        entrantId = "test-entrant",
        campsiteId = campsiteId,
        dates = DateRange(from, to),
        status = status
    )

    /**
     * Fake campsite repository matching the real project interface.
     */
    private class FakeCampsiteRepository : CampsiteRepository {

        var campsites: List<Campsite> = emptyList()

        override suspend fun getCampsites(): List<Campsite> {
            return campsites
        }

        override suspend fun getCampsite(id: String): Campsite? {
            return campsites.firstOrNull { it.id == id }
        }

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> {
            return campsites.filter { it.ownerId == ownerId }
        }

        override suspend fun saveCampsite(campsite: Campsite): String {
            return campsite.id
        }

        override suspend fun deleteCampsite(id: String) {
            // Not needed by these tests.
        }
    }

    /**
     * Fake reservation repository matching the real project interface.
     */
    private class FakeReservationRepository : ReservationRepository {

        var reservations: List<Reservation> = emptyList()

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> {
            return reservations.filter { it.entrantId == entrantId }
        }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> {
            return reservations.filter { it.campsiteId == campsiteId }
        }

        override suspend fun createReservation(reservation: Reservation): String {
            return reservation.id
        }
    }
}

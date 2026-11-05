package com.example.camply.reservations.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
 * Compose UI tests for the active reservations list (US 01.06.01).
 */
class ActiveReservationsViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var campsiteRepository: TestCampsiteRepository

    private val campsite = Campsite(id = CAMPSITE_ID, name = "Banff Pines", location = "Banff")

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
        compose.onNodeWithText("Active").assertIsDisplayed()
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

        compose.onNodeWithText("You have no active reservations.").assertIsDisplayed()
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

    private fun reservation(id: String, from: String, to: String) = Reservation(
        id = id,
        entrantId = ENTRANT_ID,
        campsiteId = CAMPSITE_ID,
        dates = DateRange(from, to),
        status = ReservationStatus.CONFIRMED
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

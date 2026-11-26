
package com.example.camply.reservations.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.reservations.controller.OwnerReservationsController
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationCancellationResult
import com.example.camply.reservations.repository.ReservationRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.collections.filter

/**
 * Compose UI tests for owner reservation management (US 02.07.01, US 02.07.02, and US 02.07.03).
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
     * Overview filters remain selected when returning from a campsite.
     */
    @Test
    fun returningToOverview_preservesFilters() {
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
        compose.onNodeWithText("Active").assertIsSelected()
        compose.onNodeWithText("Jasper Lake").assertDoesNotExist()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("Jasper Lake").performClick()

        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
    }

    /**
     * Owners can browse past campsites, inspect a completed stay, and return to the category.
     */
    @Test
    fun pastCategory_filtersOverviewCountsAndReservationDetails() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines"),
            campsite("jasper", "Jasper Lake")
        )
        reservationRepository.reservations = listOf(
            reservation("completed-stay", "banff", "2027-06-01", "2027-06-05"),
            reservation("ends-today", "banff", "2027-06-08", "2027-06-10"),
            reservation("future", "jasper", "2027-07-01", "2027-07-05")
        )

        showScreen()
        compose.onNodeWithText("Past").performClick()
        compose.onNodeWithText("Past reservations: 1").assertIsDisplayed()
        compose.onNodeWithText("Jasper Lake").assertDoesNotExist()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("2027-06-08 to 2027-06-10").assertDoesNotExist()
        compose.onNodeWithText("Entrant: test-entrant").assertIsDisplayed()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").performClick()
        compose.onNodeWithText("Reservation ID: completed-stay").assertIsDisplayed()
        compose.onNodeWithText("Back to Reservations").performClick()
        compose.onNodeWithText("Back to Campsites").performClick()
        compose.onNodeWithText("Past").assertIsSelected()
        compose.onNodeWithText("Past reservations: 1").assertIsDisplayed()
        compose.onNodeWithText("Jasper Lake").assertDoesNotExist()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("Jasper Lake").assertIsDisplayed()
    }

    /**
     * An empty past category explains why no campsites appear and allows resetting it.
     */
    @Test
    fun pastCategory_withoutMatchesShowsEmptyState() {
        campsiteRepository.campsites = listOf(campsite("banff", "Banff Pines"))
        reservationRepository.reservations = listOf(
            reservation("future", "banff", "2027-07-01", "2027-07-05")
        )
        showScreen()
        compose.onNodeWithText("Past").performClick()
        compose.onNodeWithText("No campsites match the selected reservation filters.").assertIsDisplayed()
        compose.onNodeWithText("Banff Pines").assertDoesNotExist()
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
    }

    @Test
    fun cancelledCategory_filtersOwnedCampsitesCountsAndReservationDetails() {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines"),
            campsite("jasper", "Jasper Lake"),
            campsite("yoho", "Yoho Falls", owningUserId = "other-owner")
        )
        reservationRepository.reservations = listOf(
            reservation("cancelled-before-stay", "banff", "2027-07-01", "2027-07-05", ReservationStatus.CANCELLED)
                .copy(entrantId = "cancelled-entrant"),
            reservation("cancelled-older-stay", "banff", "2027-06-01", "2027-06-05", ReservationStatus.CANCELLED),
            reservation("active-stay", "banff", "2027-06-12", "2027-06-15"),
            reservation("jasper-rejected", "jasper", "2027-07-10", "2027-07-12", ReservationStatus.REJECTED),
            reservation("yoho-cancelled", "yoho", "2027-07-20", "2027-07-25", ReservationStatus.CANCELLED)
        )

        showScreen()
        compose.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText("All")))
            .performScrollToIndex(3)
        compose.onNodeWithText("Cancelled").performClick()
        compose.onNodeWithText("Cancelled").assertIsSelected()
        compose.onNodeWithText("Cancelled reservations: 2").assertIsDisplayed()
        compose.onNodeWithText("Jasper Lake").assertDoesNotExist()
        compose.onNodeWithText("Yoho Falls").assertDoesNotExist()

        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").assertIsDisplayed()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertDoesNotExist()
        compose.onNodeWithText("Entrant: cancelled-entrant").assertIsDisplayed()
        compose.onNodeWithText("2027-07-01 to 2027-07-05").performClick()

        compose.onNodeWithText("Reservation Details").assertIsDisplayed()
        compose.onNodeWithText("Reservation ID: cancelled-before-stay").assertIsDisplayed()
    }

    /** Cancelling confirmation leaves the reservation active and does not write. */
    @Test
    fun rejectReservation_cancelKeepsBooking() {
        openActiveReservation()
        compose.onNodeWithText("Reject reservation").performClick()
        compose.onNodeWithText("Reject this reservation?").assertIsDisplayed()
        org.junit.Assert.assertEquals(0, reservationRepository.rejectionCalls)
        compose.onNodeWithText("Keep reservation").performClick()
        compose.onNodeWithText("Reject this reservation?").assertDoesNotExist()
        compose.onNodeWithText("Active").assertIsDisplayed()
        org.junit.Assert.assertEquals(0, reservationRepository.rejectionCalls)
    }

    /** Rejection moves the reservation out of Active and into Rejected. */
    @Test
    fun rejectReservation_confirmUpdatesCategories() {
        openActiveReservation()
        compose.onNodeWithText("Reject reservation").performClick()
        compose.onNodeWithText("Reject").performClick()
        compose.onNodeWithText("Rejected").assertIsDisplayed()
        compose.onNodeWithText("Reject reservation").assertDoesNotExist()
        compose.onNodeWithText("Back to Reservations").performClick()
        compose.onNodeWithText("Active").performClick()
        compose.onNodeWithText("No reservations match the selected filters.").assertIsDisplayed()
        compose.onNodeWithText("All").performClick()
        compose.onAllNodesWithText("Rejected").onFirst().performClick()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").assertIsDisplayed()
        compose.onNodeWithText("Back to Campsites").performClick()
        compose.onNodeWithText("Rejected reservations: 1").assertIsDisplayed()
        org.junit.Assert.assertEquals(1, reservationRepository.rejectionCalls)
        org.junit.Assert.assertEquals(ReservationStatus.REJECTED, reservationRepository.reservations.single().status)
    }

    /** Failed writes keep the current details and allow a confirmed retry. */
    @Test
    fun rejectReservation_failureShowsErrorAndAllowsRetry() {
        openActiveReservation()
        reservationRepository.rejectionFailure = IllegalStateException("Could not save rejection.")
        compose.onNodeWithText("Reject reservation").performClick()
        compose.onNodeWithText("Reject").performClick()
        compose.onNodeWithText("Could not save rejection.").assertIsDisplayed()
        compose.onNodeWithText("Active").assertIsDisplayed()
        reservationRepository.rejectionFailure = null
        compose.onNodeWithText("Reject reservation").performClick()
        compose.onNodeWithText("Reject").performClick()
        compose.onNodeWithText("Rejected").assertIsDisplayed()
        compose.onNodeWithText("Could not save rejection.").assertDoesNotExist()
    }

    /** Completed reservations have no rejection action. */
    @Test
    fun rejectReservation_pastStayHasNoAction() {
        campsiteRepository.campsites = listOf(campsite("banff", "Banff Pines"))
        reservationRepository.reservations = listOf(
            reservation("past", "banff", "2027-06-01", "2027-06-05")
        )
        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("2027-06-01 to 2027-06-05").performClick()
        compose.onNodeWithText("Reject reservation").assertDoesNotExist()
    }

    /** Opens an owned active reservation through the real view navigation. */
    private fun openActiveReservation() {
        campsiteRepository.campsites = listOf(campsite("banff", "Banff Pines"))
        reservationRepository.reservations = listOf(
            reservation("booking", "banff", "2027-06-12", "2027-06-15")
        )
        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("2027-06-12 to 2027-06-15").performClick()
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
        name: String,
        owningUserId: String = ownerId
    ) = Campsite(
        id = id,
        name = name,
        location = "Alberta",
        ownerId = owningUserId,
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

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String {
            return campsite.id
        }

        override suspend fun getCampsitePreview(
            campsiteId: String,
            photoId: String
        ): ByteArray? = null

        override suspend fun getCampsitePhoto(
            campsiteId: String,
            photoId: String
        ): ByteArray? = null

        override suspend fun deleteCampsite(id: String) {
            // Not needed by these tests.
        }
    }

    /**
     * Fake reservation repository matching the real project interface.
     */
    private class FakeReservationRepository : ReservationRepository {

        var rejectionFailure: Exception? = null
        var rejectionCalls = 0
        var beforeReject: (suspend () -> Unit)? = null

        override suspend fun rejectReservation(
            reservationId: String, ownerId: String, today: String
        ): Reservation {
            rejectionCalls++
            beforeReject?.invoke()
            rejectionFailure?.let { throw it }
            val current = reservations.first { it.id == reservationId }
            require(current.isActive(today)) { "Only active reservations can be rejected." }
            val saved = current.copy(status = ReservationStatus.REJECTED)
            reservations = reservations.map { if (it.id == reservationId) saved else it }
            return saved
        }


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

        override suspend fun cancelReservation(
            reservationId: String,
            entrantId: String,
            today: String
        ): ReservationCancellationResult = ReservationCancellationResult.NOT_ELIGIBLE
    }
}

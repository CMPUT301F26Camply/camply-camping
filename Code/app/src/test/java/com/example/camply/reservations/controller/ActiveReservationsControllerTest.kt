package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for loading active reservations in ActiveReservationsController (US 01.06.01).
 */
class ActiveReservationsControllerTest {

    private lateinit var model: ActiveReservationsModel
    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var controller: ActiveReservationsController

    /**
     * Creates the model, repositories, and controller before each test, using a fixed date.
     */
    @Before
    fun setUp() {
        model = ActiveReservationsModel()
        reservationRepository = TestReservationRepository()
        campsiteRepository = TestCampsiteRepository()
        controller = ActiveReservationsController(
            model,
            reservationRepository,
            campsiteRepository,
            today = { "2027-06-10" }
        )
    }

    /**
     * Checks that cancelled and ended reservations are left out.
     */
    @Test
    fun loadReservations_mixedReservations_keepsOnlyActive() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15"),
            reservation("ended", from = "2027-06-01", to = "2027-06-05"),
            reservation("cancelled", from = "2027-06-12", to = "2027-06-15", status = ReservationStatus.CANCELLED)
        )

        controller.loadReservations(ENTRANT_ID)

        assertEquals(listOf("active"), model.reservations.map { it.reservation.id })
    }

    /**
     * Checks that the soonest stay comes first.
     */
    @Test
    fun loadReservations_activeReservations_sortsByStartDate() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("july", from = "2027-07-01", to = "2027-07-03"),
            reservation("june", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)

        assertEquals(listOf("june", "july"), model.reservations.map { it.reservation.id })
    }

    @Test
    fun loadReservations_validCampsite_includesCampsite() = runBlocking {
        val campsite = Campsite(id = CAMPSITE_ID, name = "Banff Pines")
        campsiteRepository.campsites = listOf(campsite)
        reservationRepository.reservations = listOf(
            reservation("first", from = "2027-06-12", to = "2027-06-15"),
            reservation("second", from = "2027-07-01", to = "2027-07-03")
        )

        controller.loadReservations(ENTRANT_ID)

        assertEquals(listOf(campsite, campsite), model.reservations.map { it.campsite })
        assertEquals(1, campsiteRepository.readCount)
    }

    /**
     * Checks that a reservation is kept when its campsite no longer exists.
     */
    @Test
    fun loadReservations_missingCampsite_keepsReservation() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)

        assertEquals(listOf("active"), model.reservations.map { it.reservation.id })
        assertNull(model.reservations.single().campsite)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that an entrant with no reservations gets an empty list and no error.
     */
    @Test
    fun loadReservations_noReservations_showsEmptyList() = runBlocking {
        controller.loadReservations(ENTRANT_ID)

        assertTrue(model.reservations.isEmpty())
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    @Test
    fun loadReservations_reservationRepositoryError_showsError() = runBlocking {
        reservationRepository.shouldFail = true

        controller.loadReservations(ENTRANT_ID)

        assertEquals("Could not read reservations.", model.errorMessage)
        assertFalse(model.isLoading)
    }

    @Test
    fun loadReservations_campsiteRepositoryError_showsError() = runBlocking {
        campsiteRepository.shouldFail = true
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)

        assertEquals("Could not read campsite.", model.errorMessage)
        assertFalse(model.isLoading)
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
     * Provides reservation data for controller tests without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {
        var reservations: List<Reservation> = emptyList()
        var shouldFail = false

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> {
            if (shouldFail) throw Exception("Could not read reservations.")
            return reservations.filter { it.entrantId == entrantId }
        }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"
    }

    /**
     * Provides campsite data for controller tests without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()
        var shouldFail = false
        var readCount = 0

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? {
            readCount++
            if (shouldFail) throw Exception("Could not read campsite.")
            return campsites.firstOrNull { it.id == id }
        }

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

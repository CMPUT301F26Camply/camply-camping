
package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.reservations.model.OwnerReservationsModel
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
 * Unit tests for owner reservation management (US 02.07.01).
 */
class OwnerReservationsControllerTest {

    private val ownerId = "test-owner"

    private lateinit var model: OwnerReservationsModel
    private lateinit var campsiteRepository: FakeCampsiteRepository
    private lateinit var reservationRepository: FakeReservationRepository
    private lateinit var controller: OwnerReservationsController

    @Before
    fun setUp() {
        model = OwnerReservationsModel()
        campsiteRepository = FakeCampsiteRepository()
        reservationRepository = FakeReservationRepository()

        controller = OwnerReservationsController(
            model,
            campsiteRepository,
            reservationRepository
        ) { "2027-06-10" }
    }

    /**
     * Loads only campsites belonging to the requested owner.
     */
    @Test
    fun loadAllReservations_onlyLoadsOwnedCampsites() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId),
            campsite("other", "Other Campsite", "different-owner"),
            campsite("jasper", "Jasper Lake", ownerId)
        )

        reservationRepository.reservations = listOf(
            reservation("one", "banff", "2027-06-12", "2027-06-15"),
            reservation("foreign", "other", "2027-06-12", "2027-06-15")
        )

        controller.loadAllReservations(ownerId)

        assertEquals(
            listOf("banff", "jasper"),
            model.campsites.map { it.campsite.id }
        )
        assertEquals(
            listOf("one"),
            model.campsites.first().reservations.map { it.id }
        )
        assertNull(model.errorMessage)
        assertFalse(model.isLoading)
    }

    /**
     * Excludes uninitialized reservations and sorts by start date.
     */
    @Test
    fun loadAllReservations_excludesUninitializedAndSorts() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId)
        )

        reservationRepository.reservations = listOf(
            reservation("later", "banff", "2027-07-01", "2027-07-05"),
            reservation(
                "uninitialized",
                "banff",
                "2027-06-01",
                "2027-06-05",
                ReservationStatus.UNINITIALIZED
            ),
            reservation("earlier", "banff", "2027-06-12", "2027-06-15")
        )

        controller.loadAllReservations(ownerId)

        assertEquals(
            listOf("earlier", "later"),
            model.campsites.single().reservations.map { it.id }
        )
    }

    /**
     * Hides campsites without reservations but keeps past-only campsites.
     */
    @Test
    fun getCampsitesWithReservations_hidesEmptyCampsites() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("empty", "Empty Campsite", ownerId),
            campsite("banff", "Banff Pines", ownerId)
        )

        reservationRepository.reservations = listOf(
            reservation("past", "banff", "2027-06-01", "2027-06-05")
        )

        controller.loadAllReservations(ownerId)

        val results = controller.getCampsitesWithReservations()

        assertEquals(1, results.size)
        assertEquals("banff", results.single().campsite.id)
        assertEquals(0, controller.getActiveCount(results.single()))
    }

    /**
     * Counts only confirmed reservations that have not ended.
     */
    @Test
    fun getActiveCount_countsConfirmedReservations() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId)
        )

        reservationRepository.reservations = listOf(
            reservation("future", "banff", "2027-06-12", "2027-06-15"),
            reservation("today", "banff", "2027-06-08", "2027-06-10"),
            reservation("past", "banff", "2027-06-01", "2027-06-05"),
            reservation(
                "cancelled",
                "banff",
                "2027-06-12",
                "2027-06-15",
                ReservationStatus.CANCELLED
            )
        )

        controller.loadAllReservations(ownerId)

        assertEquals(2, controller.getActiveCount(model.campsites.single()))
        assertEquals(4, model.campsites.single().reservations.size)
    }

    /**
     * Categorizes confirmed, cancelled, and rejected reservations.
     */
    @Test
    fun getReservationCategory_returnsCorrectCategories() {
        assertEquals(
            "Active",
            controller.getReservationCategory(
                reservation("a", "banff", "2027-06-12", "2027-06-15")
            )
        )
        assertEquals(
            "Past",
            controller.getReservationCategory(
                reservation("p", "banff", "2027-06-01", "2027-06-05")
            )
        )
        assertEquals(
            "Cancelled",
            controller.getReservationCategory(
                reservation(
                    "c", "banff", "2027-06-12", "2027-06-15",
                    ReservationStatus.CANCELLED
                )
            )
        )
        assertEquals(
            "Rejected",
            controller.getReservationCategory(
                reservation(
                    "r", "banff", "2027-06-12", "2027-06-15",
                    ReservationStatus.REJECTED
                )
            )
        )
    }

    /**
     * Supports multiple filters, toggling filters off, and All.
     */
    @Test
    fun onFilterSelected_supportsMultipleFiltersAndAll() {
        val items = listOf(
            reservation("active", "banff", "2027-06-12", "2027-06-15"),
            reservation("past", "banff", "2027-06-01", "2027-06-05"),
            reservation(
                "cancelled",
                "banff",
                "2027-06-12",
                "2027-06-15",
                ReservationStatus.CANCELLED
            )
        )

        assertEquals(3, controller.getFilteredReservations(items).size)

        controller.onFilterSelected("Active")
        assertEquals(
            listOf("active"),
            controller.getFilteredReservations(items).map { it.id }
        )

        controller.onFilterSelected("Past")
        assertEquals(
            listOf("active", "past"),
            controller.getFilteredReservations(items).map { it.id }
        )

        controller.onFilterSelected("Active")
        assertEquals(
            listOf("past"),
            controller.getFilteredReservations(items).map { it.id }
        )

        controller.onFilterSelected("All")
        assertTrue(model.selectedFilters.isEmpty())
        assertEquals(3, controller.getFilteredReservations(items).size)
    }

    /**
     * Selecting a new campsite clears filters and reservation selection.
     */
    @Test
    fun onCampsiteSelected_resetsFilters() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId),
            campsite("jasper", "Jasper Lake", ownerId)
        )

        val booking = reservation(
            "one", "banff", "2027-06-12", "2027-06-15"
        )
        reservationRepository.reservations = listOf(booking)

        controller.loadAllReservations(ownerId)
        controller.onCampsiteSelected("banff")
        controller.onFilterSelected("Past")
        controller.onReservationSelected(booking)

        assertEquals(booking, model.selectedReservation)

        controller.onCampsiteSelected("jasper")

        assertEquals("jasper", model.selectedCampsiteId)
        assertNull(model.selectedReservation)
        assertTrue(model.selectedFilters.isEmpty())
    }

    /**
     * Back navigates from details to reservations to campsite overview.
     */
    @Test
    fun onBack_returnsThroughScreenLevels() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId)
        )

        val booking = reservation(
            "one", "banff", "2027-06-12", "2027-06-15"
        )
        reservationRepository.reservations = listOf(booking)

        controller.loadAllReservations(ownerId)
        controller.onCampsiteSelected("banff")
        controller.onReservationSelected(booking)

        controller.onBack()

        assertNull(model.selectedReservation)
        assertEquals("banff", model.selectedCampsiteId)

        controller.onBack()

        assertNull(model.selectedCampsiteId)
    }

    /**
     * Repository failures show an error instead of leaving loading active.
     */
    @Test
    fun loadAllReservations_repositoryErrorUpdatesModel() = runBlocking {
        campsiteRepository.shouldFail = true

        controller.loadAllReservations(ownerId)

        assertEquals(
            "Could not read campsites.",
            model.errorMessage
        )
        assertFalse(model.isLoading)
    }

    /**
     * Creates a campsite for unit testing.
     */
    private fun campsite(
        id: String,
        name: String,
        owner: String
    ) = Campsite(
        id = id,
        name = name,
        ownerId = owner
    )

    /**
     * Creates a reservation for unit testing.
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
        var shouldFail = false

        override suspend fun getCampsites(): List<Campsite> {
            return campsites
        }

        override suspend fun getCampsite(id: String): Campsite? {
            return campsites.firstOrNull { it.id == id }
        }

        override suspend fun getCampsitesByOwner(
            ownerId: String
        ): List<Campsite> {
            if (shouldFail) {
                throw Exception("Could not read campsites.")
            }

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

        override suspend fun getReservationsForEntrant(
            entrantId: String
        ): List<Reservation> {
            return reservations.filter { it.entrantId == entrantId }
        }

        override suspend fun getReservationsForCampsite(
            campsiteId: String
        ): List<Reservation> {
            return reservations.filter { it.campsiteId == campsiteId }
        }

        override suspend fun createReservation(
            reservation: Reservation
        ): String {
            return reservation.id
        }
    }
}

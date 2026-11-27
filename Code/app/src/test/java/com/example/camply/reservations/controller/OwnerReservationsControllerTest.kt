
package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationCancellationResult
import com.example.camply.reservations.repository.ReservationRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for owner reservation management (US 02.07.01 and US 02.07.02).
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
            reservationRepository,
            entrantNameRepository = { null }
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
     * Selecting another campsite preserves category filters and clears reservation selection.
     */
    @Test
    fun onCampsiteSelected_preservesFilters() = runBlocking {
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
        assertEquals(setOf("Past"), model.selectedFilters)
    }

    /**
     * Past includes only completed confirmed stays, with matching campsite counts.
     */
    @Test
    fun pastFilter_limitsCampsitesAndCountsToCompletedStays() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("banff", "Banff Pines", ownerId),
            campsite("jasper", "Jasper Lake", ownerId),
            campsite("empty", "Empty Campsite", ownerId)
        )
        reservationRepository.reservations = listOf(
            reservation("past-1", "banff", "2027-06-01", "2027-06-05"),
            reservation("past-2", "banff", "2027-06-06", "2027-06-09"),
            reservation("ends-today", "banff", "2027-06-08", "2027-06-10"),
            reservation("cancelled", "banff", "2027-06-01", "2027-06-05", ReservationStatus.CANCELLED),
            reservation("rejected", "banff", "2027-06-01", "2027-06-05", ReservationStatus.REJECTED),
            reservation("future", "jasper", "2027-07-01", "2027-07-05")
        )

        controller.loadAllReservations(ownerId)
        controller.onFilterSelected("Past")

        val entries = controller.getCampsitesWithReservations()
        assertEquals(listOf("banff"), entries.map { it.campsite.id })
        assertEquals(2, controller.getFilteredCount(entries.single()))
        assertEquals(
            listOf("past-1", "past-2"),
            controller.getFilteredReservations(entries.single().reservations).map { it.id }
        )

        controller.onCampsiteSelected("banff")
        controller.onReservationSelected(entries.single().reservations.first())
        controller.onBack()
        controller.onBack()
        assertEquals(setOf("Past"), model.selectedFilters)
        assertEquals(listOf("banff"), controller.getCampsitesWithReservations().map { it.campsite.id })

        controller.onFilterSelected("All")
        assertEquals(listOf("banff", "jasper"), controller.getCampsitesWithReservations().map { it.campsite.id })
        assertEquals(5, controller.getFilteredCount(model.campsites.first()))
    }

    /**
     * A category with no matching stays produces an empty overview.
     */
    @Test
    fun pastFilter_withoutCompletedStaysReturnsNoCampsites() = runBlocking {
        campsiteRepository.campsites = listOf(campsite("banff", "Banff Pines", ownerId))
        reservationRepository.reservations = listOf(
            reservation("future", "banff", "2027-07-01", "2027-07-05")
        )
        controller.loadAllReservations(ownerId)
        controller.onFilterSelected("Past")

        assertTrue(controller.getCampsitesWithReservations().isEmpty())
        assertEquals(0, controller.getFilteredCount(model.campsites.single()))
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

    /** Confirmation can be dismissed or omitted without writing anything. */
    @Test
    fun rejection_requiresConfirmation() = runBlocking {
        selectActiveReservation()
        controller.confirmRejection()
        assertEquals(0, reservationRepository.rejectionCalls)

        controller.requestRejection()
        assertEquals("booking", model.pendingRejection?.id)
        assertEquals(0, reservationRepository.rejectionCalls)
        controller.dismissRejection()
        controller.confirmRejection()
        assertNull(model.pendingRejection)
        assertEquals(ReservationStatus.CONFIRMED, model.selectedReservation?.status)
        assertEquals(0, reservationRepository.rejectionCalls)
    }

    /** A successful rejection changes details, counts, filters, and subsequently loaded data. */
    @Test
    fun rejection_updatesSavedReservationAndCategories() = runBlocking {
        selectActiveReservation()
        controller.onFilterSelected("Active")
        controller.requestRejection()
        controller.confirmRejection()

        assertEquals(1, reservationRepository.rejectionCalls)
        assertEquals(ReservationStatus.REJECTED, model.selectedReservation?.status)
        assertFalse(model.isRejecting)
        assertFalse(controller.canRejectSelectedReservation())
        assertNull(model.pendingRejection)
        assertEquals(0, controller.getActiveCount(model.campsites.single()))
        assertTrue(controller.getCampsitesWithReservations().isEmpty())

        controller.onFilterSelected("All")
        controller.onFilterSelected("Rejected")
        assertEquals(1, controller.getFilteredCount(model.campsites.single()))
        controller.loadAllReservations(ownerId)
        assertEquals(ReservationStatus.REJECTED, model.campsites.single().reservations.single().status)
    }

    /** A failed save retains details and can be retried after a new confirmation. */
    @Test
    fun rejection_failurePreservesReservationAndAllowsRetry() = runBlocking {
        selectActiveReservation()
        reservationRepository.rejectionFailure = IllegalStateException("Could not save rejection.")
        controller.requestRejection()
        controller.confirmRejection()

        assertEquals("Could not save rejection.", model.rejectionError)
        assertNull(model.errorMessage)
        assertEquals(ReservationStatus.CONFIRMED, model.selectedReservation?.status)
        assertFalse(model.isRejecting)
        assertTrue(controller.canRejectSelectedReservation())
        reservationRepository.rejectionFailure = null
        controller.requestRejection()
        controller.confirmRejection()
        assertEquals(ReservationStatus.REJECTED, model.selectedReservation?.status)
        assertNull(model.rejectionError)
    }

    /** Completed or nonconfirmed stays cannot enter the rejection flow. */
    @Test
    fun rejection_blocksInactiveReservations() = runBlocking {
        selectActiveReservation()
        val active = model.selectedReservation!!
        val invalid = listOf(
            active.copy(dates = DateRange("2027-06-01", "2027-06-05")),
            active.copy(status = ReservationStatus.CANCELLED),
            active.copy(status = ReservationStatus.REJECTED),
            active.copy(status = ReservationStatus.UNINITIALIZED)
        )
        for (reservation in invalid) {
            model.updateSelectedReservation(reservation)
            assertFalse(controller.canRejectSelectedReservation())
            controller.requestRejection()
            controller.confirmRejection()
            assertNull(model.pendingRejection)
        }
        assertEquals(0, reservationRepository.rejectionCalls)
    }

    /** Caller-supplied reservations from another campsite cannot be rejected. */
    @Test
    fun rejection_blocksUnownedReservations() = runBlocking {
        selectActiveReservation()
        model.updateSelectedReservation(model.selectedReservation!!.copy(campsiteId = "foreign"))
        controller.requestRejection()
        controller.confirmRejection()
        assertFalse(controller.canRejectSelectedReservation())
        assertNull(model.pendingRejection)
        assertEquals(0, reservationRepository.rejectionCalls)
    }

    /** Repeated clicks while saving cannot start a second write or leave the selected details. */
    @Test
    fun rejection_blocksDuplicateSubmissions() = runBlocking {
        selectActiveReservation()
        val release = CompletableDeferred<Unit>()
        reservationRepository.beforeReject = { release.await() }
        controller.requestRejection()
        val firstSave = launch { controller.confirmRejection() }
        yield()
        assertTrue(model.isRejecting)
        controller.requestRejection()
        controller.confirmRejection()
        controller.onBack()
        assertEquals("booking", model.selectedReservation?.id)
        assertEquals(1, reservationRepository.rejectionCalls)
        release.complete(Unit)
        firstSave.join()
        assertFalse(model.isRejecting)
    }

    /** Repository validation must prevent overwriting an externally cancelled reservation. */
    @Test
    fun rejection_staleReservationIsNotOverwritten() = runBlocking {
        selectActiveReservation()
        controller.requestRejection()
        reservationRepository.reservations = reservationRepository.reservations.map {
            it.copy(status = ReservationStatus.CANCELLED)
        }
        controller.confirmRejection()
        assertEquals(ReservationStatus.CANCELLED, reservationRepository.reservations.single().status)
        assertEquals("Only active reservations can be rejected.", model.rejectionError)
        assertFalse(model.isRejecting)
    }

    /** Cancelling the save propagates coroutine cancellation and releases the busy state. */
    @Test
    fun rejection_cancellationReleasesBusyState() = runBlocking {
        selectActiveReservation()
        reservationRepository.rejectionFailure = kotlin.coroutines.cancellation.CancellationException("Stopped")
        controller.requestRejection()
        var cancelled = false
        try {
            controller.confirmRejection()
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertFalse(model.isRejecting)
        assertEquals(ReservationStatus.CONFIRMED, model.selectedReservation?.status)
    }

    /** Loads a realistic owned reservation and opens its details. */
    private suspend fun selectActiveReservation() {
        campsiteRepository.campsites = listOf(campsite("banff", "Banff Pines", ownerId))
        reservationRepository.reservations = listOf(
            reservation("booking", "banff", "2027-06-12", "2027-06-15")
        )
        controller.loadAllReservations(ownerId)
        controller.onCampsiteSelected("banff")
        controller.onReservationSelected(reservationRepository.reservations.single())
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

        override suspend fun cancelReservation(
            reservationId: String,
            entrantId: String,
            today: String
        ): ReservationCancellationResult = ReservationCancellationResult.NOT_ELIGIBLE
    }
}

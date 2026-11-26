package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.ActiveReservationItem
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationCancellationResult
import com.example.camply.reservations.repository.ReservationRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for loading and cancelling reservations in ActiveReservationsController
 * (US 01.06.01, US 01.07.01, Us 01.07.02).
 */
class ActiveReservationsControllerTest {

    private lateinit var model: ActiveReservationsModel
    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var notificationRepository: TestNotificationRepository
    private lateinit var controller: ActiveReservationsController

    /**
     * Creates the model, repositories, and controller before each test, using a fixed date.
     */
    @Before
    fun setUp() {
        model = ActiveReservationsModel()
        reservationRepository = TestReservationRepository()
        campsiteRepository = TestCampsiteRepository()
        notificationRepository = TestNotificationRepository()
        controller = ActiveReservationsController(
            model,
            reservationRepository,
            campsiteRepository,
            NotificationService(notificationRepository),
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

    /**
     * Checks that reservations sharing a campsite are all kept and the campsite is read once.
     */
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

    /**
     * Checks that a failed reservation load shows an error.
     */
    @Test
    fun loadReservations_reservationRepositoryError_showsError() = runBlocking {
        reservationRepository.shouldFail = true

        controller.loadReservations(ENTRANT_ID)

        assertEquals("Could not read reservations.", model.errorMessage)
        assertFalse(model.isLoading)
    }

    /**
     * Checks that a failed campsite lookup shows an error.
     */
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

    /**
     * Checks that selecting a reservation stores it in the model.
     */
    @Test
    fun onReservationSelected_validItem_selectsReservation() {
        val item = selectedItem()

        controller.onReservationSelected(item)

        assertEquals(item, model.selectedReservation)
    }

    /**
     * Checks that going back to the list clears the selection.
     */
    @Test
    fun onBackToList_reservationSelected_clearsSelection() {
        controller.onReservationSelected(selectedItem())

        controller.onBackToList()

        assertNull(model.selectedReservation)
    }

    /**
     * Checks that loading the reservations again returns to the list.
     */
    @Test
    fun loadReservations_reservationSelected_clearsSelection() = runBlocking {
        controller.onReservationSelected(selectedItem())

        controller.loadReservations(ENTRANT_ID)

        assertNull(model.selectedReservation)
    }

    /**
     * Checks that cancelling an active reservation updates its status,
     * removes it from the active lists, and preserves the selected reservation.
     */
    @Test
    fun cancelReservation_activeReservation_updatesStatus() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onFilterSelected("Active")
        controller.onReservationSelected(model.reservations.single())

        controller.cancelReservation(ENTRANT_ID)

        assertEquals(ReservationStatus.CANCELLED, model.selectedReservation?.reservation?.status)
        assertEquals(ReservationStatus.CANCELLED, model.allReservations.single().reservation.status)
        assertEquals(ReservationStatus.CANCELLED, reservationRepository.reservations.single().status)
        assertTrue(model.reservations.isEmpty())
        assertTrue(model.filteredReservations.isEmpty())
        assertFalse(model.isCancelling)
        assertNull(model.cancellationError)
    }

    /**
     * Checks that a cancelled reservation retains its status after reloading
     * and appears under the Cancelled filter.
     */
    @Test
    fun cancelReservation_reloadReservation_keepsCancelledStatus() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onReservationSelected(model.reservations.single())

        controller.cancelReservation(ENTRANT_ID)
        controller.loadReservations(ENTRANT_ID)
        controller.onFilterSelected("Cancelled")

        assertEquals(ReservationStatus.CANCELLED, model.filteredReservations.single().reservation.status)
        assertTrue(model.reservations.isEmpty())
        assertNull(model.selectedReservation)
    }

    /**
     * Checks that a past reservation cannot be cancelled and no cancellation notification is sent.
     */
    @Test
    fun cancelReservation_pastReservation_showsError() = runBlocking {
        campsiteRepository.campsites = listOf(
            Campsite(id = CAMPSITE_ID, name = "Banff Pines", ownerId = "owner1")
        )
        reservationRepository.reservations = listOf(
            reservation("ended", from = "2027-06-01", to = "2027-06-05")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onReservationSelected(model.allReservations.single())

        controller.cancelReservation(ENTRANT_ID)

        assertEquals(ReservationStatus.CONFIRMED, model.selectedReservation?.reservation?.status)
        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.reservations.single().status)
        assertEquals("This reservation can no longer be cancelled.", model.cancellationError)
        assertFalse(model.isCancelling)
        assertTrue(notificationRepository.records.isEmpty())
    }

    /**
     * Checks that a failed cancellation displays an error without changing
     * the reservation's status.
     */
    @Test
    fun cancelReservation_repositoryError_showsError() = runBlocking {
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onReservationSelected(model.reservations.single())

        reservationRepository.shouldFail = true

        controller.cancelReservation(ENTRANT_ID)

        assertEquals(ReservationStatus.CONFIRMED, model.selectedReservation?.reservation?.status)
        assertEquals(ReservationStatus.CONFIRMED, reservationRepository.reservations.single().status)
        assertEquals("Could not cancel reservation.", model.cancellationError)
        assertFalse(model.isCancelling)
    }

    /**
     * Checks that successfully cancelling a reservation sends the entrant
     * a cancellation confirmation notification.
     */
    @Test
    fun cancelReservation_success_sendsNotification() = runBlocking {
        campsiteRepository.campsites = listOf(
            Campsite(id = CAMPSITE_ID, name = "Banff Pines", ownerId = "owner1")
        )
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onReservationSelected(model.reservations.single())

        controller.cancelReservation(ENTRANT_ID)

        val notification = notificationRepository.records.single()

        assertEquals(ENTRANT_ID, notification.recipientId)
        assertEquals("owner1", notification.senderId)
        assertEquals("active", notification.reservationId)
        assertEquals(NotificationType.RESERVATION_CANCELLED, notification.type)
    }

    /**
     * Checks that a notification failure does not undo a successful cancellation
     * or display a cancellation error.
     */
    @Test
    fun cancelReservation_notificationError_keepsCancelledStatus() = runBlocking {
        campsiteRepository.campsites = listOf(
            Campsite(id = CAMPSITE_ID, name = "Banff Pines", ownerId = "owner1")
        )
        reservationRepository.reservations = listOf(
            reservation("active", from = "2027-06-12", to = "2027-06-15")
        )

        controller.loadReservations(ENTRANT_ID)
        controller.onReservationSelected(model.reservations.single())

        notificationRepository.shouldFail = true

        controller.cancelReservation(ENTRANT_ID)

        assertEquals(ReservationStatus.CANCELLED, model.selectedReservation?.reservation?.status)
        assertEquals(ReservationStatus.CANCELLED, reservationRepository.reservations.single().status)
        assertTrue(notificationRepository.records.isEmpty())
        assertFalse(model.isCancelling)
        assertNull(model.cancellationError)
    }

    private fun selectedItem() =
        ActiveReservationItem(reservation("selected", from = "2027-06-12", to = "2027-06-15"), campsite = null)

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

        override suspend fun rejectReservation(
            reservationId: String, ownerId: String, today: String
        ): Reservation = error("Rejection is not used by this test.")

        var reservations: List<Reservation> = emptyList()
        var shouldFail = false

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> {
            if (shouldFail) throw Exception("Could not read reservations.")
            return reservations.filter { it.entrantId == entrantId }
        }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"

        override suspend fun cancelReservation(
            reservationId: String,
            entrantId: String,
            today: String
        ): ReservationCancellationResult {
            if (shouldFail) throw Exception("Could not cancel reservation.")

            val reservation = reservations.firstOrNull { it.id == reservationId }

            if (reservation == null || reservation.entrantId != entrantId || !reservation.isActive(today)) {
                return ReservationCancellationResult.NOT_ELIGIBLE
            }

            reservations = reservations.map {
                if (it.id == reservationId) {
                    it.copy(status = ReservationStatus.CANCELLED)
                } else {
                    it
                }
            }

            return ReservationCancellationResult.CANCELLED
        }
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


        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = campsite.id

        override suspend fun getCampsitePreview(
            campsiteId: String,
            photoId: String
        ): ByteArray? = null

        override suspend fun getCampsitePhoto(
            campsiteId: String,
            photoId: String
        ): ByteArray? = null

        override suspend fun deleteCampsite(id: String) = Unit

    }

    /**
     * Records notifications for controller tests without accessing Firestore.
     */
    private class TestNotificationRepository : NotificationRepository {
        val records = mutableListOf<NewNotification>()
        var shouldFail = false

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            if (shouldFail) throw Exception("Could not send notification.")
            records.add(notification)
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by controller tests")

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by controller tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by controller tests")
    }

    private companion object {
        const val ENTRANT_ID = "entrant1"
        const val CAMPSITE_ID = "campsite1"
    }
}

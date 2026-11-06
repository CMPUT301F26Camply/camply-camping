package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for reservation date validation in ReservationBookingController.
 */
class ReservationBookingControllerTest {

    private lateinit var controller: ReservationBookingController
    private lateinit var repository: TestReservationRepository
    private lateinit var campsite: Campsite

    /**
     * Creates the controller, repository and campsite before each test.
     */
    @Before
    fun setUp() {
        repository = TestReservationRepository()
        controller = ReservationBookingController(BookingService(), repository, notificationService())

        campsite = Campsite(
            id = "1",
            name = "Banff Pines",
            availability = listOf(DateRange("2027-06-01", "2027-06-10"))
        )
    }

    /**
     * Checks that available dates without reservation conflicts are accepted.
     */
    @Test
    fun validateDates_availableDates_returnsNull() = runBlocking {
        val selectedDates = DateRange("2027-06-03", "2027-06-05")

        val error = controller.validateDates(campsite, selectedDates)

        assertNull(error)
    }

    /**
     * Checks that dates outside campsite availability are rejected.
     */
    @Test
    fun validateDates_unavailableDates_returnsError() = runBlocking {
        val selectedDates = DateRange("2027-06-12", "2027-06-15")

        val error = controller.validateDates(campsite, selectedDates)

        assertEquals("Selected dates are outside the campsite's availability.",error)
    }

    /**
     * Checks that dates overlapping a confirmed reservation are rejected.
     */
    @Test
    fun validateDates_overlappingReservation_returnsError() = runBlocking {
        repository.reservations = listOf(
            Reservation(
                campsiteId = "1",
                dates = DateRange("2027-06-04", "2027-06-07"),
                status = ReservationStatus.CONFIRMED
            )
        )

        val selectedDates = DateRange("2027-06-05", "2027-06-08")

        val error = controller.validateDates(campsite, selectedDates)

        assertEquals("Selected dates overlap an existing reservation.",error)
    }

    /**
     * Checks that cancelled reservations do not block new bookings.
     */
    @Test
    fun validateDates_cancelledReservation_returnsNull() = runBlocking {
        repository.reservations = listOf(
            Reservation(
                campsiteId = "1",
                dates = DateRange("2027-06-04", "2027-06-07"),
                status = ReservationStatus.CANCELLED
            )
        )

        val selectedDates = DateRange("2027-06-05", "2027-06-08")

        val error = controller.validateDates(campsite, selectedDates)

        assertNull(error)
    }

    /**
     * Checks that valid dates update the booking model successfully.
     */
    @Test
    fun checkDates_availableDates_updatesModel() = runBlocking {
        val model = ReservationBookingModel()
        val controller = ReservationBookingController(BookingService(), repository, notificationService(), model)

        controller.checkDates(campsite, DateRange("2027-06-03", "2027-06-05"))

        assertTrue(model.datesValid)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that unavailable dates update the booking model with an error.
     */
    @Test
    fun checkDates_unavailableDates_updatesModel() = runBlocking {
        val model = ReservationBookingModel()
        val controller = ReservationBookingController(BookingService(), repository, notificationService(), model)

        controller.checkDates(campsite, DateRange("2027-06-12", "2027-06-15"))

        assertFalse(model.datesValid)
        assertFalse(model.isLoading)
        assertEquals("Selected dates are outside the campsite's availability.",model.errorMessage)
    }

    /**
     * Checks that a reservation is rejected and not saved without a rules acknowledgement (US 02.11.01).
     */
    @Test
    fun confirmReservation_rulesNotAcknowledged_returnsNullAndShowsError() = runBlocking {
        val model = ReservationBookingModel()
        val controller = ReservationBookingController(BookingService(), repository, notificationService(), model)
        val reservation = Reservation(campsiteId = "1", rulesAcknowledged = false)

        val reservationId = controller.confirmReservation(campsite, reservation)

        assertNull(reservationId)
        assertEquals(
            "You must confirm that you have read the campsite rules and policies.",
            model.errorMessage
        )
        assertFalse(model.isLoading)
        assertTrue(repository.reservations.isEmpty())
    }

    /**
     * Checks that a saved reservation sends its confirmation to the entrant (US 01.05.01).
     */
    @Test
    fun notifyReservationConfirmed_validReservation_sendsConfirmation() = runBlocking {
        val notifications = TestNotificationRepository()
        val controller = ReservationBookingController(
            BookingService(),
            repository,
            NotificationService(notifications)
        )

        controller.notifyReservationConfirmed(ownedCampsite, savedReservation, "reservation1")

        val notification = notifications.records.single()
        assertEquals("entrant1", notification.recipientId)
        assertEquals("owner1", notification.senderId)
        assertEquals("reservation1", notification.reservationId)
    }

    /**
     * Checks that a failed send does not throw, because the reservation is already saved.
     */
    @Test
    fun notifyReservationConfirmed_sendFails_doesNotThrow() = runBlocking {
        val notifications = TestNotificationRepository()
        notifications.shouldFail = true
        val controller = ReservationBookingController(
            BookingService(),
            repository,
            NotificationService(notifications)
        )

        controller.notifyReservationConfirmed(ownedCampsite, savedReservation, "reservation1")

        assertTrue(notifications.records.isEmpty())
    }

    private val ownedCampsite = Campsite(id = "1", name = "Banff Pines", ownerId = "owner1")

    private val savedReservation = Reservation(
        entrantId = "entrant1",
        campsiteId = "1",
        dates = DateRange("2027-06-03", "2027-06-05")
    )

    private fun notificationService() = NotificationService(TestNotificationRepository())

    /**
     * Provides reservation data for controller tests without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {

        var reservations: List<Reservation> = emptyList()

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
            reservations.filter { it.entrantId == entrantId }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String {
            reservations = reservations + reservation
            return "testReservationId"
        }
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
}

package com.example.camply.reservations.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.repository.DeclineResult
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.lottery.repository.LotteryRepository
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
    private lateinit var lotteryRepository: TestLotteryRepository
    private lateinit var lotteryEntryRepository: TestLotteryEntryRepository
    private lateinit var campsite: Campsite

    /**
     * Creates the controller, repository and campsite before each test.
     */
    @Before
    fun setUp() {
        repository = TestReservationRepository()
        lotteryRepository = TestLotteryRepository()
        lotteryEntryRepository = TestLotteryEntryRepository()

        controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository
        )

        campsite = Campsite(
            id = "1",
            name = "Banff Pines",
            ownerId = "owner1",
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
        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )
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
        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )
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
        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )
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
     * Checks that an entrant without a winning lottery entry cannot confirm a reservation.
     * US 01.04.01.
     */
    @Test
    fun confirmReservation_noWinningEntry_doesNotSave() = runBlocking {
        val model = ReservationBookingModel()
        lotteryRepository.lottery = Lottery(
            id = "lottery1",
            campsiteIds = listOf("1")
        )

        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )

        val reservation = Reservation(
            entrantId = "entrant1",
            campsiteId = "1",
            lotteryId = "lottery1",
            dates = DateRange("2027-06-03", "2027-06-05"),
            rulesAcknowledged = true
        )

        val result = controller.confirmReservation(campsite, reservation)

        assertNull(result)
        assertEquals(
            "You do not have an available winning opportunity.",
            model.errorMessage
        )
        assertTrue(repository.reservations.isEmpty())
    }

    /**
     * Checks that a saved reservation sends its confirmation to the entrant (US 01.05.01).
     */
    @Test
    fun notifyReservationConfirmed_validReservation_sendsConfirmation() = runBlocking {
        val notifications = TestNotificationRepository()
        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = NotificationService(notifications),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository
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
            bookingService = BookingService(),
            repository = repository,
            notificationService = NotificationService(notifications),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository
        )

        controller.notifyReservationConfirmed(ownedCampsite, savedReservation, "reservation1")

        assertTrue(notifications.records.isEmpty())
    }


    /**
     * Checks that a lottery winner can confirm a reservation and receive a notification.
     * US 01.04.01.
     */
    @Test
    fun confirmReservation_winner_savesAndNotifies() = runBlocking {
        val model = ReservationBookingModel()
        val notifications = TestNotificationRepository()
        lotteryRepository.lottery = bookingLottery()
        lotteryEntryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        val bookingController = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = NotificationService(notifications),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )

        val reservation = bookingReservation()
        val result = bookingController.confirmReservation(campsite, reservation)

        assertEquals("testReservationId", result)
        assertEquals(1, repository.reservations.size)
        assertEquals(ReservationStatus.CONFIRMED, repository.reservations.single().status)
        assertEquals("entrant1", repository.reservations.single().entrantId)
        assertEquals("lottery1", repository.reservations.single().lotteryId)
        assertTrue(model.reservationConfirmed)
        assertNull(model.errorMessage)

        assertEquals(1, notifications.records.size)
        assertEquals("entrant1", notifications.records.single().recipientId)
        assertEquals("testReservationId", notifications.records.single().reservationId)
    }

    /**
     * Checks that a winner cannot confirm a reservation after declining.
     * US 01.04.01.
     */
    @Test
    fun confirmReservation_declinedWinner_doesNotSave() = runBlocking {
        val model = ReservationBookingModel()
        lotteryRepository.lottery = bookingLottery()
        lotteryEntryRepository.entry = LotteryEntry(
            status = LotteryEntry.STATUS_WON,
            opportunityStatus = LotteryEntry.OPPORTUNITY_DECLINED
        )

        val bookingController = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )

        val result = bookingController.confirmReservation(campsite, bookingReservation())

        assertNull(result)
        assertEquals(
            "You do not have an available winning opportunity.",
            model.errorMessage
        )
        assertTrue(repository.reservations.isEmpty())
        assertFalse(model.reservationConfirmed)
    }

    /**
     * Checks that the same lottery win cannot be used for another reservation.
     * US 01.04.01.
     */
    @Test
    fun confirmReservation_existingLotteryReservation_doesNotSave() = runBlocking {
        val model = ReservationBookingModel()
        lotteryRepository.lottery = bookingLottery()
        lotteryEntryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        repository.reservations = listOf(
            bookingReservation().copy(
                dates = DateRange("2027-06-01", "2027-06-02"),
                status = ReservationStatus.CONFIRMED
            )
        )

        val bookingController = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )

        val result = bookingController.confirmReservation(campsite, bookingReservation())

        assertNull(result)
        assertEquals(
            "You have already confirmed a reservation for this lottery.",
            model.errorMessage
        )
        assertEquals(1, repository.reservations.size)
        assertFalse(model.reservationConfirmed)
    }

    /**
     * Checks that a winning entrant cannot book after the lottery booking period.
     * US 01.04.01.
     */
    @Test
    fun confirmReservation_bookingPeriodClosed_doesNotSave() = runBlocking {
        val model = ReservationBookingModel()
        lotteryRepository.lottery = bookingLottery().copy(
            bookingStart = "2020-01-01",
            bookingEnd = "2020-01-10"
        )
        lotteryEntryRepository.entry = LotteryEntry(status = LotteryEntry.STATUS_WON)

        val bookingController = ReservationBookingController(
            bookingService = BookingService(),
            repository = repository,
            notificationService = notificationService(),
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = model
        )

        val result = bookingController.confirmReservation(campsite, bookingReservation())

        assertNull(result)
        assertEquals("The lottery booking period is not open.", model.errorMessage)
        assertTrue(repository.reservations.isEmpty())
        assertFalse(model.reservationConfirmed)
    }

    /**
     * Creates a lottery with a booking period that includes the test date.
     */
    private fun bookingLottery() = Lottery(
        id = "lottery1",
        campsiteIds = listOf("1"),
        bookingStart = "2020-01-01",
        bookingEnd = "2099-12-31",
        lotteryStart = "2027-06-01",
        lotteryEnd = "2027-06-10"
    )

    /**
     * Creates a valid reservation for lottery booking tests.
     */
    private fun bookingReservation() = Reservation(
        entrantId = "entrant1",
        campsiteId = "1",
        lotteryId = "lottery1",
        dates = DateRange("2027-06-03", "2027-06-05"),
        rulesAcknowledged = true
    )


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

    /**
     * Provides lottery information for controller tests without accessing Firestore.
     */
    private class TestLotteryRepository : LotteryRepository {

        var lottery: Lottery? = null

        override suspend fun getLotteries(): List<Lottery> =
            listOfNotNull(lottery)

        override suspend fun getLottery(id: String): Lottery? =
            lottery?.takeIf { it.id == id }

        override suspend fun create(lottery: Lottery): String =
            lottery.id

        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> =
            listOfNotNull(lottery)
    }

    /**
     * Provides lottery entry information for controller tests without accessing Firestore.
     */
    private class TestLotteryEntryRepository : LotteryEntryRepository {

        var entry: LotteryEntry? = null

        override suspend fun getEntry(
            lotteryId: String,
            entrantId: String
        ): LotteryEntry? = entry

        override suspend fun declineOpportunity(
            lotteryId: String,
            entrantId: String
        ): DeclineResult = DeclineResult.NOT_ELIGIBLE
    }
}

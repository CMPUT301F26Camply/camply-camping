package com.example.camply.user.controller

import com.example.camply.campsite.controller.CampsiteRemovalController
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRemovalModel
import com.example.camply.campsite.model.CampsiteRemovalRepository
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.repository.LotteryRepository
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.reservations.repository.ReservationCancellationResult
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.user.model.AdminUser
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.Profile
import com.example.camply.user.model.ProfileRemovalModel
import com.example.camply.user.repository.ProfileRemovalRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for an administrator removing a profile (US 03.02.01).
 *
 * Entrant removal is the same method an entrant will call to delete their own profile (US 01.08.01).
 */
class ProfileRemovalControllerTest {

    private lateinit var model: ProfileRemovalModel
    private lateinit var profileRepository: TestProfileRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var campsiteRemovalModel: CampsiteRemovalModel
    private lateinit var lotteryRepository: TestLotteryRepository
    private lateinit var reservationRepository: TestReservationRepository
    private lateinit var notificationRepository: TestNotificationRepository
    private lateinit var controller: ProfileRemovalController

    /**
     * Builds the controller with a fixed date so lottery and reservation comparisons do not use the clock.
     */
    @Before
    fun setUp() {
        model = ProfileRemovalModel()
        profileRepository = TestProfileRepository()
        campsiteRepository = TestCampsiteRepository()
        campsiteRemovalModel = CampsiteRemovalModel()
        lotteryRepository = TestLotteryRepository()
        reservationRepository = TestReservationRepository()
        notificationRepository = TestNotificationRepository()
        val campsiteRemoval = CampsiteRemovalController(
            campsiteRemovalModel,
            TestCampsiteRemovalRepository(),
            campsiteRepository,
            NotificationService(notificationRepository),
            today = { TODAY }
        )
        controller = ProfileRemovalController(
            model,
            profileRepository,
            campsiteRepository,
            campsiteRemoval,
            campsiteRemovalModel,
            lotteryRepository,
            reservationRepository,
            NotificationService(notificationRepository),
            today = { TODAY }
        )
    }

    /**
     * Checks that an admin profile is refused and the user document stays.
     */
    @Test
    fun removeProfile_admin_returnsFalse() = runBlocking {
        val removed = controller.removeProfile(admin())

        assertFalse(removed)
        assertEquals("This profile cannot be removed.", model.error)
        assertTrue(profileRepository.deletedUsers.isEmpty())
    }

    /**
     * Checks that a second removal is ignored while the first is still marked as running.
     */
    @Test
    fun removeProfile_alreadyRunning_returnsFalse() = runBlocking {
        model.start()

        val removed = controller.removeProfile(entrant())

        assertFalse(removed)
        assertTrue(profileRepository.deletedUsers.isEmpty())
    }

    /**
     * Checks that every campsite is removed before the owner document is deleted.
     */
    @Test
    fun removeProfile_owner_removesEveryCampsiteThenDeletesUser() = runBlocking {
        campsiteRepository.campsites = listOf(
            campsite("camp1"),
            campsite("camp2")
        )

        val removed = controller.removeProfile(owner())

        assertTrue(removed)
        assertEquals(listOf("camp1", "camp2"), campsiteRepository.deletedIds)
        assertEquals(listOf(OWNER_ID), profileRepository.deletedUsers)
        assertFalse(model.isRemoving)
        assertNull(model.error)
    }

    /**
     * Checks that a failed campsite removal stops the owner deletion.
     */
    @Test
    fun removeProfile_ownerCampsiteMissing_doesNotDeleteUser() = runBlocking {
        campsiteRepository.campsites = listOf(campsite("camp1"))
        campsiteRepository.missingOnDelete = setOf("camp1")

        val removed = controller.removeProfile(owner())

        assertFalse(removed)
        assertEquals("This campsite no longer exists.", model.error)
        assertTrue(profileRepository.deletedUsers.isEmpty())
    }

    /**
     * Checks that an entrant is removed from a current lottery and a future reservation, then deleted.
     */
    @Test
    fun removeProfile_entrant_removesCurrentRecordsThenDeletesUser() = runBlocking {
        lotteryRepository.joined = listOf(lottery("current", end = "2026-12-31"))
        reservationRepository.reservations = listOf(
            reservation("future", from = "2026-12-01", status = ReservationStatus.CONFIRMED)
        )
        campsiteRepository.campsites = listOf(campsite("camp1"))

        val removed = controller.removeProfile(entrant())

        assertTrue(removed)
        assertEquals(listOf("current" to ENTRANT_ID), profileRepository.deletedEntries)
        assertEquals(listOf("future"), profileRepository.deletedReservations)
        assertEquals(listOf(ENTRANT_ID), profileRepository.deletedUsers)
        assertEquals(
            listOf(NotificationType.ENTRANT_REMOVED_FROM_LOTTERY, NotificationType.RESERVATION_CANCELLED),
            notificationRepository.sent.map { it.type }
        )
        assertTrue(notificationRepository.sent.all { it.recipientId == OWNER_ID })
    }

    /**
     * Checks that a finished lottery and a reservation that has already started are left alone.
     */
    @Test
    fun removeProfile_entrantPastRecords_areLeftAlone() = runBlocking {
        lotteryRepository.joined = listOf(lottery("past", end = "2026-11-01"))
        reservationRepository.reservations = listOf(
            reservation("past", from = "2026-11-01", status = ReservationStatus.CONFIRMED),
            reservation("today", from = TODAY, status = ReservationStatus.CONFIRMED),
            reservation("cancelled", from = "2026-12-01", status = ReservationStatus.CANCELLED)
        )

        val removed = controller.removeProfile(entrant())

        assertTrue(removed)
        assertTrue(profileRepository.deletedEntries.isEmpty())
        assertTrue(profileRepository.deletedReservations.isEmpty())
        assertTrue(notificationRepository.sent.isEmpty())
        assertEquals(listOf(ENTRANT_ID), profileRepository.deletedUsers)
    }

    /**
     * Checks that removing an entrant directly does not delete the user document.
     *
     * US 01.08.01 calls this method and decides separately when the account itself is deleted.
     */
    @Test
    fun removeEntrant_doesNotDeleteUserDocument() = runBlocking {
        lotteryRepository.joined = listOf(lottery("current", end = ""))

        val removed = controller.removeEntrant(ENTRANT_ID)

        assertTrue(removed)
        assertEquals(listOf("current" to ENTRANT_ID), profileRepository.deletedEntries)
        assertTrue(profileRepository.deletedUsers.isEmpty())
    }

    /**
     * Checks that one failed notice still removes the entry and the user.
     */
    @Test
    fun removeProfile_notificationFails_stillDeletesEntrant() = runBlocking {
        lotteryRepository.joined = listOf(lottery("current", end = "2026-12-31"))
        notificationRepository.shouldFail = true

        val removed = controller.removeProfile(entrant())

        assertTrue(removed)
        assertEquals(listOf("current" to ENTRANT_ID), profileRepository.deletedEntries)
        assertEquals(listOf(ENTRANT_ID), profileRepository.deletedUsers)
    }
    /**
     * Checks the entrant deletion rules for lotteries and reservations together.
     *
     * A lottery stays when its end date is before today. A blank end, an end of today,
     * or a later end removes the entry. A reservation is removed only when it is
     * confirmed and its start date is after today.
     */
    @Test
    fun removeProfile_entrant_appliesLotteryAndReservationRules() = runBlocking {
        lotteryRepository.joined = listOf(
            lottery("ended", end = "2026-11-26"),
            lottery("endsToday", end = TODAY),
            lottery("open", end = "2026-12-31"),
            lottery("noEnd", end = "")
        )
        reservationRepository.reservations = listOf(
            reservation("started", from = "2026-11-01", status = ReservationStatus.CONFIRMED),
            reservation("startsToday", from = TODAY, status = ReservationStatus.CONFIRMED),
            reservation("future", from = "2026-12-01", status = ReservationStatus.CONFIRMED),
            reservation("cancelledFuture", from = "2026-12-02", status = ReservationStatus.CANCELLED),
            reservation("rejectedFuture", from = "2026-12-03", status = ReservationStatus.REJECTED)
        )
        campsiteRepository.campsites = listOf(campsite("camp1"))

        val removed = controller.removeProfile(entrant())

        assertTrue(removed)
        assertEquals(
            listOf("endsToday" to ENTRANT_ID, "open" to ENTRANT_ID, "noEnd" to ENTRANT_ID),
            profileRepository.deletedEntries
        )
        assertEquals(listOf("future"), profileRepository.deletedReservations)
        assertEquals(listOf(ENTRANT_ID), profileRepository.deletedUsers)
        assertEquals(
            listOf(
                NotificationType.ENTRANT_REMOVED_FROM_LOTTERY,
                NotificationType.ENTRANT_REMOVED_FROM_LOTTERY,
                NotificationType.ENTRANT_REMOVED_FROM_LOTTERY,
                NotificationType.RESERVATION_CANCELLED
            ),
            notificationRepository.sent.map { it.type }
        )
    }
    private fun owner() = CampsiteOwnerUser(OWNER_ID, Profile("Owner", "owner@example.com", "1"))

    private fun entrant() = EntrantUser(ENTRANT_ID, Profile("Entrant", "entrant@example.com", "2"))

    private fun admin() = AdminUser(ADMIN_ID, Profile("Admin", "admin@example.com", "3"))

    private fun campsite(id: String) = Campsite(id = id, name = "Lakeside", ownerId = OWNER_ID)

    private fun lottery(id: String, end: String) = Lottery(
        id = id,
        ownerId = OWNER_ID,
        lotteryEnd = end
    )

    private fun reservation(id: String, from: String, status: ReservationStatus) = Reservation(
        id = id,
        entrantId = ENTRANT_ID,
        campsiteId = "camp1",
        dates = DateRange(from = from, to = "2026-12-05"),
        status = status
    )

    /**
     * Records profile deletions without accessing Firestore.
     */
    private class TestProfileRepository : ProfileRemovalRepository {
        var deletedEntries: List<Pair<String, String>> = emptyList()
        var deletedReservations: List<String> = emptyList()
        var deletedUsers: List<String> = emptyList()

        override suspend fun deleteLotteryEntry(lotteryId: String, entrantId: String) {
            deletedEntries = deletedEntries + (lotteryId to entrantId)
        }

        override suspend fun deleteReservation(reservationId: String) {
            deletedReservations = deletedReservations + reservationId
        }

        override suspend fun deleteUser(userId: String) {
            deletedUsers = deletedUsers + userId
        }
    }

    /**
     * Records campsite reads and deletions for both profile removal and campsite removal.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()
        var deletedIds: List<String> = emptyList()
        var missingOnDelete: Set<String> = emptySet()

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? {
            if (id in missingOnDelete) return null
            return campsites.firstOrNull { it.id == id }
        }

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            campsites.filter { it.ownerId == ownerId }

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = campsite.id

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun deleteCampsite(id: String) {
            deletedIds = deletedIds + id
        }
    }

    /**
     * Campsite removal finds no lotteries or reservations of its own.
     */
    private class TestCampsiteRemovalRepository : CampsiteRemovalRepository {
        override suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery> = emptyList()
        override suspend fun getLotteryEntrantIds(lotteryId: String): List<String> = emptyList()
        override suspend fun removeCampsiteFromLottery(lotteryId: String, campsiteId: String) = Unit
        override suspend fun deleteLottery(lotteryId: String) = Unit
        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> = emptyList()
        override suspend fun deleteReservation(reservationId: String) = Unit
    }

    /**
     * Returns the lotteries the test arranged for one entrant.
     */
    private class TestLotteryRepository : LotteryRepository {
        var joined: List<Lottery> = emptyList()

        override suspend fun getLotteries(): List<Lottery> = joined
        override suspend fun getLottery(id: String): Lottery? = joined.firstOrNull { it.id == id }
        override suspend fun create(lottery: Lottery): String = lottery.id
        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> = joined
    }

    /**
     * Returns the reservations the test arranged for one entrant.
     */
    private class TestReservationRepository : ReservationRepository {
        var reservations: List<Reservation> = emptyList()

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
            reservations.filter { it.entrantId == entrantId }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun createReservation(reservation: Reservation): String = reservation.id

        override suspend fun cancelReservation(
            reservationId: String,
            entrantId: String,
            today: String
        ): ReservationCancellationResult = ReservationCancellationResult.NOT_ELIGIBLE

        override suspend fun rejectReservation(
            reservationId: String,
            ownerId: String,
            today: String
        ): Reservation = reservations.first { it.id == reservationId }
    }

    /**
     * Records the notifications profile removal asks to send.
     */
    private class TestNotificationRepository : NotificationRepository {
        var sent: List<NewNotification> = emptyList()
        var shouldFail = false

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            if (shouldFail) throw Exception("Could not send notification.")
            sent = sent + notification
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = {}

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = {}

        override suspend fun markAsRead(notificationId: String, recipientId: String) = Unit
    }

    private companion object {
        const val OWNER_ID = "owner1"
        const val ENTRANT_ID = "entrant1"
        const val ADMIN_ID = "admin1"
        const val TODAY = "2026-11-27"
    }
}

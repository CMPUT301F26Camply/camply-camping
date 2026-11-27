package com.example.camply.campsite.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRemovalModel
import com.example.camply.campsite.model.CampsiteRemovalRepository
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.model.Lottery
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for an administrator removing one campsite (US 03.01.01).
 */
class CampsiteRemovalControllerTest {

    private lateinit var model: CampsiteRemovalModel
    private lateinit var removalRepository: TestRemovalRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var notificationRepository: TestNotificationRepository
    private lateinit var controller: CampsiteRemovalController

    /**
     * Builds the controller with a fixed date so reservation comparisons do not depend on the clock.
     */
    @Before
    fun setUp() {
        model = CampsiteRemovalModel()
        removalRepository = TestRemovalRepository()
        campsiteRepository = TestCampsiteRepository()
        notificationRepository = TestNotificationRepository()
        controller = CampsiteRemovalController(
            model,
            removalRepository,
            campsiteRepository,
            NotificationService(notificationRepository),
            today = { TODAY }
        )
    }

    /**
     * Checks that a missing campsite is reported and nothing is deleted.
     */
    @Test
    fun removeCampsite_missingCampsite_returnsFalse() = runBlocking {
        val removed = controller.removeCampsite("missing")

        assertFalse(removed)
        assertEquals("This campsite no longer exists.", model.error)
        assertFalse(model.isRemoving)
        assertTrue(campsiteRepository.deletedIds.isEmpty())
    }

    /**
     * Checks that a second removal is ignored while the first is still marked as running.
     */
    @Test
    fun removeCampsite_alreadyRunning_returnsFalse() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        model.start()

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertFalse(removed)
        assertTrue(campsiteRepository.deletedIds.isEmpty())
    }

    /**
     * Checks that the only campsite in a lottery cancels that lottery and notifies each entrant once.
     */
    @Test
    fun removeCampsite_onlyCampsiteInLottery_cancelsLottery() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.lotteries = listOf(lottery("lottery1", listOf(CAMPSITE_ID)))
        removalRepository.entrants = mapOf("lottery1" to listOf("entrant1", "entrant1", "entrant2"))

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removed)
        assertEquals(listOf("lottery1"), removalRepository.deletedLotteries)
        assertTrue(removalRepository.removedFromLotteries.isEmpty())
        assertEquals(listOf(CAMPSITE_ID), campsiteRepository.deletedIds)
        assertEquals(listOf("entrant1", "entrant2"), notificationRepository.sent.map { it.recipientId })
        assertTrue(notificationRepository.sent.all { it.type == NotificationType.LOTTERY_CANCELLED })
        assertEquals(
            "The lottery has been canceled due to the campsite being taken down.",
            notificationRepository.sent.first().content
        )
        assertFalse(model.isRemoving)
        assertNull(model.error)
    }

    /**
     * Checks that a lottery with other campsites keeps those campsites and notifies its entrants.
     */
    @Test
    fun removeCampsite_lotteryHasOtherCampsites_removesOnlyThisCampsite() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.lotteries = listOf(lottery("lottery1", listOf(CAMPSITE_ID, "other")))
        removalRepository.entrants = mapOf("lottery1" to listOf("entrant1"))

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removed)
        assertEquals(listOf("lottery1" to CAMPSITE_ID), removalRepository.removedFromLotteries)
        assertTrue(removalRepository.deletedLotteries.isEmpty())
        assertEquals(NotificationType.CAMPSITE_REMOVED_FROM_LOTTERY, notificationRepository.sent.single().type)
        assertEquals("One of the campsites has been removed.", notificationRepository.sent.single().content)
    }

    /**
     * Checks that a confirmed reservation starting after today is cancelled and deleted.
     */
    @Test
    fun removeCampsite_futureReservation_cancelsAndDeletesIt() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.reservations = listOf(
            reservation("future", from = "2026-12-01", status = ReservationStatus.CONFIRMED)
        )

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removed)
        assertEquals(listOf("future"), removalRepository.deletedReservations)
        val notice = notificationRepository.sent.single()
        assertEquals(NotificationType.RESERVATION_CANCELLED, notice.type)
        assertEquals(
            "Your reservation at Lakeside from 2026-12-01 to 2026-12-05 has been canceled because the campsite was removed.",
            notice.content
        )
    }

    /**
     * Checks that a reservation starting today or earlier is left in place with no notice.
     */
    @Test
    fun removeCampsite_pastOrCurrentReservation_isLeftAlone() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.reservations = listOf(
            reservation("past", from = "2026-11-01", status = ReservationStatus.CONFIRMED),
            reservation("today", from = TODAY, status = ReservationStatus.CONFIRMED)
        )

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removed)
        assertTrue(removalRepository.deletedReservations.isEmpty())
        assertTrue(notificationRepository.sent.isEmpty())
        assertEquals(listOf(CAMPSITE_ID), campsiteRepository.deletedIds)
    }

    /**
     * Checks that a future reservation that is not confirmed is left in place.
     */
    @Test
    fun removeCampsite_unconfirmedFutureReservation_isLeftAlone() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.reservations = listOf(
            reservation("cancelled", from = "2026-12-01", status = ReservationStatus.CANCELLED)
        )

        controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removalRepository.deletedReservations.isEmpty())
        assertTrue(notificationRepository.sent.isEmpty())
    }

    /**
     * Checks that one failed notice does not stop the campsite from being deleted.
     */
    @Test
    fun removeCampsite_notificationFails_stillDeletesCampsite() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.lotteries = listOf(lottery("lottery1", listOf(CAMPSITE_ID)))
        removalRepository.entrants = mapOf("lottery1" to listOf("entrant1"))
        notificationRepository.shouldFail = true

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertTrue(removed)
        assertEquals(listOf("lottery1"), removalRepository.deletedLotteries)
        assertEquals(listOf(CAMPSITE_ID), campsiteRepository.deletedIds)
    }

    /**
     * Checks that a failed lookup is reported and the campsite document stays.
     */
    @Test
    fun removeCampsite_repositoryFails_returnsFalse() = runBlocking {
        campsiteRepository.campsites = listOf(campsite())
        removalRepository.shouldFail = true

        val removed = controller.removeCampsite(CAMPSITE_ID)

        assertFalse(removed)
        assertEquals("Could not read lotteries.", model.error)
        assertFalse(model.isRemoving)
        assertTrue(campsiteRepository.deletedIds.isEmpty())
    }

    private fun campsite() = Campsite(id = CAMPSITE_ID, name = "Lakeside", ownerId = OWNER_ID)

    private fun lottery(id: String, campsiteIds: List<String>) = Lottery(
        id = id,
        ownerId = OWNER_ID,
        campsiteIds = campsiteIds
    )

    private fun reservation(id: String, from: String, status: ReservationStatus) = Reservation(
        id = id,
        entrantId = "entrant1",
        campsiteId = CAMPSITE_ID,
        dates = DateRange(from = from, to = "2026-12-05"),
        status = status
    )

    /**
     * Stores lotteries and reservations in memory.
     */
    private class TestRemovalRepository : CampsiteRemovalRepository {
        var lotteries: List<Lottery> = emptyList()
        var entrants: Map<String, List<String>> = emptyMap()
        var reservations: List<Reservation> = emptyList()
        var deletedLotteries: List<String> = emptyList()
        var removedFromLotteries: List<Pair<String, String>> = emptyList()
        var deletedReservations: List<String> = emptyList()
        var shouldFail = false

        override suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery> {
            if (shouldFail) throw Exception("Could not read lotteries.")
            return lotteries.filter { campsiteId in it.campsiteIds }
        }

        override suspend fun getLotteryEntrantIds(lotteryId: String): List<String> =
            entrants[lotteryId].orEmpty()

        override suspend fun removeCampsiteFromLottery(lotteryId: String, campsiteId: String) {
            removedFromLotteries = removedFromLotteries + (lotteryId to campsiteId)
        }

        override suspend fun deleteLottery(lotteryId: String) {
            deletedLotteries = deletedLotteries + lotteryId
        }

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
            reservations.filter { it.campsiteId == campsiteId }

        override suspend fun deleteReservation(reservationId: String) {
            deletedReservations = deletedReservations + reservationId
        }
    }

    /**
     * Records campsite deletion without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()
        var deletedIds: List<String> = emptyList()

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? =
            campsites.firstOrNull { it.id == id }

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
     * Records the notifications the removal asks to send.
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
        const val CAMPSITE_ID = "camp1"
        const val TODAY = "2026-11-27"
    }
}
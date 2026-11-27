package com.example.camply.lottery.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.OwnerWaitlistLottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.lottery.repository.OwnerWaitlistRepository
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.notification.service.NotificationService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for the owner's waitlist screen and notifications to its entrants
 * (US 02.09.01, US 02.10.01).
 */
class OwnerWaitlistControllerTest {

    private lateinit var model: OwnerWaitlistModel
    private lateinit var waitlistRepository: TestOwnerWaitlistRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var nameRepository: TestRecipientNameRepository
    private lateinit var notificationRepository: TestNotificationRepository
    private lateinit var controller: OwnerWaitlistController

    /**
     * Creates the model, repositories, and controller before each test.
     */
    @Before
    fun setUp() {
        model = OwnerWaitlistModel()
        waitlistRepository = TestOwnerWaitlistRepository()
        campsiteRepository = TestCampsiteRepository()
        nameRepository = TestRecipientNameRepository()
        notificationRepository = TestNotificationRepository()
        controller = OwnerWaitlistController(
            model,
            waitlistRepository,
            campsiteRepository,
            nameRepository,
            NotificationService(notificationRepository)
        )
    }

    /**
     * Checks that the owner's lotteries are shown with their campsite names.
     */
    @Test
    fun loadLotteries_showsLotteriesWithCampsiteNames() = runBlocking {
        waitlistRepository.lotteries = listOf(lottery("lottery1", "campsite1"))
        campsiteRepository.campsites = listOf(Campsite(id = "campsite1", name = "Lakeside"))

        controller.loadLotteries(OWNER_ID)

        assertEquals(OWNER_ID, waitlistRepository.ownerId)
        assertEquals(listOf("Lakeside"), model.lotteries.single().campsiteNames)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that a lottery with several campsites shows every campsite name.
     */
    @Test
    fun loadLotteries_multipleCampsites_showsAllNames() = runBlocking {
        waitlistRepository.lotteries = listOf(
            lottery("lottery1", listOf("campsite1", "campsite2"))
        )
        campsiteRepository.campsites = listOf(
            Campsite(id = "campsite1", name = "Lakeside"),
            Campsite(id = "campsite2", name = "Banff Pines")
        )

        controller.loadLotteries(OWNER_ID)

        assertEquals(listOf("Lakeside", "Banff Pines"), model.lotteries.single().campsiteNames)
    }

    /**
     * Checks that a missing or unreadable campsite only hides that lottery's campsite name.
     */
    @Test
    fun loadLotteries_missingCampsite_keepsLotteryWithoutName() = runBlocking {
        waitlistRepository.lotteries = listOf(lottery("lottery1", "deleted"))
        campsiteRepository.shouldFail = true

        controller.loadLotteries(OWNER_ID)

        assertEquals("lottery1", model.lotteries.single().lottery.id)
        assertTrue(model.lotteries.single().campsiteNames.isEmpty())
        assertNull(model.errorMessage)
    }

    /**
     * Checks that one unreadable campsite does not hide the other campsite names.
     */
    @Test
    fun loadLotteries_oneCampsiteFails_keepsOtherNames() = runBlocking {
        waitlistRepository.lotteries = listOf(
            lottery("lottery1", listOf("broken", "campsite2"))
        )
        campsiteRepository.campsites = listOf(Campsite(id = "campsite2", name = "Banff Pines"))
        campsiteRepository.failingIds = setOf("broken")

        controller.loadLotteries(OWNER_ID)

        assertEquals(listOf("Banff Pines"), model.lotteries.single().campsiteNames)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that lotteries are shown with the newest registration period first.
     */
    @Test
    fun loadLotteries_sortsNewestRegistrationFirst() = runBlocking {
        waitlistRepository.lotteries = listOf(
            lottery("old", "campsite1", "2026-05-01"),
            lottery("new", "campsite1", "2026-11-01")
        )

        controller.loadLotteries(OWNER_ID)

        assertEquals(listOf("new", "old"), model.lotteries.map { it.lottery.id })
    }

    /**
     * Checks that an error is shown when the owner's lotteries cannot be loaded.
     */
    @Test
    fun loadLotteries_readFails_showsError() = runBlocking {
        waitlistRepository.shouldFail = true

        controller.loadLotteries(OWNER_ID)

        assertEquals("Could not load your lotteries.", model.errorMessage)
        assertFalse(model.isLoading)
    }

    /**
     * Checks that selecting a lottery shows its entrants with their names.
     */
    @Test
    fun onLotterySelected_showsEntrantsWithNames() = runBlocking {
        waitlistRepository.entrants = listOf(entrant("entrant1"), entrant("entrant2"))
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam")

        controller.onLotterySelected(item("lottery1"))

        assertEquals("lottery1", waitlistRepository.lotteryId)
        assertEquals(listOf("Alex", "Sam"), model.entrants.map { it.name })
        assertFalse(model.isLoadingEntrants)
    }

    /**
     * Checks that a failed name lookup keeps the entrant in the list without a name.
     */
    @Test
    fun onLotterySelected_nameLookupFails_keepsEntrantWithoutName() = runBlocking {
        waitlistRepository.entrants = listOf(entrant("entrant1"), entrant("entrant2"))
        nameRepository.names = mapOf("entrant2" to "Sam")
        nameRepository.failingIds = setOf("entrant1")

        controller.onLotterySelected(item("lottery1"))

        assertEquals(listOf(null, "Sam"), model.entrants.map { it.name })
        assertNull(model.entrantsError)
    }

    /**
     * Checks that an error is shown when the lottery's entrants cannot be loaded.
     */
    @Test
    fun onLotterySelected_readFails_showsError() = runBlocking {
        waitlistRepository.shouldFail = true

        controller.onLotterySelected(item("lottery1"))

        assertEquals("lottery1", model.selectedLottery?.lottery?.id)
        assertEquals("Could not load the waitlist.", model.entrantsError)
        assertFalse(model.isLoadingEntrants)
    }

    /**
     * Checks that entrants loaded for a previous lottery are not shown for the new one.
     */
    @Test
    fun onLotterySelected_otherLotterySelectedWhileLoading_ignoresOldEntrants() = runBlocking {
        waitlistRepository.entrants = listOf(entrant("entrant1"))
        // Simulates the owner choosing another lottery before the first read finishes.
        waitlistRepository.onRead = { model.selectLottery(item("lottery2")) }

        controller.onLotterySelected(item("lottery1"))

        assertEquals("lottery2", model.selectedLottery?.lottery?.id)
        assertTrue(model.entrants.isEmpty())
        assertTrue(model.isLoadingEntrants)
    }

    /**
     * Checks that going back clears the selected lottery.
     */
    @Test
    fun onBackToLotteries_clearsSelection() = runBlocking {
        waitlistRepository.entrants = listOf(entrant("entrant1"))
        controller.onLotterySelected(item("lottery1"))

        controller.onBackToLotteries()

        assertNull(model.selectedLottery)
        assertTrue(model.entrants.isEmpty())
    }

    /**
     * Checks that every entrant on the waitlist gets the owner's message.
     */
    @Test
    fun onSendNotification_sendsToEveryEntrant() = runBlocking {
        openWaitlist(entrant("entrant1"), entrant("entrant2"))
        controller.onStartMessage()
        controller.onMessageChanged("  Quiet hours start at 10 PM.  ")

        controller.onSendNotification(OWNER_ID)

        val sent = notificationRepository.records
        assertEquals(listOf("entrant1", "entrant2"), sent.map { it.recipientId })
        assertEquals(1, sent.map { it.eventId }.distinct().size)
        sent.forEach {
            assertEquals(OWNER_ID, it.senderId)
            assertEquals(NotificationOrigin.OWNER, it.origin)
            assertEquals(NotificationType.OWNER_MESSAGE, it.type)
            assertEquals("lottery1", it.lotteryId)
            assertEquals("Lakeside waitlist update", it.title)
            assertEquals("Quiet hours start at 10 PM.", it.content)
        }
        assertEquals(2, model.sentCount)
        assertNull(model.sendError)
    }

    /**
     * Checks that an entrant listed twice only gets the message once.
     */
    @Test
    fun onSendNotification_repeatedEntrant_sendsOnce() = runBlocking {
        openWaitlist(entrant("entrant1"), entrant("entrant1"))
        controller.onMessageChanged("Quiet hours start at 10 PM.")

        controller.onSendNotification(OWNER_ID)

        assertEquals(1, notificationRepository.records.size)
        assertEquals(1, model.sentCount)
    }

    /**
     * Checks that one failed entrant does not stop the others and shows how many failed.
     */
    @Test
    fun onSendNotification_oneEntrantFails_sendsToOthersAndShowsError() = runBlocking {
        openWaitlist(entrant("entrant1"), entrant("entrant2"), entrant("entrant3"))
        controller.onMessageChanged("Quiet hours start at 10 PM.")
        notificationRepository.failingRecipients = setOf("entrant2")

        controller.onSendNotification(OWNER_ID)

        assertEquals(listOf("entrant1", "entrant3"), notificationRepository.records.map { it.recipientId })
        assertEquals("Could not send to 1 of 3 entrants. Try again.", model.sendError)
        assertNull(model.sentCount)
        assertFalse(model.isSending)
    }

    /**
     * Checks that trying again reuses the event ID, so entrants who got it are not sent it twice.
     */
    @Test
    fun onSendNotification_retry_reusesEventId() = runBlocking {
        openWaitlist(entrant("entrant1"), entrant("entrant2"))
        controller.onMessageChanged("Quiet hours start at 10 PM.")
        notificationRepository.failingRecipients = setOf("entrant2")
        controller.onSendNotification(OWNER_ID)
        val firstEventId = notificationRepository.records.single().eventId

        notificationRepository.failingRecipients = emptySet()
        controller.onSendNotification(OWNER_ID)

        assertEquals(setOf(firstEventId), notificationRepository.attempts.map { it.eventId }.toSet())
        assertEquals(listOf("entrant1", "entrant2"), notificationRepository.records.map { it.recipientId })
        assertEquals(2, model.sentCount)
    }

    /**
     * Checks that an edited message is sent with a new event ID.
     */
    @Test
    fun onSendNotification_editedMessage_usesNewEventId() = runBlocking {
        openWaitlist(entrant("entrant1"))
        controller.onMessageChanged("Quiet hours start at 10 PM.")
        notificationRepository.failingRecipients = setOf("entrant1")
        controller.onSendNotification(OWNER_ID)

        notificationRepository.failingRecipients = emptySet()
        controller.onMessageChanged("Quiet hours start at 11 PM.")
        controller.onSendNotification(OWNER_ID)

        assertEquals(2, notificationRepository.attempts.map { it.eventId }.distinct().size)
        assertEquals("Quiet hours start at 11 PM.", notificationRepository.records.single().content)
    }

    /**
     * Checks that nothing is sent when the message is blank or the waitlist is empty.
     */
    @Test
    fun onSendNotification_blankMessageOrNoEntrants_sendsNothing() = runBlocking {
        openWaitlist(entrant("entrant1"))
        controller.onMessageChanged("   ")
        controller.onSendNotification(OWNER_ID)

        openWaitlist()
        controller.onMessageChanged("Quiet hours start at 10 PM.")
        controller.onSendNotification(OWNER_ID)

        assertTrue(notificationRepository.attempts.isEmpty())
        assertNull(model.sentCount)
    }

    // Opens the waitlist of "lottery1" with the given entrants.
    private suspend fun openWaitlist(vararg entrants: WaitlistEntrant) {
        waitlistRepository.entrants = entrants.toList()
        controller.onLotterySelected(item("lottery1"))
    }

    private fun lottery(id: String, campsiteId: String, registrationStart: String = "2026-11-01") =
        lottery(id, listOf(campsiteId), registrationStart)

    private fun lottery(
        id: String,
        campsiteIds: List<String>,
        registrationStart: String = "2026-11-01"
    ) = Lottery(
        id = id,
        ownerId = OWNER_ID,
        campsiteIds = campsiteIds,
        registrationStart = registrationStart
    )

    private fun item(id: String) =
        OwnerWaitlistLottery(lottery(id, "campsite1"), listOf("Lakeside"))

    private fun entrant(id: String) = WaitlistEntrant(
        entrantId = id,
        name = null,
        status = "waiting",
        joinedAt = Date(1000)
    )

    /**
     * Provides lotteries and entrants without accessing Firestore.
     */
    private class TestOwnerWaitlistRepository : OwnerWaitlistRepository {
        var lotteries: List<Lottery> = emptyList()
        var entrants: List<WaitlistEntrant> = emptyList()
        var shouldFail = false
        var onRead: () -> Unit = {}
        var ownerId: String? = null
        var lotteryId: String? = null

        override suspend fun getOwnerLotteries(ownerId: String): List<Lottery> {
            this.ownerId = ownerId
            if (shouldFail) throw Exception("Could not read lotteries.")
            return lotteries
        }

        override suspend fun getEntrants(lotteryId: String): List<WaitlistEntrant> {
            this.lotteryId = lotteryId
            onRead()
            if (shouldFail) throw Exception("Could not read entrants.")
            return entrants
        }
    }

    /**
     * Records saved notifications without accessing Firestore, and fails for chosen recipients.
     */
    private class TestNotificationRepository : NotificationRepository {
        val attempts = mutableListOf<NewNotification>()
        val records = mutableListOf<NewNotification>()
        private val savedIds = mutableSetOf<String>()
        var failingRecipients: Set<String> = emptySet()

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            attempts.add(notification)
            if (notification.recipientId in failingRecipients) throw Exception("Could not save.")
            if (!savedIds.add(id)) return SendResult.ALREADY_EXISTS
            records.add(notification)
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by waitlist tests")

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by waitlist tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by waitlist tests")
    }

    /**
     * Provides entrant names without accessing Firestore.
     */
    private class TestRecipientNameRepository : RecipientNameRepository {
        var names: Map<String, String> = emptyMap()
        var failingIds: Set<String> = emptySet()

        override suspend fun getRecipientName(userId: String): String? {
            if (userId in failingIds) throw Exception("Could not read name.")
            return names[userId]
        }
    }

    /**
     * Provides campsites without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()
        var shouldFail = false
        var failingIds: Set<String> = emptySet()

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? {
            if (shouldFail || id in failingIds) throw Exception("Could not read campsite.")
            return campsites.firstOrNull { it.id == id }
        }

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            campsites.filter { it.ownerId == ownerId }

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = campsite.id

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? =
            null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? =
            null

        override suspend fun deleteCampsite(id: String) = Unit
    }

    private companion object {
        const val OWNER_ID = "owner1"
    }
}

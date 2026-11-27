package com.example.camply.lottery.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.controller.OwnerWaitlistController
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.lottery.repository.OwnerWaitlistRepository
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.notification.service.NotificationService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * Compose UI tests for the owner's waitlist screen and notifications to its entrants
 * (US 02.09.01, US 02.10.01).
 */
class OwnerWaitlistViewTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var waitlistRepository: TestOwnerWaitlistRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var nameRepository: TestRecipientNameRepository
    private lateinit var notificationRepository: TestNotificationRepository

    private val lottery = Lottery(
        id = "lottery1",
        ownerId = OWNER_ID,
        campsiteIds = listOf(CAMPSITE_ID),
        registrationStart = "2026-11-01",
        registrationEnd = "2026-11-20",
        waitlistCount = 3
    )

    /**
     * Creates the fake repositories before each test.
     */
    @Before
    fun setUp() {
        waitlistRepository = TestOwnerWaitlistRepository()
        campsiteRepository = TestCampsiteRepository()
        nameRepository = TestRecipientNameRepository()
        notificationRepository = TestNotificationRepository()
        campsiteRepository.campsites = listOf(Campsite(id = CAMPSITE_ID, name = "Banff Pines"))
    }

    /**
     * Checks that each lottery shows its campsite, registration period, and number of entrants.
     */
    @Test
    fun display_lotteries_showsCampsiteAndRegistrationPeriod() {
        waitlistRepository.lotteries = listOf(lottery)

        showScreen()

        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText("Registration: 2026-11-01 to 2026-11-20").assertIsDisplayed()
        compose.onNodeWithText("3 joined").assertIsDisplayed()
    }

    /**
     * Checks that a lottery with several campsites shows every campsite name.
     */
    @Test
    fun display_multipleCampsites_showsAllCampsiteNames() {
        campsiteRepository.campsites = listOf(
            Campsite(id = CAMPSITE_ID, name = "Banff Pines"),
            Campsite(id = "campsite2", name = "Lakeside")
        )
        waitlistRepository.lotteries =
            listOf(lottery.copy(campsiteIds = listOf(CAMPSITE_ID, "campsite2")))

        showScreen()

        compose.onNodeWithText("Banff Pines, Lakeside").assertIsDisplayed()
    }

    /**
     * Checks that an owner without lotteries sees an empty message.
     */
    @Test
    fun display_noLotteries_showsEmptyMessage() {
        showScreen()

        compose.onNodeWithText("You have no lotteries yet.").assertIsDisplayed()
    }

    /**
     * Checks that a failed load shows an error, and trying again shows the lotteries.
     */
    @Test
    fun display_loadFails_tryAgainShowsLotteries() {
        waitlistRepository.lotteries = listOf(lottery)
        waitlistRepository.shouldFail = true

        showScreen()

        compose.onNodeWithText("Could not load your lotteries.").assertIsDisplayed()

        waitlistRepository.shouldFail = false
        compose.onNodeWithText("Try again").performClick()

        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
    }

    /**
     * Checks that every entrant who joined is shown, whether they are waiting, won, or lost.
     */
    @Test
    fun selectLottery_showsEntrantNamesAndStatuses() {
        waitlistRepository.lotteries = listOf(lottery)
        waitlistRepository.entrants = listOf(
            entrant("entrant1", "waiting", joinedOn(2026, Calendar.NOVEMBER, 25)),
            entrant("entrant2", "won", joinedOn(2026, Calendar.NOVEMBER, 26)),
            entrant("entrant3", "lost", joinedOn(2026, Calendar.NOVEMBER, 27))
        )
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam", "entrant3" to "Jo")

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("ENTRANTS (3)").assertIsDisplayed()
        compose.onNodeWithText("Alex").assertIsDisplayed()
        compose.onNodeWithText("Waiting").assertIsDisplayed()
        compose.onNodeWithText("Joined Nov 25, 2026").assertIsDisplayed()
        compose.onNodeWithText("Sam").assertIsDisplayed()
        compose.onNodeWithText("Won").assertIsDisplayed()
        compose.onNodeWithText("Jo").assertIsDisplayed()
        compose.onNodeWithText("Lost").assertIsDisplayed()
    }

    /**
     * Checks that a lottery without entrants shows an empty message.
     */
    @Test
    fun selectLottery_noEntrants_showsEmptyMessage() {
        waitlistRepository.lotteries = listOf(lottery)

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("No entrants have joined this waitlist yet.").assertIsDisplayed()
    }

    /**
     * Checks that an entrant without a name is still shown.
     */
    @Test
    fun selectLottery_missingName_showsUnknownEntrant() {
        waitlistRepository.lotteries = listOf(lottery)
        waitlistRepository.entrants = listOf(entrant("deleted", "waiting", null))

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("Unknown entrant").assertIsDisplayed()
        compose.onNodeWithText("Waiting").assertIsDisplayed()
    }

    /**
     * Checks that the back button returns to the owner's lottery list.
     */
    @Test
    fun backButton_returnsToLotteryList() {
        waitlistRepository.lotteries = listOf(lottery)

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
        compose.onNodeWithText("Waitlist").assertIsDisplayed()

        compose.onNodeWithText("Back to lotteries").performClick()

        compose.onNodeWithText("Lottery Waitlists").assertIsDisplayed()
        compose.onNodeWithText("3 joined").assertIsDisplayed()
    }

    /**
     * Checks that an empty waitlist has no button to send a notification.
     */
    @Test
    fun emptyWaitlist_hidesSendNotification() {
        waitlistRepository.lotteries = listOf(lottery)

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()

        compose.onNodeWithText("No entrants have joined this waitlist yet.").assertIsDisplayed()
        compose.onNodeWithText("Send notification").assertDoesNotExist()
    }

    /**
     * Checks that sending a message reaches every entrant and shows how many received it.
     */
    @Test
    fun sendNotification_message_sendsToEveryEntrant() {
        openWaitlistWithTwoEntrants()

        compose.onNodeWithText("Send notification").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(MESSAGE)
        compose.onNodeWithText("Send").performClick()

        compose.onNodeWithText("Sent to 2 entrants.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("entrant1", "entrant2"), notificationRepository.records.map { it.recipientId })
            assertEquals(MESSAGE, notificationRepository.records.first().content)
        }
    }

    /**
     * Checks that a blank message cannot be sent.
     */
    @Test
    fun sendNotification_blankMessage_sendIsDisabled() {
        openWaitlistWithTwoEntrants()

        compose.onNodeWithText("Send notification").performClick()

        compose.onNodeWithText("Send").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput(MESSAGE)
        compose.onNodeWithText("Send").assertIsEnabled()
    }

    /**
     * Checks that cancelling closes the message box without sending anything.
     */
    @Test
    fun sendNotification_cancel_sendsNothing() {
        openWaitlistWithTwoEntrants()

        compose.onNodeWithText("Send notification").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(MESSAGE)
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Send notification").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.runOnIdle { assertTrue(notificationRepository.records.isEmpty()) }
    }

    /**
     * Checks that a failed send shows an error, and sending again reaches the missed entrant.
     */
    @Test
    fun sendNotification_partialFailure_showsErrorAndRetries() {
        openWaitlistWithTwoEntrants()
        notificationRepository.failingRecipients = setOf("entrant2")

        compose.onNodeWithText("Send notification").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(MESSAGE)
        compose.onNodeWithText("Send").performClick()

        compose.onNodeWithText("Could not send to 1 of 2 entrants. Try again.").assertIsDisplayed()

        compose.runOnIdle { notificationRepository.failingRecipients = emptySet() }
        compose.onNodeWithText("Send").performClick()

        compose.onNodeWithText("Sent to 2 entrants.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("entrant1", "entrant2"), notificationRepository.records.map { it.recipientId })
        }
    }

    private fun openWaitlistWithTwoEntrants() {
        waitlistRepository.lotteries = listOf(lottery)
        waitlistRepository.entrants = listOf(
            entrant("entrant1", "waiting", joinedOn(2026, Calendar.NOVEMBER, 25)),
            entrant("entrant2", "waiting", joinedOn(2026, Calendar.NOVEMBER, 26))
        )
        nameRepository.names = mapOf("entrant1" to "Alex", "entrant2" to "Sam")

        showScreen()
        compose.onNodeWithText("Banff Pines").performClick()
    }

    private fun showScreen() {
        val model = OwnerWaitlistModel()
        val controller = OwnerWaitlistController(
            model,
            waitlistRepository,
            campsiteRepository,
            nameRepository,
            NotificationService(notificationRepository)
        )
        val view = OwnerWaitlistView(model, controller)

        compose.setContent { view.Display(OWNER_ID) }
    }

    private fun entrant(id: String, status: String, joinedAt: Date?) = WaitlistEntrant(
        entrantId = id,
        name = null,
        status = status,
        joinedAt = joinedAt
    )

    // Uses the device's time zone so the displayed date matches.
    private fun joinedOn(year: Int, month: Int, day: Int): Date =
        GregorianCalendar(year, month, day, 12, 0).time

    /**
     * Provides lotteries and entrants without accessing Firestore.
     */
    private class TestOwnerWaitlistRepository : OwnerWaitlistRepository {
        var lotteries: List<Lottery> = emptyList()
        var entrants: List<WaitlistEntrant> = emptyList()
        var shouldFail = false

        override suspend fun getOwnerLotteries(ownerId: String): List<Lottery> {
            if (shouldFail) throw Exception("Could not read lotteries.")
            return lotteries
        }

        override suspend fun getEntrants(lotteryId: String): List<WaitlistEntrant> = entrants
    }

    /**
     * Records notifications without accessing Firestore, and fails for chosen recipients.
     */
    private class TestNotificationRepository : NotificationRepository {
        val records = mutableListOf<NewNotification>()
        private val savedIds = mutableSetOf<String>()
        var failingRecipients: Set<String> = emptySet()

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            if (notification.recipientId in failingRecipients) throw Exception("Could not save.")
            if (!savedIds.add(id)) return SendResult.ALREADY_EXISTS
            records.add(notification)
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by waitlist view tests")

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by waitlist view tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by waitlist view tests")
    }

    /**
     * Provides entrant names without accessing Firestore.
     */
    private class TestRecipientNameRepository : RecipientNameRepository {
        var names: Map<String, String> = emptyMap()

        override suspend fun getRecipientName(userId: String): String? = names[userId]
    }

    /**
     * Provides campsites without accessing Firestore.
     */
    private class TestCampsiteRepository : CampsiteRepository {
        var campsites: List<Campsite> = emptyList()

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

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? =
            null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? =
            null

        override suspend fun deleteCampsite(id: String) = Unit
    }

    private companion object {
        const val OWNER_ID = "owner1"
        const val CAMPSITE_ID = "campsite1"
        const val MESSAGE = "Quiet hours start at 10 PM."
    }
}

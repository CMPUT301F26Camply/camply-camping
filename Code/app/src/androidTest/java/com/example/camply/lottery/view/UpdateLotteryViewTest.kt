package com.example.camply.lottery.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.controller.UpdateLotteryController
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.lottery.model.UpdateLotteryModel
import com.example.camply.lottery.repository.LotteryRepository
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UpdateLotteryViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var lotteryRepo: MockLotteryRepository
    private lateinit var model: UpdateLotteryModel
    private lateinit var controller: UpdateLotteryController

    private val lottery = Lottery(
        id = "lot1",
        campsiteIds = listOf("camp1"),
        registrationStart = "2026-11-20",
        registrationEnd = "2026-11-23",
        bookingStart = "2026-11-24",
        bookingEnd = "2026-11-25",
        lotteryStart = "2026-12-01",
        lotteryEnd = "2026-12-05",
        numberOfWinners = 3,
        status = LotteryStatus.OPEN
    )

    @Before
    fun setUp() {
        lotteryRepo = MockLotteryRepository()
        model = UpdateLotteryModel()
        controller = UpdateLotteryController(model, lotteryRepo, MockCampsiteRepository())
    }

    private fun show(onAdd: () -> Unit = {}, onBack: () -> Unit = {}) {
        val view = UpdateLotteryView(model, controller)
        composeRule.setContent {
            view.Display(ownerId = "owner1", onAddLottery = onAdd, onBack = onBack)
        }
        composeRule.waitForIdle()
    }

    private fun openEditForm(target: Lottery = lottery) {
        lotteryRepo.lotteries = listOf(target)
        show()
        composeRule.onNodeWithContentDescription("Edit lottery for 1 campsite(s)").performClick()
    }

    // Checks the list screen shows its title and subtitle.
    @Test
    fun listScreen_showsHeader() {
        show()
        composeRule.onNodeWithText("My Lotteries").assertIsDisplayed()
        composeRule.onNodeWithText("Open lotteries you can edit").assertIsDisplayed()
    }

    // Checks the empty message appears when the owner has no lotteries.
    @Test
    fun listScreen_showsEmptyMessage() {
        show()
        composeRule
            .onNodeWithText("No open lotteries. Press the '+' button to create one.")
            .assertIsDisplayed()
    }

    // Checks a loaded lottery card shows all its dates and winners.
    @Test
    fun listScreen_showsLotteryCardDetails() {
        lotteryRepo.lotteries = listOf(lottery)
        show()
        composeRule.onNodeWithText("Registration: 2026-11-20 to 2026-11-23").assertIsDisplayed()
        composeRule.onNodeWithText("Booking: 2026-11-24 to 2026-11-25").assertIsDisplayed()
        composeRule.onNodeWithText("Stay: 2026-12-01 to 2026-12-05").assertIsDisplayed()
        composeRule.onNodeWithText("Winners: 3").assertIsDisplayed()
    }

    // Checks the "+" and Back buttons run their callbacks.
    @Test
    fun listScreen_addAndBackButtonsTriggerCallbacks() {
        var added = false
        var back = false
        show(onAdd = { added = true }, onBack = { back = true })

        composeRule.onNodeWithContentDescription("Create lottery").performClick()
        composeRule.onNodeWithText("Back").performClick()

        assertTrue(added)
        assertTrue(back)
    }

    // Checks the edit button opens a form pre-filled with the lottery's dates.
    @Test
    fun editForm_isPrefilledWithLotteryDates() {
        openEditForm()
        composeRule.onNodeWithText("Update Lottery").assertIsDisplayed()
        composeRule.onNodeWithText("2026-11-20").assertIsDisplayed()
        composeRule.onNodeWithText("2026-11-23").assertIsDisplayed()
        composeRule.onNodeWithText("2026-12-01").assertIsDisplayed()
        composeRule.onNodeWithText("2026-12-05").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsEnabled()
    }

    // Checks Next is disabled when a required date is blank.
    @Test
    fun editForm_nextDisabledWhenDateBlank() {
        openEditForm(lottery.copy(registrationStart = ""))
        composeRule.onNodeWithText("Next").assertIsNotEnabled()
    }

    // Checks Next opens page two and Back returns to page one.
    @Test
    fun editForm_nextShowsPageTwoAndBackReturns() {
        openEditForm()

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Booking Range").assertIsDisplayed()
        composeRule.onNodeWithText("2026-11-24").assertIsDisplayed()
        composeRule.onNodeWithText("Save Changes").assertIsEnabled()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Registration Range").assertIsDisplayed()
    }

    // Checks the winners field strips out non-digit characters.
    @Test
    fun editForm_winnerFieldFiltersNonDigits() {
        openEditForm()

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Number of lottery winners").performTextReplacement("5a")
        composeRule.onNodeWithText("5").assertIsDisplayed()
    }

    // Checks saving zero winners shows the controller's validation error.
    @Test
    fun editForm_zeroWinnersShowsError() {
        openEditForm()

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Number of lottery winners").performTextReplacement("0")
        composeRule.onNodeWithText("Save Changes").performClick()
        composeRule.onNodeWithText("Winner count must be at least 1.").assertIsDisplayed()
    }
}

/** In-memory lottery repository: no Firebase, no network. */
/**
 * Supplies lotteries without accessing Firebase.
 */
private class MockLotteryRepository : LotteryRepository {
    var lotteries: List<Lottery> = emptyList()
    var error: Exception? = null
    var hang = false

    override suspend fun getLotteries(): List<Lottery> = lotteries

    override suspend fun getLottery(id: String): Lottery? =
        lotteries.find { it.id == id }

    override suspend fun create(lottery: Lottery): String {
        val id = "lot${lotteries.size + 1}"
        lotteries = lotteries + lottery.copy(id = id)
        return id
    }

    override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> = emptyList()

    override suspend fun getLotteriesByOwner(ownerId: String): List<Lottery> {
        if (hang) awaitCancellation()
        error?.let { throw it }
        return lotteries
    }

    override suspend fun updateLottery(lottery: Lottery) {
        lotteries = lotteries.map { if (it.id == lottery.id) lottery else it }
    }
}

/**
 * Supplies the owner's campsites without accessing Firebase.
 */
private class MockCampsiteRepository : CampsiteRepository {
    val owned = mutableListOf<Campsite>()

    override suspend fun getCampsites(): List<Campsite> = owned

    override suspend fun getCampsite(id: String): Campsite? = owned.find { it.id == id }

    override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
        owned.filter { it.ownerId == ownerId }

    override suspend fun saveCampsite(
        campsite: Campsite,
        photosToSave: Map<String, PreparedCampsiteImages>,
        photosToRemove: List<String>
    ): String = campsite.id

    override suspend fun deleteCampsite(id: String) {}

    override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

    override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null
}
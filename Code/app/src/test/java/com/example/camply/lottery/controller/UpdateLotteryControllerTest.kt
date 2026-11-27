package com.example.camply.lottery.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.UpdateLotteryModel
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.lottery.repository.LotteryRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
    /**
     * Unit tests for loading and editing an owner's open lotteries.
     */
class UpdateLotteryControllerTest {

    private lateinit var model: UpdateLotteryModel
    private lateinit var lotteries: MockLotteryRepository
    private lateinit var campsites: MockCampsiteRepository
    private lateinit var controller: UpdateLotteryController

    @Before
    fun setUp() {
        model = UpdateLotteryModel()
        lotteries = MockLotteryRepository()
        campsites = MockCampsiteRepository()
        campsites.owned += Campsite(id = "site-1", name = "Pine Ridge", ownerId = "owner-1")
        controller = UpdateLotteryController(model, lotteries, campsites)
    }

    private fun lottery(
        id: String = "lottery-1",
        ownerId: String = "owner-1",
        campsiteIds: List<String> = listOf("site-1"),
        registrationStart: String = "2027-05-01",
        registrationEnd: String = "2027-05-10",
        status: LotteryStatus = LotteryStatus.OPEN
    ) = Lottery(
        id = id,
        ownerId = ownerId,
        campsiteIds = campsiteIds,
        registrationStart = registrationStart,
        registrationEnd = registrationEnd,
        bookingStart = "2027-05-15",
        bookingEnd = "2027-05-20",
        numberOfWinners = 2,
        lotteryStart = "2027-06-01",
        lotteryEnd = "2027-06-10",
        status = status
    )

    private suspend fun update(
        existing: Lottery,
        registrationStart: String = "2027-05-01",
        registrationEnd: String = "2027-05-10",
        bookingStart: String = "2027-05-15",
        bookingEnd: String = "2027-05-20",
        lotteryStart: String = "2027-06-01",
        lotteryEnd: String = "2027-06-10",
        winnerCount: String = "2"
    ): Boolean = controller.updateLottery(
        existing, registrationStart, registrationEnd, bookingStart,
        bookingEnd, lotteryStart, lotteryEnd, winnerCount
    )

    // Only this owner's open lotteries are shown, earliest registration first
    @Test
    fun loadLotteries_showsOnlyOwnersOpenLotteriesInOrder() = runBlocking {
        lotteries.stored += lottery(
            id = "late",
            registrationStart = "2027-08-01",
            registrationEnd = "2027-08-10"
        )
        lotteries.stored += lottery(
            id = "early",
            registrationStart = "2027-03-01",
            registrationEnd = "2027-03-10"
        )
        lotteries.stored += lottery(id = "other-owner", ownerId = "owner-2")

        controller.loadLotteries("owner-1")

        assertEquals(listOf("early", "late"), model.lotteries.map { it.id })
        assertEquals("site-1", model.ownedCampsites.single().id)
        assertFalse(model.isLoading)
    }

    // A failed load shows the error and stops the spinner
    @Test
    fun loadLotteries_failure_setsError() = runBlocking {
        lotteries.failOnGet = true

        controller.loadLotteries("owner-1")

        assertEquals("offline", model.errorMessage)
        assertFalse(model.isLoading)
        assertTrue(model.lotteries.isEmpty())
    }

    // A valid edit is saved first, then shown in the model, and the form closes
    @Test
    fun updateLottery_validEdit_savesAndUpdatesModel() = runBlocking {
        val original = lottery()
        lotteries.stored += original
        controller.loadLotteries("owner-1")
        controller.onEdit(original)

        val ok = update(original, winnerCount = "5", bookingEnd = "2027-05-25")

        assertTrue(ok)
        assertEquals(5, lotteries.updated.single().numberOfWinners)
        assertEquals("2027-05-25", lotteries.updated.single().bookingEnd)
        assertEquals(5, model.lotteries.single().numberOfWinners)
        assertNull(model.editingLottery)
        assertNull(model.errorMessage)
    }

    // The campsites and id are never changed by an edit
    @Test
    fun updateLottery_keepsIdOwnerAndCampsites() = runBlocking {
        val original = lottery(campsiteIds = listOf("site-1", "site-2"))
        lotteries.stored += original
        controller.loadLotteries("owner-1")

        update(original, winnerCount = "4")

        val saved = lotteries.updated.single()
        assertEquals("lottery-1", saved.id)
        assertEquals("owner-1", saved.ownerId)
        assertEquals(listOf("site-1", "site-2"), saved.campsiteIds)
    }
    // A date that is not a real day is rejected
    @Test
    fun updateLottery_dateNotARealDay_doesNotSave() = runBlocking {
        val original = lottery()
        lotteries.stored += original

        val ok = update(original, lotteryStart = "2027-02-31")

        assertFalse(ok)
        assertTrue(lotteries.updated.isEmpty())
        assertEquals("Enter the lottery range as YYYY-MM-DD.", model.errorMessage)
    }

    // A registration period that ends before it starts is rejected
    @Test
    fun updateLottery_registrationEndBeforeStart_doesNotSave() = runBlocking {
        val original = lottery()

        val ok = update(original, registrationEnd = "2027-04-01")

        assertFalse(ok)
        assertTrue(lotteries.updated.isEmpty())
        assertEquals("Registration end must be on or after the start.", model.errorMessage)
    }

    // A booking period that ends before it starts is rejected
    @Test
    fun updateLottery_bookingEndBeforeStart_doesNotSave() = runBlocking {
        val original = lottery()

        val ok = update(original, bookingEnd = "2027-05-01")

        assertFalse(ok)
        assertEquals("Booking end must be on or after the start.", model.errorMessage)
    }

    // A stay window that ends before it starts is rejected
    @Test
    fun updateLottery_stayEndBeforeStart_doesNotSave() = runBlocking {
        val original = lottery()

        val ok = update(original, lotteryEnd = "2027-05-01")

        assertFalse(ok)
        assertEquals("Lottery end must be on or after the lottery start.", model.errorMessage)
    }

    // Zero, empty and non-numeric winner counts are rejected
    @Test
    fun updateLottery_badWinnerCount_doesNotSave() = runBlocking {
        val original = lottery()

        for (bad in listOf("0", "", "abc")) {
            val ok = update(original, winnerCount = bad)
            assertFalse(ok)
            assertEquals("Winner count must be at least 1.", model.errorMessage)
        }
        assertTrue(lotteries.updated.isEmpty())
    }

    // Tapping the pencil opens the form for that lottery, and cancel closes it with no changes
    @Test
    fun onEditAndCancel_openAndCloseTheForm() = runBlocking {
        val original = lottery()

        controller.onEdit(original)
        assertEquals("lottery-1", model.editingLottery?.id)

        controller.onCancelEdit()
        assertNull(model.editingLottery)
        assertTrue(lotteries.updated.isEmpty())
    }

    private class MockLotteryRepository : LotteryRepository {
        val stored = mutableListOf<Lottery>()
        val updated = mutableListOf<Lottery>()
        var failOnGet = false
        var failOnUpdate = false

        override suspend fun getLotteries(): List<Lottery> {
            if (failOnGet) throw RuntimeException("offline")
            return stored
        }

        override suspend fun getLottery(id: String): Lottery? = stored.find { it.id == id }

        override suspend fun create(lottery: Lottery): String = "new-id"

        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> = emptyList()

        override suspend fun getLotteriesByOwner(ownerId: String): List<Lottery> {
            if (failOnGet) throw RuntimeException("offline")
            return stored.filter { it.ownerId == ownerId }
        }

        override suspend fun updateLottery(lottery: Lottery) {
            if (failOnUpdate) throw RuntimeException("offline")
            updated += lottery
        }
    }

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

        override suspend fun getCampsitePreview(
            campsiteId: String,
            photoId: String
        ): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? =
            null
    }
}

package com.example.camply.lottery.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryCreateModel
import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.lottery.repository.LotteryRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for creating an owner lottery.
 * US 02.04.01, US 02.04.02, US 02.04.03, US 02.04.04, US 02.04.05.
 */
class LotteryCreateControllerTest {

    private lateinit var model: LotteryCreateModel
    private lateinit var lotteries: FakeLotteryRepository
    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var controller: LotteryCreateController

    @Before
    fun setUp() {
        model = LotteryCreateModel()
        lotteries = FakeLotteryRepository()
        campsites = FakeCampsiteRepository()
        controller = LotteryCreateController(model, lotteries, campsites)
    }

    @Test
    fun createLottery_validForm_savesOwnedCampsitePeriodsAndWinners() = runBlocking {
        campsites.owned += Campsite(id = "site-1", ownerId = "owner-1")
        controller.loadOwnedCampsites("owner-1")
        controller.onCampsiteSelected("site-1")
        controller.onRegistrationStart("2026-11-01")
        controller.onRegistrationEnd("2026-11-05")
        controller.onBookingStart("2026-11-06")
        controller.onBookingEnd("2026-11-10")
        controller.onStayFrom("2026-12-01")
        controller.onStayTo("2026-12-05")
        controller.onWinnerCount("2")

        controller.createLottery("owner-1")

        assertEquals(1, lotteries.created.size)
        assertEquals(
            Lottery(
                lotteryStart = "2026-12-01",
                lotteryEnd = "2026-12-05",
                ownerId = "owner-1",
                campsiteIds = listOf("site-1"),
                registrationStart = "2026-11-01",
                registrationEnd = "2026-11-05",
                bookingStart = "2026-11-06",
                bookingEnd = "2026-11-10",
                numberOfWinners = 2,
                status = LotteryStatus.OPEN
            ),
            lotteries.created.single()
        )
        assertEquals("new-id", model.savedId)
        assertNull(model.errorMessage)
    }

    @Test
    fun createLottery_noCampsiteSelected_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onCampsiteSelected("site-1")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Select your campsite(s)", model.errorMessage)
    }

    @Test
    fun createLottery_registrationEndBeforeStart_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onRegistrationEnd("2026-10-01")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Registration end must be on or after the start.", model.errorMessage)
    }

    @Test
    fun createLottery_bookingEndBeforeStart_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onBookingEnd("2026-11-01")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Booking end must be on or after the start.", model.errorMessage)
    }

    @Test
    fun createLottery_winnerCountBelowOne_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onWinnerCount("0")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Winner count must be at least 1.", model.errorMessage)
    }
    @Test
    fun createLottery_lotteryEndBeforeStart_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onStayTo("2026-11-01")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Lottery end must be on or after the lottery start.", model.errorMessage)
    }

    @Test
    fun createLottery_lotteryDateNotARealDay_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onStayFrom("2026-02-31")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Enter the lottery range as YYYY-MM-DD.", model.errorMessage)
    }

    // testing for multiple campsites in lottery
    @Test
    fun createLottery_ForMultipleCampsitesTest() = runBlocking {
        fillValidForm()
        campsites.owned += Campsite(id = "site-2", ownerId = "owner-1")
        controller.loadOwnedCampsites("owner-1")
        controller.onCampsiteSelected("site-2")
        controller.createLottery("owner-1")

        assertEquals(1, lotteries.created.size)
        assertEquals(listOf("site-1", "site-2"), lotteries.created.single().campsiteIds)
        assertNull(model.errorMessage)

    }

    @Test
    fun campsiteAlreadyExistsInLottery_doesNotSave() = runBlocking {
        fillValidForm()
        lotteries.existing += existingLottery(listOf("site-1"), "2026-11-03", "2026-11-08")
        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("One of these campsites is already in an open lottery during this registration period.", model.errorMessage)
    }

    @Test
    fun createLottery_existingLotteryEndsBeforeNewStarts_saves() = runBlocking {
        fillValidForm()
        lotteries.existing += existingLottery(listOf("site-1"), "2026-10-01", "2026-10-31")

        controller.createLottery("owner-1")

        assertEquals(1, lotteries.created.size)
        assertNull(model.errorMessage)
    }

    @Test
    fun createLottery_periodsTouchOnOneDay_countsAsOverlap() = runBlocking {
        fillValidForm()
        lotteries.existing += existingLottery(listOf("site-1"), "2026-11-05", "2026-11-09")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals(
            "One of these campsites is already in an open lottery during this registration period.",
            model.errorMessage
        )
    }
    private suspend fun fillValidForm() {
        campsites.owned += Campsite(id = "site-1", ownerId = "owner-1")
        controller.loadOwnedCampsites("owner-1")
        controller.onCampsiteSelected("site-1")
        controller.onRegistrationStart("2026-11-01")
        controller.onRegistrationEnd("2026-11-05")
        controller.onStayFrom("2026-12-01")
        controller.onStayTo("2026-12-05")
        controller.onBookingStart("2026-11-06")
        controller.onBookingEnd("2026-11-10")
        controller.onWinnerCount("2")
    }

    private class FakeLotteryRepository : LotteryRepository {
        val created = mutableListOf<Lottery>()
        val existing = mutableListOf<Lottery>()
        val testGetLottery = false

        override suspend fun getLotteries(): List<Lottery> {
            if(testGetLottery) {
                throw RuntimeException("Runtime Error")

            }
            return existing + created
        }
        override suspend fun getLottery(id: String): Lottery? = (existing + created).find { it.id == id }
        override suspend fun create(lottery: Lottery): String {
            created += lottery
            return "new-id"
        }
        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> = emptyList()
    }

    private class FakeCampsiteRepository : CampsiteRepository {
        val owned = mutableListOf<Campsite>()

        override suspend fun getCampsites(): List<Campsite> = owned
        override suspend fun getCampsite(id: String): Campsite? = owned.find { it.id == id }
        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            owned.filter { it.ownerId == ownerId }
        override suspend fun saveCampsite(campsite: Campsite): String = error("not used")
        override suspend fun deleteCampsite(id: String) = error("not used")
    }
    private fun existingLottery(
        campsiteIds: List<String>,
        registrationStart: String,
        registrationEnd: String
    ) = Lottery(
        ownerId = "owner-2",
        campsiteIds = campsiteIds,
        registrationStart = registrationStart,
        registrationEnd = registrationEnd,
        bookingStart = "2026-11-20",
        bookingEnd = "2026-11-25",
        numberOfWinners = 1,
        lotteryStart = "2026-12-10",
        lotteryEnd = "2026-12-15",
        status = LotteryStatus.OPEN
    )

}
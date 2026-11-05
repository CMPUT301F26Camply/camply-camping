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
 * US 02.04.01, US 02.04.03, US 02.04.04, US 02.04.05.
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
        controller.onWinnerCount("2")

        controller.createLottery("owner-1")

        assertEquals(1, lotteries.created.size)
        assertEquals(
            Lottery(
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
    fun createLottery_campsiteNotOwned_doesNotSave() = runBlocking {
        fillValidForm()
        controller.onCampsiteSelected("someone-elses-site")

        controller.createLottery("owner-1")

        assertTrue(lotteries.created.isEmpty())
        assertEquals("Select one of your campsites.", model.errorMessage)
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

    private suspend fun fillValidForm() {
        campsites.owned += Campsite(id = "site-1", ownerId = "owner-1")
        controller.loadOwnedCampsites("owner-1")
        controller.onCampsiteSelected("site-1")
        controller.onRegistrationStart("2026-11-01")
        controller.onRegistrationEnd("2026-11-05")
        controller.onBookingStart("2026-11-06")
        controller.onBookingEnd("2026-11-10")
        controller.onWinnerCount("2")
    }

    private class FakeLotteryRepository : LotteryRepository {
        val created = mutableListOf<Lottery>()

        override suspend fun getLotteries(): List<Lottery> = created
        override suspend fun getLottery(id: String): Lottery? = created.find { it.id == id }
        override suspend fun create(lottery: Lottery): String {
            created += lottery
            return "new-id"
        }
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
}
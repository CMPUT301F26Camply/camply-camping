package com.example.camply.lottery.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.OwnerWaitlistLottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.lottery.repository.OwnerWaitlistRepository
import com.example.camply.notification.repository.RecipientNameRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for loading the owner's waitlist screen (US 02.09.01).
 */
class OwnerWaitlistControllerTest {

    private lateinit var model: OwnerWaitlistModel
    private lateinit var waitlistRepository: TestOwnerWaitlistRepository
    private lateinit var campsiteRepository: TestCampsiteRepository
    private lateinit var nameRepository: TestRecipientNameRepository
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
        controller = OwnerWaitlistController(
            model, waitlistRepository, campsiteRepository, nameRepository
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
        assertEquals("Lakeside", model.lotteries.single().campsiteName)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
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
        assertNull(model.lotteries.single().campsiteName)
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

    private fun lottery(id: String, campsiteId: String, registrationStart: String = "2026-11-01") =
        Lottery(
            id = id,
            ownerId = OWNER_ID,
            campsiteIds = listOf(campsiteId),
            registrationStart = registrationStart
        )

    private fun item(id: String) = OwnerWaitlistLottery(lottery(id, "campsite1"), "Lakeside")

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

        override suspend fun getCampsites(): List<Campsite> = campsites

        override suspend fun getCampsite(id: String): Campsite? {
            if (shouldFail) throw Exception("Could not read campsite.")
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

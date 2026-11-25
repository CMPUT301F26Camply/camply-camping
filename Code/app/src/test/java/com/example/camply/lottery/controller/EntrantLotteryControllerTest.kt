package com.example.camply.lottery.controller

import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.repository.LotteryRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EntrantLotteryController (US 01.04.01).
 *
 * These tests use a fake repository to verify that the controller passes the
 * correct entrant ID, updates the model, and handles repository errors.
 * They do not connect to Firestore or test database filtering.
 *
 * TODO: Add Firestore integration tests using the Firebase Emulator to verify that getJoinedLotteries returns only lotteries joined by the specified entrant.
 */
class EntrantLotteryControllerTest {

    private lateinit var model: EntrantLotteryModel
    private lateinit var repository: TestLotteryRepository
    private lateinit var controller: EntrantLotteryController

    /**
     * Creates the model, repository, and controller before each test.
     */
    @Before
    fun setUp() {
        model = EntrantLotteryModel()
        repository = TestLotteryRepository()
        controller = EntrantLotteryController(model, repository)
    }
    /**
     * Checks that the controller passes the entrant ID and stores the repository results.
     */
    @Test
    fun loadLotteries_repositoryResults_updatesModel() = runBlocking {
        val lottery = Lottery(id = "lottery1")
        repository.lotteries.add(lottery)

        controller.loadLotteries("entrant1")

        assertEquals("entrant1", repository.requestedEntrantId)
        assertEquals(listOf(lottery), model.lotteries)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that the model receives an empty list when the repository returns no lotteries.
     */
    @Test
    fun loadLotteries_emptyRepositoryResults_updatesModel() = runBlocking {
        controller.loadLotteries("entrant1")

        assertEquals(emptyList<Lottery>(), model.lotteries)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that repository errors are displayed by the model.
     */
    @Test
    fun loadLotteries_repositoryError_showsError() = runBlocking {
        repository.shouldFail = true

        controller.loadLotteries("entrant1")

        assertEquals("Could not load lotteries.", model.errorMessage)
        assertFalse(model.isLoading)
    }

    /**
     * Successfully retrying after a repository error clears the old error and displays
     * the newly loaded lotteries.
     */
    @Test
    fun loadLotteries_afterError_successClearsError() = runBlocking {
        repository.shouldFail = true
        controller.loadLotteries("entrant1")

        assertEquals("Could not load lotteries.", model.errorMessage)

        repository.shouldFail = false
        val lottery = Lottery(id = "lottery2")
        repository.lotteries.add(lottery)

        controller.loadLotteries("entrant1")

        assertEquals(listOf(lottery), model.lotteries)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Loading new lottery results replaces outdated entries in the model rather than keeping both lists.
     */
    @Test
    fun loadLotteries_newResults_replacePreviousResults() = runBlocking {
        model.showLotteries(listOf(Lottery(id = "oldLottery")))

        val newLottery = Lottery(id = "newLottery")
        repository.lotteries.add(newLottery)

        controller.loadLotteries("entrant1")

        assertEquals(listOf(newLottery), model.lotteries)
        assertEquals("entrant1", repository.requestedEntrantId)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Provides predefined lottery data to test the controller without Firestore.
     * Entrant membership filtering is handled by the real Firestore repository.
     */
    private class TestLotteryRepository : LotteryRepository {
        val lotteries = mutableListOf<Lottery>()
        var shouldFail = false
        var requestedEntrantId: String? = null

        override suspend fun getLotteries(): List<Lottery> = lotteries

        override suspend fun getLottery(id: String): Lottery? {
            if (shouldFail) throw Exception("Could not load lotteries.")

            for (lottery in lotteries) {
                if (lottery.id == id) return lottery
            }

            return null
        }
        override suspend fun create(lottery: Lottery): String = error("not used by these tests.")

        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> {
            requestedEntrantId = entrantId

            if (shouldFail) throw Exception("Could not load lotteries.")

            return lotteries
        }
    }
}
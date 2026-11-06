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
 * Unit tests for loading entrant lotteries in EntrantLotteryController (US 01.04.01).
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
     * Checks that lotteries joined by the entrant are loaded.
     */
    @Test
    fun loadLotteries_joinedLotteries_loadsLotteries() = runBlocking {
        val lottery = Lottery(id = "lottery1")
        repository.lotteries.add(lottery)

        controller.loadLotteries("entrant1")

        assertEquals("entrant1", repository.requestedEntrantId)
        assertEquals(listOf(lottery), model.lotteries)
        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
    }

    /**
     * Checks that an entrant with no joined lotteries gets an empty list.
     */
    @Test
    fun loadLotteries_noJoinedLotteries_returnsEmptyList() = runBlocking {
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
     * Provides lottery data for testing without connecting to Firestore.
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
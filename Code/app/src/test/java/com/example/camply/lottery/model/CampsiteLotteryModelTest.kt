package com.example.camply.lottery.model

import com.example.camply.lottery.repository.ActiveWaitingList
import com.example.camply.lottery.repository.WaitingListRepository
import org.junit.Assert.*
import org.junit.Test

class CampsiteLotteryModelTest {
    @Test
    fun savedMembershipIsRestoredWithoutJoiningAgain() {
        val repository = LookupRepository()
        val model = CampsiteLotteryModel(repository, "site", "entrant")
        model.load()
        assertTrue(model.isLoading)
        repository.complete(ActiveWaitingList("lottery", true))
        assertFalse(model.isLoading)
        assertEquals(WaitingListModel.JoinState.ALREADY_JOINED, model.waitingList?.state)
    }

    @Test
    fun lookupFailureCanBeRetriedAndNoLotteryIsNotAnError() {
        val repository = LookupRepository()
        val model = CampsiteLotteryModel(repository, "site", "entrant")
        model.load()
        repository.fail(Exception("Unavailable"))
        assertTrue(model.hasError)
        assertFalse(model.isLoading)
        model.load()
        assertFalse(model.hasError)
        repository.complete(null)
        assertFalse(model.isLoading)
        assertFalse(model.hasError)
        assertNull(model.waitingList)
    }

    private class LookupRepository : WaitingListRepository {
        override fun leave(
            lotteryId: String,
            entrantId: String,
            onComplete: (WaitingList.LeaveResult) -> Unit,
            onFailure: (Exception) -> Unit
        ) = error("Loading membership must not remove an entry.")

        lateinit var complete: (ActiveWaitingList?) -> Unit
        lateinit var fail: (Exception) -> Unit

        override fun findForCampsite(
            campsiteId: String,
            entrantId: String,
            onComplete: (ActiveWaitingList?) -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            assertEquals("site", campsiteId)
            assertEquals("entrant", entrantId)
            complete = onComplete
            fail = onFailure
        }

        override fun join(
            lotteryId: String,
            entrantId: String,
            onComplete: (WaitingList.JoinResult) -> Unit,
            onFailure: (Exception) -> Unit
        ) = error("Loading membership must not create an entry.")
    }
}

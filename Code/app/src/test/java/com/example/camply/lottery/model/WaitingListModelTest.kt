package com.example.camply.lottery.model

import com.example.camply.lottery.repository.WaitingListRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class WaitingListModelTest {
    @Test
    fun leaveWaitsForCommitAndAllowsRejoiningAfterSuccess() {
        val repository = PendingRepository()
        val model = WaitingListModel(repository, "lottery", "entrant", alreadyJoined = true)
        model.leave()
        model.leave()
        model.join()
        assertEquals(1, repository.leaveCalls)
        assertEquals(0, repository.calls)
        assertEquals(WaitingListModel.JoinState.LEAVING, model.state)
        repository.completeLeave(WaitingList.LeaveResult.LEFT)
        assertEquals(WaitingListModel.JoinState.LEFT, model.state)
        model.join()
        assertEquals(1, repository.calls)
    }

    @Test
    fun failedLeaveRetriesWithdrawalRatherThanJoin() {
        val repository = PendingRepository()
        val model = WaitingListModel(repository, "lottery", "entrant", alreadyJoined = true)
        model.leave()
        repository.fail(Exception("Offline"))
        assertEquals(WaitingListModel.JoinState.LEAVE_ERROR, model.state)
        model.join()
        assertEquals(0, repository.calls)
        model.leave()
        assertEquals(2, repository.leaveCalls)
        repository.completeLeave(WaitingList.LeaveResult.NOT_JOINED)
        assertEquals(WaitingListModel.JoinState.LEFT, model.state)
    }

    @Test
    fun finalizedOrClosedEntryCannotBeRetriedAsAJoin() {
        for ((result, expected) in listOf(
            WaitingList.LeaveResult.ENTRY_FINALIZED to WaitingListModel.JoinState.ENTRY_FINALIZED,
            WaitingList.LeaveResult.REGISTRATION_CLOSED to WaitingListModel.JoinState.REGISTRATION_CLOSED
        )) {
            val repository = PendingRepository()
            val model = WaitingListModel(repository, "lottery", "entrant", alreadyJoined = true)
            model.leave()
            repository.completeLeave(result)
            assertEquals(expected, model.state)
            model.join()
            model.leave()
            assertEquals(0, repository.calls)
            assertEquals(1, repository.leaveCalls)
        }
    }

    @Test
    fun joinWaitsForSavedResultAndIgnoresRepeatedClicks() {
        val repository = PendingRepository()
        val model = WaitingListModel(repository, "lottery", "entrant")

        model.join()
        model.join()
        assertEquals(1, repository.calls)
        assertEquals(WaitingListModel.JoinState.JOINING, model.state)

        repository.complete(WaitingList.JoinResult.JOINED)
        assertEquals(WaitingListModel.JoinState.JOINED, model.state)
        model.join()
        assertEquals(1, repository.calls)
    }

    @Test
    fun failedSaveAllowsRetryWithoutReportingMembership() {
        val repository = PendingRepository()
        val model = WaitingListModel(repository, "lottery", "entrant")

        model.join()
        repository.fail(Exception("Save failed"))
        assertEquals(WaitingListModel.JoinState.ERROR, model.state)

        model.join()
        assertEquals(2, repository.calls)
        repository.complete(WaitingList.JoinResult.ALREADY_JOINED)
        assertEquals(WaitingListModel.JoinState.ALREADY_JOINED, model.state)
    }

    @Test
    fun rejectedJoinDoesNotReportSuccess() {
        val repository = PendingRepository()
        val model = WaitingListModel(repository, "lottery", "entrant")

        model.join()
        repository.complete(WaitingList.JoinResult.FULL)
        assertEquals(WaitingListModel.JoinState.FULL, model.state)

        model.join()
        repository.complete(WaitingList.JoinResult.REGISTRATION_CLOSED)
        assertEquals(WaitingListModel.JoinState.REGISTRATION_CLOSED, model.state)
    }

    private class PendingRepository : WaitingListRepository {
        override fun findForCampsite(
            campsiteId: String,
            entrantId: String,
            onComplete: (com.example.camply.lottery.repository.ActiveWaitingList?) -> Unit,
            onFailure: (Exception) -> Unit
        ) = error("Lookup is not used by these join tests.")

        var calls = 0
        var leaveCalls = 0
        lateinit var completeLeave: (WaitingList.LeaveResult) -> Unit
        lateinit var complete: (WaitingList.JoinResult) -> Unit
        lateinit var fail: (Exception) -> Unit

        override fun leave(
            lotteryId: String,
            entrantId: String,
            onComplete: (WaitingList.LeaveResult) -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            assertEquals("lottery", lotteryId)
            assertEquals("entrant", entrantId)
            leaveCalls++
            completeLeave = onComplete
            fail = onFailure
        }

        override fun join(
            lotteryId: String,
            entrantId: String,
            onComplete: (WaitingList.JoinResult) -> Unit,
            onFailure: (Exception) -> Unit
        ) {
            assertEquals("lottery", lotteryId)
            assertEquals("entrant", entrantId)
            calls++
            complete = onComplete
            fail = onFailure
        }
    }
}

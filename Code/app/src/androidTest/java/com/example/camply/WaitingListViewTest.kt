package com.example.camply

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.lottery.controller.WaitingListController
import com.example.camply.lottery.model.WaitingList
import com.example.camply.lottery.model.WaitingListModel
import com.example.camply.lottery.repository.ActiveWaitingList
import com.example.camply.lottery.repository.WaitingListRepository
import com.example.camply.lottery.view.WaitingListView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercises real Android UI with controlled save callbacks; makes no Firebase writes. */
class WaitingListViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun joinThenLeaveUpdatesButtonsOnlyAfterSavedCallbacks() {
        val repository = TestRepository()
        showView(repository)
        compose.onNodeWithText("Join waiting list").performClick()
        compose.onNodeWithText("Joining…").assertIsNotEnabled()
        compose.runOnIdle { repository.joinComplete(WaitingList.JoinResult.JOINED) }
        compose.onNodeWithText("Leave waiting list").assertIsEnabled().performClick()
        compose.onNodeWithText("Leaving...").assertIsNotEnabled()
        compose.runOnIdle { repository.leaveComplete(WaitingList.LeaveResult.LEFT) }
        compose.onNodeWithText("You are no longer on this waiting list.").assertIsDisplayed()
        compose.onNodeWithText("Join waiting list").assertIsEnabled()
    }

    @Test
    fun failedLeaveOffersWithdrawalRetry() {
        val repository = TestRepository()
        showView(repository, alreadyJoined = true)
        compose.onNodeWithText("Leave waiting list").performClick()
        compose.runOnIdle { repository.failure(Exception("Offline")) }
        compose.onNodeWithText("Try leaving again").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, repository.leaveCalls) }
    }

    @Test
    fun fullListShowsFeedbackAndAllowsAnotherCheck() {
        val repository = TestRepository()
        showView(repository)
        compose.onNodeWithText("Join waiting list").performClick()
        compose.runOnIdle { repository.joinComplete(WaitingList.JoinResult.FULL) }
        compose.onNodeWithText("The waiting list is full. You can check availability again.").assertIsDisplayed()
        compose.onNodeWithText("Check availability again").assertIsEnabled()
    }

    private fun showView(repository: TestRepository, alreadyJoined: Boolean = false) {
        val model = WaitingListModel(repository, "lottery", "entrant", alreadyJoined)
        val view = WaitingListView(model, WaitingListController(model))
        compose.setContent { view.Display() }
    }

    private class TestRepository : WaitingListRepository {
        lateinit var joinComplete: (WaitingList.JoinResult) -> Unit
        lateinit var leaveComplete: (WaitingList.LeaveResult) -> Unit
        lateinit var failure: (Exception) -> Unit
        var leaveCalls = 0

        override fun findForCampsite(
            campsiteId: String, entrantId: String,
            onComplete: (ActiveWaitingList?) -> Unit, onFailure: (Exception) -> Unit
        ) = error("Lookup is not used in these UI tests.")

        override fun join(
            lotteryId: String, entrantId: String,
            onComplete: (WaitingList.JoinResult) -> Unit, onFailure: (Exception) -> Unit
        ) {
            joinComplete = onComplete
            failure = onFailure
        }

        override fun leave(
            lotteryId: String, entrantId: String,
            onComplete: (WaitingList.LeaveResult) -> Unit, onFailure: (Exception) -> Unit
        ) {
            leaveCalls++
            leaveComplete = onComplete
            failure = onFailure
        }
    }
}

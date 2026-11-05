package com.example.camply.lottery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.controller.WaitingListController
import com.example.camply.lottery.model.WaitingListModel
import com.example.camply.lottery.model.WaitingListModel.JoinState
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton

/** Join controls for the campsite details extraContent slot (US 01.02.01). */
class WaitingListView(
    private val model: WaitingListModel,
    private val controller: WaitingListController
) {
    @Composable
    fun Display() {
        WaitingListContent(model.state, controller::joinWaitingList, controller::leaveWaitingList)
    }
}

/** Rendering only: persistence and join rules remain in the model/repository. */
@Composable
private fun WaitingListContent(state: JoinState, onJoin: () -> Unit, onLeave: () -> Unit = {}) {
    val leaveAction = state == JoinState.JOINED || state == JoinState.ALREADY_JOINED || state == JoinState.LEAVE_ERROR
    val buttonLabel = when (state) {
        JoinState.JOINING -> "Joining…"
        JoinState.JOINED, JoinState.ALREADY_JOINED -> "Leave waiting list"
        JoinState.LEAVING -> "Leaving..."
        JoinState.LEAVE_ERROR -> "Try leaving again"
        JoinState.ENTRY_FINALIZED -> "Entry finalized"
        JoinState.LEFT -> "Join waiting list"
        JoinState.ERROR -> "Try again"
        JoinState.FULL -> "Check availability again"
        JoinState.REGISTRATION_CLOSED -> "Registration closed"
        JoinState.IDLE -> "Join waiting list"
    }
    val message = when (state) {
        JoinState.LEAVING -> "Removing your entry..."
        JoinState.LEFT -> "You are no longer on this waiting list."
        JoinState.LEAVE_ERROR -> "Unable to confirm withdrawal. Try leaving again."
        JoinState.ENTRY_FINALIZED -> "This entry has a lottery result and cannot be removed from the waiting list."
        JoinState.IDLE -> "Join the lottery waiting list for a chance to reserve this campsite."
        JoinState.JOINING -> "Saving your entry…"
        JoinState.JOINED -> "You have joined the waiting list. This is not a confirmed reservation."
        JoinState.ALREADY_JOINED -> "You already have an entry in this lottery."
        JoinState.REGISTRATION_CLOSED -> "This lottery is not accepting entries."
        JoinState.FULL -> "The waiting list is full. You can check availability again."
        JoinState.ERROR -> "Unable to confirm your entry. Check your connection and try again."
    }

    OrangeCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Campsite lottery",
                style = MaterialTheme.typography.titleMedium,
                color = CamplyPalette.Ink
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = CamplyPalette.Ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            PrimaryButton(
                text = buttonLabel,
                onClick = if (leaveAction) onLeave else onJoin,
                enabled = leaveAction || state == JoinState.IDLE || state == JoinState.LEFT ||
                    state == JoinState.ERROR || state == JoinState.FULL
            )
        }
    }
}

@Preview(name = "Waitlist - join", showBackground = true)
@Composable
private fun JoinWaitingListPreview() {
    CamplyScreen("Campsite details") {
        WaitingListContent(JoinState.IDLE, onJoin = {})
    }
}

@Preview(name = "Waitlist - saved", showBackground = true)
@Composable
private fun JoinedWaitingListPreview() {
    CamplyScreen("Campsite details") {
        WaitingListContent(JoinState.JOINED, onJoin = {})
    }
}

@Preview(name = "Waitlist - error", showBackground = true)
@Composable
private fun FailedWaitingListPreview() {
    CamplyScreen("Campsite details") {
        WaitingListContent(JoinState.ERROR, onJoin = {})
    }
}

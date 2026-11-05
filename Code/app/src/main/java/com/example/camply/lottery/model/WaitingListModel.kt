package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.lottery.repository.WaitingListRepository

/**
 * Observable join state for one entrant and lottery (US 01.02.01).
 *
 * Create a separate instance when either ID changes. The repository owns
 * persistence; successful membership is reported only after its callback.
 */
class WaitingListModel(
    private val repository: WaitingListRepository,
    private val lotteryId: String,
    private val entrantId: String,
    alreadyJoined: Boolean = false
) {
    init {
        require(lotteryId.isNotBlank()) { "Lottery ID must not be blank." }
        require(entrantId.isNotBlank()) { "Entrant ID must not be blank." }
    }

    var state by mutableStateOf(if (alreadyJoined) JoinState.ALREADY_JOINED else JoinState.IDLE)
        private set

    /** Prevents repeat submissions while saving or after membership is confirmed. */
    fun join() {
        if (state !in listOf(JoinState.IDLE, JoinState.LEFT, JoinState.ERROR, JoinState.FULL)) return

        state = JoinState.JOINING
        try {
            repository.join(
                lotteryId = lotteryId,
                entrantId = entrantId,
                onComplete = { result ->
                    state = when (result) {
                        WaitingList.JoinResult.JOINED -> JoinState.JOINED
                        WaitingList.JoinResult.ALREADY_JOINED -> JoinState.ALREADY_JOINED
                        WaitingList.JoinResult.REGISTRATION_CLOSED -> JoinState.REGISTRATION_CLOSED
                        WaitingList.JoinResult.FULL -> JoinState.FULL
                    }
                },
                onFailure = { state = JoinState.ERROR }
            )
        } catch (_: Exception) {
            state = JoinState.ERROR
        }
    }

    fun leave() {
        if (state !in listOf(JoinState.JOINED, JoinState.ALREADY_JOINED, JoinState.LEAVE_ERROR)) return
        state = JoinState.LEAVING
        try {
            repository.leave(lotteryId, entrantId,
                onComplete = { result ->
                    state = when (result) {
                        WaitingList.LeaveResult.LEFT, WaitingList.LeaveResult.NOT_JOINED -> JoinState.LEFT
                        WaitingList.LeaveResult.REGISTRATION_CLOSED -> JoinState.REGISTRATION_CLOSED
                        WaitingList.LeaveResult.ENTRY_FINALIZED -> JoinState.ENTRY_FINALIZED
                    }
                },
                onFailure = { state = JoinState.LEAVE_ERROR }
            )
        } catch (_: Exception) {
            state = JoinState.LEAVE_ERROR
        }
    }

    enum class JoinState {
        IDLE,
        JOINING,
        JOINED,
        ALREADY_JOINED,
        REGISTRATION_CLOSED,
        FULL,
        ERROR,
        LEAVING,
        LEFT,
        LEAVE_ERROR,
        ENTRY_FINALIZED
    }
}

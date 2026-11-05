package com.example.camply.lottery.repository

import com.example.camply.lottery.model.WaitingList

/**
 * Persistence boundary for joining a lottery (US 01.02.01).
 *
 * Implementations must atomically read the lottery's registration eligibility,
 * capacity and current membership, then save at most one entry per entrant.
 * They must preserve existing draw results and never replace a lottery's entries
 * with a stale local list. Collection names and fields are defined by the shared
 * lottery schema, rather than by this interface.
 */
interface WaitingListRepository {
    /** Deletes only a waiting entry during registration, updating the count atomically. */
    fun leave(
        lotteryId: String,
        entrantId: String,
        onComplete: (WaitingList.LeaveResult) -> Unit,
        onFailure: (Exception) -> Unit
    )

    /** Finds the currently registering lottery and the entrant's saved membership. */
    fun findForCampsite(
        campsiteId: String,
        entrantId: String,
        onComplete: (ActiveWaitingList?) -> Unit,
        onFailure: (Exception) -> Unit
    )

    /**
     * Calls onComplete with JOINED only after the entry is committed.
     * Calls onFailure for storage or network failures, not normal rejections.
     * Exactly one callback must be invoked for each completed operation.
     */
    fun join(
        lotteryId: String,
        entrantId: String,
        onComplete: (WaitingList.JoinResult) -> Unit,
        onFailure: (Exception) -> Unit
    )
}

data class ActiveWaitingList(val lotteryId: String, val alreadyJoined: Boolean)

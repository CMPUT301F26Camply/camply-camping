package com.example.camply.lottery.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.lottery.repository.WaitingListRepository

/** Loads the active lottery and saved membership for one campsite/entrant pair. */
class CampsiteLotteryModel(
    private val repository: WaitingListRepository,
    private val campsiteId: String,
    private val entrantId: String
) {
    var isLoading by mutableStateOf(true)
        private set
    var hasError by mutableStateOf(false)
        private set
    var waitingList by mutableStateOf<WaitingListModel?>(null)
        private set

    fun load() {
        isLoading = true
        hasError = false
        waitingList = null
        repository.findForCampsite(campsiteId, entrantId,
            onComplete = { active ->
                waitingList = active?.let {
                    WaitingListModel(repository, it.lotteryId, entrantId, it.alreadyJoined)
                }
                isLoading = false
            },
            onFailure = {
                hasError = true
                isLoading = false
            }
        )
    }
}

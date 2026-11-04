package com.example.camply.lottery.controller

import com.example.camply.lottery.model.WaitingListModel

/** Forwards the view's Join action to the model, following Camply's MVC structure. */
class WaitingListController(private val model: WaitingListModel) {
    fun leaveWaitingList() {
        model.leave()
    }

    fun joinWaitingList() {
        model.join()
    }
}

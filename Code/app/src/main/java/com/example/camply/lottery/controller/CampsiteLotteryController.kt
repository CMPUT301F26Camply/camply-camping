package com.example.camply.lottery.controller

import com.example.camply.lottery.model.CampsiteLotteryModel

class CampsiteLotteryController(private val model: CampsiteLotteryModel) {
    fun loadLottery() = model.load()
}

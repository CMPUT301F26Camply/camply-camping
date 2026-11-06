package com.example.camply.lottery.service

import com.example.camply.lottery.model.ALBERTA
import com.example.camply.lottery.repository.LotteryDrawRepository
import com.example.camply.notification.service.NotificationService
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * While Camply is open, checks for lotteries whose lotteryEnd has passed.
 * Stores each entrant's win or loss message, then marks that lottery DRAWN.
 * US 02.05.01.
 */
class LotteryDrawService(
    private val repository: LotteryDrawRepository,
    private val notifications: NotificationService,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now(ALBERTA) }
) {
    /** Checks once a minute for as long as the app stays open. */
    suspend fun keepChecking() {
        while (true) {
            try {
                deliverDueLotteries()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed check stays in the loop and runs again after the delay.
            }
            delay(60_000)
        }
    }

    /** Sends the result messages, then marks each finished lottery DRAWN. */
    internal suspend fun deliverDueLotteries() {
        val draws = repository.drawDueLotteries(clock())
        for (draw in draws) {
            for (entrantId in draw.winnerIds) {
                notifications.sendLotteryResult(draw.ownerId, entrantId, draw.lotteryId, true)
            }
            for (entrantId in draw.loserIds) {
                notifications.sendLotteryResult(draw.ownerId, entrantId, draw.lotteryId, false)
            }
            repository.markDrawn(draw.lotteryId)
        }
    }
}
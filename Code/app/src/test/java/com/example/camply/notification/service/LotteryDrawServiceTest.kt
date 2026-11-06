package com.example.camply.lottery.service

import com.example.camply.lottery.model.ALBERTA
import com.example.camply.lottery.repository.LotteryDrawRepository
import com.example.camply.lottery.repository.RecordedDraw
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class LotteryDrawServiceTest {
    private val now = ZonedDateTime.of(LocalDate.parse("2026-11-10"), LocalTime.of(23, 59), ALBERTA)

    @Test
    fun winnerAndLoserAreNotifiedAndOwnerIsNot() = runBlocking {
        val draws = listOf(RecordedDraw("lot1", "owner", listOf("winner"), listOf("loser")))
        val drawRepository = FakeDrawRepository(draws)
        val saved = MemoryNotificationRepository()
        val service = LotteryDrawService(drawRepository, NotificationService(saved)) { now }

        service.deliverDueLotteries()

        val messages = saved.records.values
        assertEquals(setOf("winner", "loser"), messages.map { it.recipientId }.toSet())
        assertTrue(messages.none { it.recipientId == "owner" })
        assertEquals(NotificationType.LOTTERY_WIN, messages.single { it.recipientId == "winner" }.type)
        assertEquals(NotificationType.LOTTERY_LOSE, messages.single { it.recipientId == "loser" }.type)
        assertEquals(listOf("lot1"), drawRepository.markedDrawn)
    }

    @Test
    fun nobodyJoinedStillMarksTheLotteryDrawn() = runBlocking {
        val draws = listOf(RecordedDraw("lot1", "owner", emptyList(), emptyList()))
        val drawRepository = FakeDrawRepository(draws)
        val saved = MemoryNotificationRepository()
        val service = LotteryDrawService(drawRepository, NotificationService(saved)) { now }

        service.deliverDueLotteries()

        assertTrue(saved.records.isEmpty())
        assertEquals(listOf("lot1"), drawRepository.markedDrawn)
    }

    @Test
    fun failedMessageDoesNotMarkTheLotteryDrawn() {
        val draws = listOf(RecordedDraw("lot1", "owner", listOf("winner"), emptyList()))
        val drawRepository = FakeDrawRepository(draws)
        val saved = MemoryNotificationRepository(failWrite = true)
        val service = LotteryDrawService(drawRepository, NotificationService(saved)) { now }

        try {
            runBlocking { service.deliverDueLotteries() }
        } catch (e: IllegalStateException) {
            assertEquals("Storage unavailable", e.message)
        }
        assertTrue(drawRepository.markedDrawn.isEmpty())
    }

    private class FakeDrawRepository(private val draws: List<RecordedDraw>) : LotteryDrawRepository {
        val markedDrawn = mutableListOf<String>()
        override suspend fun drawDueLotteries(now: ZonedDateTime) = draws
        override suspend fun markDrawn(lotteryId: String) {
            markedDrawn.add(lotteryId)
        }
    }

    private class MemoryNotificationRepository(private val failWrite: Boolean = false) : NotificationRepository {
        val records = mutableMapOf<String, NewNotification>()

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            check(!failWrite) { "Storage unavailable" }
            records[id] = notification
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = {}

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = {}

        override suspend fun markAsRead(notificationId: String, recipientId: String) = Unit
    }
}
package com.example.camply.notification.service

import com.example.camply.campsite.model.DateRange
import com.example.camply.notification.model.*
import com.example.camply.notification.repository.NotificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationServiceTest {
    @Test
    fun winAndLoseRecordsUseCorrectRecipientsAndTypes() = runBlocking {
        val repository = MemoryRepository()
        val service = NotificationService(repository)
        service.sendLotteryResult("owner", "winner", "lottery", true)
        service.sendLotteryResult("owner", "loser", "lottery", false)
        assertEquals(2, repository.records.size)
        val winner = repository.records.values.single { it.recipientId == "winner" }
        val loser = repository.records.values.single { it.recipientId == "loser" }
        assertEquals(NotificationType.LOTTERY_WIN, winner.type)
        assertEquals(NotificationType.LOTTERY_LOSE, loser.type)
        assertEquals("owner", winner.senderId)
        assertEquals("lottery", winner.lotteryId)
        assertEquals(NotificationOrigin.SYSTEM, winner.origin)
        assertFalse(winner.content.isBlank())
        assertNotEquals(winner.content, loser.content)
    }

    @Test
    fun retryUsesSameRecordAndDoesNotCreateAnotherNotification() = runBlocking {
        val repository = MemoryRepository()
        val service = NotificationService(repository)
        assertEquals(SendResult.CREATED, service.sendLotteryResult("owner", "entrant", "lottery", true))
        assertEquals(SendResult.ALREADY_EXISTS, service.sendLotteryResult("owner", "entrant", "lottery", true))
        assertEquals(1, repository.records.size)
        assertTrue(repository.records.keys.single().matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun changedResultForSameEventIsRejected() {
        val repository = MemoryRepository()
        val service = NotificationService(repository)
        runBlocking { service.sendLotteryResult("owner", "entrant", "lottery", true) }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.sendLotteryResult("owner", "entrant", "lottery", false) }
        }
        assertEquals(NotificationType.LOTTERY_WIN, repository.records.values.single().type)
    }

    @Test
    fun failedWritePropagatesAndCanBeRetried() {
        val repository = MemoryRepository()
        val service = NotificationService(repository)
        repository.failWrite = true
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.sendLotteryResult("owner", "entrant", "lottery", true) }
        }
        assertTrue(repository.records.isEmpty())
        repository.failWrite = false
        runBlocking {
            assertEquals(SendResult.CREATED, service.sendLotteryResult("owner", "entrant", "lottery", true))
        }
    }

    @Test
    fun blankIdentityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                NotificationService(MemoryRepository()).sendLotteryResult("owner", "", "lottery", true)
            }
        }
    }

    /**
     * Checks that a reservation confirmation reaches the entrant as a system notification.
     */
    @Test
    fun reservationConfirmationUsesCorrectRecipientTypeAndOrigin() = runBlocking {
        val repository = MemoryRepository()

        NotificationService(repository).sendReservationConfirmation(
            "owner", "entrant", "reservation", "Banff Pines", DateRange("2027-06-12", "2027-06-15")
        )

        val record = repository.records.values.single()
        assertEquals("entrant", record.recipientId)
        assertEquals("owner", record.senderId)
        assertEquals(NotificationType.RESERVATION_CONFIRMED, record.type)
        assertEquals(NotificationOrigin.SYSTEM, record.origin)
        assertEquals("reservation", record.reservationId)
    }

    /**
     * Checks that a reservation confirmation names the campsite and the reserved dates.
     */
    @Test
    fun reservationConfirmationIncludesCampsiteAndDates() = runBlocking {
        val repository = MemoryRepository()

        NotificationService(repository).sendReservationConfirmation(
            "owner", "entrant", "reservation", "Banff Pines", DateRange("2027-06-12", "2027-06-15")
        )

        assertEquals(
            "Your reservation at Banff Pines from 2027-06-12 to 2027-06-15 is confirmed.",
            repository.records.values.single().content
        )
    }

    private class MemoryRepository : NotificationRepository {
        val records = mutableMapOf<String, NewNotification>()
        var failWrite = false

        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult {
            check(!failWrite) { "Storage unavailable" }
            val existing = records[id]
            if (existing != null) {
                check(existing == notification) { "Conflicting event" }
                return SendResult.ALREADY_EXISTS
            }
            records[id] = notification
            return SendResult.CREATED
        }

        override fun observeReceived(
            recipientId: String, onUpdate: (List<Notification>) -> Unit, onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by service tests")

        override fun observeSent(
            senderId: String, onUpdate: (List<Notification>) -> Unit, onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by service tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by service tests")
    }
}

package com.example.camply.messaging

import com.example.camply.messaging.controller.MessageLogController
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.MessageLogModel
import com.example.camply.messaging.repository.MessageLogRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies grouping across reservations, message ordering, and denied access. */
class MessageLogControllerTest {
    private val model = MessageLogModel()
    private val repository = FakeRepository()
    private val controller = MessageLogController(model, repository) { id ->
        mapOf("owner-a" to "Taylor Morgan", "owner-b" to "Sam Rivera")[id]
    }

    @Test
    fun groupsReservationsByOwnerAndSortsHistory() = runBlocking {
        controller.loadOwners("admin")

        assertEquals(listOf("Sam Rivera", "Taylor Morgan"), model.owners.map { it.name })
        assertEquals(listOf("new", "older"), model.owners.last().conversations.map { it.id })

        controller.loadHistory("admin", "new")
        assertEquals(listOf("owner-a", "entrant"), model.messages.map { it.senderId })
    }

    @Test
    fun unprivilegedAccountCannotLoadDirectory() = runBlocking {
        controller.loadOwners("outsider")

        assertEquals("Your account does not have permission to review message logs.", model.error)
        assertTrue(model.owners.isEmpty())
        assertFalse(model.isLoading)
    }

    private class FakeRepository : MessageLogRepository {
        override suspend fun loadLoggedConversations(adminId: String): List<Conversation> {
            if (adminId != "admin") throw SecurityException()
            return listOf(
                Conversation(id = "older", ownerId = "owner-a", updatedAt = Timestamp(1L, 0)),
                Conversation(id = "separate", ownerId = "owner-b"),
                Conversation(id = "new", ownerId = "owner-a", updatedAt = Timestamp(2L, 0))
            )
        }

        override suspend fun loadMessageHistory(adminId: String, conversationId: String) = listOf(
            ConversationMessage("reply", "entrant", "Thanks!", Timestamp(3L, 0)),
            ConversationMessage("first", "owner-a", "Check-in starts at 3 pm.", Timestamp(2L, 0))
        )
    }
}

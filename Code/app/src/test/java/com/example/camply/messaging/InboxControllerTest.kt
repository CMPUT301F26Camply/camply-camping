package com.example.camply.messaging

import com.example.camply.messaging.controller.InboxController
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.ConversationRole
import com.example.camply.messaging.model.InboxModel
import com.example.camply.messaging.repository.ConversationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests message ordering and safe retry through the owner conversation controller. */
class InboxControllerTest {
    private val model = InboxModel()
    private val repository = FakeRepository()
    private val controller = InboxController(model, repository)

    @Test
    fun reservationLinkOpensItsConversationAndSortsMessages() = runBlocking {
        controller.openReservation("owner", "booking", ConversationRole.OWNER)

        assertEquals("booking", model.conversation?.id)
        assertEquals(listOf("first", "second"), model.messages.map { it.id })
        assertFalse(model.isLoading)
    }

    @Test
    fun sendingIgnoresBlankDraftAndRetainsFailedDraftForRetry() = runBlocking {
        controller.openReservation("entrant", "booking", ConversationRole.ENTRANT)
        controller.editDraft("  ")
        controller.send("entrant")
        assertEquals(emptyList<String>(), repository.saved)

        controller.editDraft("  Your site is ready.  ")
        repository.failSend = true
        controller.send("entrant")
        assertEquals("  Your site is ready.  ", model.draft)
        assertEquals("Could not send your message. Try again.", model.sendError)
        assertFalse(model.isSending)

        repository.failSend = false
        controller.send("entrant")
        assertEquals(listOf("Your site is ready."), repository.saved)
        assertEquals("", model.draft)
        assertNull(model.sendError)
    }

    private class FakeRepository : ConversationRepository {
        private val thread = Conversation(id = "booking", ownerId = "owner", entrantId = "entrant")
        var failSend = false
        val saved = mutableListOf<String>()

        override fun observeConversations(userId: String, role: ConversationRole) = flowOf(listOf(thread))

        override suspend fun openForReservation(
            reservationId: String, userId: String, role: ConversationRole
        ): Conversation {
            check(reservationId == thread.id && role.userId(thread) == userId)
            return thread
        }

        override fun observeMessages(conversation: Conversation, userId: String) = flowOf(
            listOf(
                ConversationMessage(id = "second", sentAt = Timestamp(20L, 0)),
                ConversationMessage(id = "first", sentAt = Timestamp(10L, 0))
            )
        )

        override suspend fun sendMessage(conversation: Conversation, senderId: String, text: String) {
            check(!failSend)
            saved += text
        }
    }
}

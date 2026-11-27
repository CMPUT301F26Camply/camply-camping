package com.example.camply.messaging

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.ConversationRole
import com.example.camply.messaging.repository.ConversationRepository
import com.example.camply.messaging.view.InboxView
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Exercises the reservation shortcut, message composer, and reopening the same thread from Inbox. */
class InboxViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sendsFromReservationAndReopensHistoryFromInbox() {
        val repository = FakeRepository()
        val view = InboxView(repository) { null }
        val reservationId = mutableStateOf<String?>("booking")
        compose.setContent {
            view.Display("owner", ConversationRole.OWNER, reservationId.value) { reservationId.value = null }
        }

        compose.onNodeWithText("Sam Rivera").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsNotEnabled()
        compose.onNodeWithText("Message").performTextInput("Check-in starts at 3 pm.")
        compose.onNodeWithText("Send").performClick()
        compose.onNodeWithText("Check-in starts at 3 pm.").assertIsDisplayed()
        compose.onNodeWithText("Back to reservations").performClick()
        compose.onNodeWithText("Inbox").assertIsDisplayed()
        compose.onNodeWithText("Sam Rivera").performClick()
        compose.onNodeWithText("Check-in starts at 3 pm.").assertIsDisplayed()
        compose.onNodeWithText("Back to Inbox").performClick()
        compose.onNodeWithText("Sam Rivera").assertIsDisplayed()
        assertEquals(listOf("booking"), repository.opened)
        assertEquals("owner", repository.messages.value.single().senderId)
    }

    @Test
    fun entrantOpensExistingOwnerThreadAndRepliesFromInbox() {
        val repository = FakeRepository(withOwnerMessage = true)
        val view = InboxView(repository) { "Taylor Morgan" }
        val reservationId = mutableStateOf<String?>("booking")
        compose.setContent {
            view.Display("entrant", ConversationRole.ENTRANT, reservationId.value) { reservationId.value = null }
        }

        compose.onNodeWithText("Taylor Morgan").assertIsDisplayed()
        compose.onNodeWithText("Check-in starts at 3 pm.").assertIsDisplayed()
        compose.onNodeWithText("Back to reservations").performClick()
        compose.onNodeWithText("Taylor Morgan").performClick()
        compose.onNodeWithText("Message").performTextInput("Thanks, see you then!")
        compose.onNodeWithText("Send").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithText("Thanks, see you then!").isDisplayed()
        }
        assertEquals(listOf("owner", "entrant"), repository.messages.value.map { it.senderId })
        assertEquals(listOf("booking"), repository.opened)
    }

    private class FakeRepository(withOwnerMessage: Boolean = false) : ConversationRepository {
        private val thread = Conversation(
            id = "booking", ownerId = "owner", entrantId = "entrant", campsiteName = "Banff Pines",
            entrantName = "Sam Rivera", stayFrom = "2027-07-01", stayTo = "2027-07-05"
        )
        private val inbox = MutableStateFlow(if (withOwnerMessage) listOf(thread) else emptyList())
        val messages = MutableStateFlow(
            if (withOwnerMessage) listOf(
                ConversationMessage("first", "owner", "Check-in starts at 3 pm.", Timestamp(1L, 0))
            ) else emptyList()
        )
        val opened = mutableListOf<String>()

        override fun observeConversations(userId: String, role: ConversationRole) = inbox

        override suspend fun openForReservation(
            reservationId: String, userId: String, role: ConversationRole
        ): Conversation {
            check(role.userId(thread) == userId && reservationId == thread.id)
            opened += reservationId
            inbox.value = listOf(thread)
            return thread
        }

        override fun observeMessages(conversation: Conversation, userId: String) = messages

        override suspend fun sendMessage(conversation: Conversation, senderId: String, text: String) {
            messages.value += ConversationMessage(id = "reply", senderId = senderId, text = text)
        }
    }
}

package com.example.camply.messaging

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.repository.MessageLogRepository
import com.example.camply.messaging.view.MessageLogView
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test

/** Covers navigating the read-only admin history by owner and reservation. */
class MessageLogViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun adminOpensReservationHistoryAndIdentifiesBothSenders() {
        val view = MessageLogView(FakeRepository()) { "Taylor Morgan" }
        compose.setContent { view.Display("admin") }

        compose.onNodeWithText("Taylor Morgan").performClick()
        compose.onNodeWithText("Banff Pines").assertIsDisplayed()
        compose.onNodeWithText("Sam Rivera").performClick()

        compose.onNodeWithText("Message history").assertIsDisplayed()
        compose.onNodeWithText("Taylor Morgan").assertIsDisplayed()
        compose.onNodeWithText("Sam Rivera").assertIsDisplayed()
        compose.onNodeWithText("Check-in starts at 3 pm.").assertIsDisplayed()
        compose.onNodeWithText("Thanks!").assertIsDisplayed()
        compose.onNodeWithText("Send").assertDoesNotExist()

        compose.onNodeWithText("Back to conversations").performClick()
        compose.onNodeWithText("Reservation ID: booking").assertIsDisplayed()
        compose.onNodeWithText("Back to owners").performClick()
        compose.onNodeWithText("Message logs").assertIsDisplayed()
    }

    private class FakeRepository : MessageLogRepository {
        override suspend fun loadLoggedConversations(adminId: String) = listOf(
            Conversation(
                id = "booking", ownerId = "owner", entrantId = "entrant", campsiteName = "Banff Pines",
                entrantName = "Sam Rivera", stayFrom = "2026-12-04", stayTo = "2026-12-06"
            )
        )

        override suspend fun loadMessageHistory(adminId: String, conversationId: String) = listOf(
            ConversationMessage("reply", "entrant", "Thanks!", Timestamp(2L, 0)),
            ConversationMessage("first", "owner", "Check-in starts at 3 pm.", Timestamp(1L, 0))
        )
    }
}

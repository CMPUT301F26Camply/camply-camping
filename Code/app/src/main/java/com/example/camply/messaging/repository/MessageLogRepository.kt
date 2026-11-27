package com.example.camply.messaging.repository

import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage

/** Read-only access to reservation conversations for authorized message-log admins. */
interface MessageLogRepository {
    /** Loads conversations containing at least one saved message. */
    suspend fun loadLoggedConversations(adminId: String): List<Conversation>

    /** Loads the full history of a single reservation conversation. */
    suspend fun loadMessageHistory(adminId: String, conversationId: String): List<ConversationMessage>
}

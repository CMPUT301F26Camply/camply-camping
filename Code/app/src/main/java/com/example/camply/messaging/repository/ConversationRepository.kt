package com.example.camply.messaging.repository

import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.ConversationRole
import kotlinx.coroutines.flow.Flow

/** Stores and observes reservation conversations and their message histories. */
interface ConversationRepository {
    /** Lists conversations belonging to the signed-in participant as they change. */
    fun observeConversations(userId: String, role: ConversationRole): Flow<List<Conversation>>

    /** Returns the existing conversation or creates it for a participant of the reservation. */
    suspend fun openForReservation(reservationId: String, userId: String, role: ConversationRole): Conversation

    /** Observes a participant's conversation history in chronological order. */
    fun observeMessages(conversation: Conversation, userId: String): Flow<List<ConversationMessage>>

    /** Saves a message from a participant and updates the conversation's activity. */
    suspend fun sendMessage(conversation: Conversation, senderId: String, text: String)
}

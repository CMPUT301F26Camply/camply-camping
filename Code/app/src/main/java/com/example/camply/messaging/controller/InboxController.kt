package com.example.camply.messaging.controller

import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.ConversationRole
import com.example.camply.messaging.model.InboxModel
import com.example.camply.messaging.repository.ConversationRepository
import kotlinx.coroutines.CancellationException

/** Loads participant conversations and handles reading and sending reservation messages. */
class InboxController(
    private val model: InboxModel,
    private val repository: ConversationRepository
) {
    /** Observes the participant's conversation list until the caller leaves the Inbox. */
    suspend fun observeInbox(userId: String, role: ConversationRole) = load("Could not load conversations.") {
        repository.observeConversations(userId, role).collect {
            model.conversations = it
            model.isLoading = false
        }
    }

    /** Opens the reservation's conversation and observes its history. */
    suspend fun openReservation(userId: String, reservationId: String, role: ConversationRole) =
        load("Could not open this reservation conversation.") {
            openConversation(repository.openForReservation(reservationId, userId, role), userId)
        }

    /** Selects [conversation] and observes its messages until the caller leaves the thread. */
    suspend fun observeConversation(conversation: Conversation, userId: String) =
        load("Could not load messages.") { openConversation(conversation, userId) }

    private suspend fun openConversation(conversation: Conversation, userId: String) {
        model.conversation = conversation
        repository.observeMessages(conversation, userId).collect {
            model.messages = it.sortedWith(
                compareBy<ConversationMessage> { it.sentAt == null }.thenBy { it.sentAt }.thenBy { it.id }
            )
            model.isLoading = false
        }
    }

    private suspend fun load(errorMessage: String, action: suspend () -> Unit) {
        model.isLoading = true
        model.loadError = null
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            model.isLoading = false
            model.loadError = errorMessage
        }
    }

    /** Updates the draft up to the allowed message length. */
    fun editDraft(text: String) {
        if (text.length <= ConversationMessage.MAX_LENGTH && !model.isSending) model.draft = text
    }

    /** Sends the current draft once and keeps its text available if saving fails. */
    suspend fun send(userId: String) {
        val conversation = model.conversation ?: return
        val body = model.draft.trim()
        if (body.isEmpty() || model.isSending) return
        model.isSending = true
        model.sendError = null
        try {
            repository.sendMessage(conversation, userId, body)
            model.draft = ""
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            model.sendError = "Could not send your message. Try again."
        } finally {
            model.isSending = false
        }
    }
}

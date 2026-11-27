package com.example.camply.messaging.controller

import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.MessageLogModel
import com.example.camply.messaging.model.MessageLogOwner
import com.example.camply.messaging.repository.MessageLogRepository
import com.example.camply.notification.repository.RecipientNameRepository
import kotlinx.coroutines.CancellationException

/** Groups saved conversations by owner and loads the selected thread for admin review. */
class MessageLogController(
    private val model: MessageLogModel,
    private val repository: MessageLogRepository,
    private val nameRepository: RecipientNameRepository
) {
    /** Loads the owner directory for saved message conversations. */
    suspend fun loadOwners(adminId: String) = load("Could not load message logs.") {
        model.owners = repository.loadLoggedConversations(adminId)
            .groupBy { it.ownerId }
            .map { (id, conversations) ->
                MessageLogOwner(id, ownerName(id), conversations.sortedByDescending { it.updatedAt })
            }
            .sortedWith(compareBy<MessageLogOwner> { it.name.lowercase() }.thenBy { it.id })
    }

    /** Loads and sorts all messages for the selected reservation. */
    suspend fun loadHistory(adminId: String, conversationId: String) = load("Could not load this history.") {
        model.messages = repository.loadMessageHistory(adminId, conversationId)
            .sortedWith(compareBy<ConversationMessage> { it.sentAt }.thenBy { it.id })
    }

    private suspend fun ownerName(id: String): String = try {
        nameRepository.getRecipientName(id)?.trim()?.takeIf { it.isNotEmpty() } ?: "Owner ($id)"
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        "Owner ($id)"
    }

    private suspend fun load(errorMessage: String, action: suspend () -> Unit) {
        model.isLoading = true
        model.error = null
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: SecurityException) {
            model.error = "Your account does not have permission to review message logs."
        } catch (_: Exception) {
            model.error = errorMessage
        } finally {
            model.isLoading = false
        }
    }
}

package com.example.camply.messaging.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.messaging.controller.MessageLogController
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.MessageLogModel
import com.example.camply.messaging.model.MessageLogOwner
import com.example.camply.messaging.repository.MessageLogRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import java.text.DateFormat

/** Lets admins browse campsite owners and read their reservation message histories. */
class MessageLogView(
    private val repository: MessageLogRepository,
    private val nameRepository: RecipientNameRepository
) {
    /** Displays the message logs available to [adminId]. */
    @Composable
    fun Display(adminId: String) {
        val directory = remember(adminId) { MessageLogModel() }
        val directoryController = remember(directory) { MessageLogController(directory, repository, nameRepository) }
        var selectedOwner by remember(adminId) { mutableStateOf<MessageLogOwner?>(null) }
        var selectedConversation by remember(adminId) { mutableStateOf<Conversation?>(null) }
        var directoryRetry by remember(adminId) { mutableIntStateOf(0) }
        val owner = selectedOwner
        val conversation = selectedConversation
        val history = remember(adminId, conversation?.id) { MessageLogModel() }
        val historyController = remember(history) { MessageLogController(history, repository, nameRepository) }
        var historyRetry by remember(history) { mutableIntStateOf(0) }

        LaunchedEffect(directoryController, directoryRetry) { directoryController.loadOwners(adminId) }
        LaunchedEffect(historyController, historyRetry) {
            if (conversation != null) historyController.loadHistory(adminId, conversation.id)
        }

        val back: () -> Unit = {
            if (conversation != null) selectedConversation = null else selectedOwner = null
        }
        BackHandler(enabled = owner != null, onBack = back)

        CamplyScreen(title = when {
            conversation != null -> "Message history"
            owner != null -> owner.name
            else -> "Message logs"
        }) {
            when {
                conversation != null && owner != null -> {
                    Text(owner.name + " and " + entrantName(conversation), style = MaterialTheme.typography.titleMedium)
                    ReservationSummary(conversation)
                    Spacer(Modifier.height(8.dp))
                    val error = history.error
                    when {
                        history.isLoading -> CircularProgressIndicator()
                        error != null -> CenteredMessage(error, "Try again") { historyRetry++ }
                        history.messages.isEmpty() -> CenteredMessage("No messages in this conversation.")
                        else -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history.messages, key = { it.id }) { message ->
                                OrangeCard {
                                    val sender = when (message.senderId) {
                                        conversation.ownerId -> owner.name
                                        conversation.entrantId -> entrantName(conversation)
                                        else -> "Unknown sender"
                                    }
                                    Text(sender, style = MaterialTheme.typography.titleMedium)
                                    message.sentAt?.let {
                                        Text(DateFormat.getDateTimeInstance().format(it.toDate()))
                                    }
                                    Text(message.text)
                                }
                            }
                        }
                    }
                }
                owner != null -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(owner.conversations, key = { it.id }) { entry ->
                        OrangeCard(Modifier.clickable { selectedConversation = entry }) {
                            Text(entrantName(entry), style = MaterialTheme.typography.titleMedium)
                            ReservationSummary(entry)
                        }
                    }
                }
                directory.isLoading -> CircularProgressIndicator()
                directory.error != null -> CenteredMessage(directory.error.orEmpty(), "Try again") { directoryRetry++ }
                directory.owners.isEmpty() -> CenteredMessage("No reservation messages have been sent.")
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(directory.owners, key = { it.id }) { entry ->
                        OrangeCard(Modifier.clickable { selectedOwner = entry }) {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (owner != null) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(if (conversation != null) "Back to conversations" else "Back to owners", back)
            }
        }
    }

    @Composable
    private fun ReservationSummary(conversation: Conversation) {
        Text(conversation.campsiteName.ifBlank { "Campsite unavailable" })
        Text("${conversation.stayFrom} to ${conversation.stayTo}")
        Text("Reservation ID: ${conversation.id}")
    }

    private fun entrantName(conversation: Conversation) =
        conversation.entrantName.ifBlank { "Entrant (${conversation.entrantId})" }
}

package com.example.camply.messaging.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.camply.messaging.controller.InboxController
import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationRole
import com.example.camply.messaging.model.InboxModel
import com.example.camply.messaging.repository.ConversationRepository
import com.example.camply.notification.repository.RecipientNameRepository
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.ErrorMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch

/** Shared Inbox and conversation screen for owners and entrants. */
class InboxView(
    private val repository: ConversationRepository,
    private val nameRepository: RecipientNameRepository
) {
    /** Displays the Inbox, or opens [reservationId] directly from reservation details. */
    @Composable
    fun Display(
        userId: String,
        role: ConversationRole,
        reservationId: String?,
        onReturnToReservation: () -> Unit
    ) {
        var selected by remember(userId, role, reservationId) { mutableStateOf<Conversation?>(null) }
        val isThread = selected != null || reservationId != null
        val model = remember(userId, role, reservationId, selected?.id) { InboxModel() }
        val controller = remember(model) { InboxController(model, repository, nameRepository) }
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        var retry by remember(model) { mutableIntStateOf(0) }
        val back: () -> Unit = {
            if (reservationId != null) onReturnToReservation() else selected = null
        }

        LaunchedEffect(controller, retry) {
            val selectedConversation = selected
            when {
                reservationId != null -> controller.openReservation(userId, reservationId, role)
                selectedConversation != null -> controller.observeConversation(selectedConversation, userId, role)
                else -> controller.observeInbox(userId, role)
            }
        }
        BackHandler(enabled = isThread) { if (!model.isSending) back() }

        CamplyScreen(
            title = if (isThread) "Conversation" else "Inbox",
            modifier = Modifier.imePadding()
        ) {
            val conversation = model.conversation
            val loadError = model.loadError
            when {
                model.isLoading -> CircularProgressIndicator()
                loadError != null -> CenteredMessage(loadError, "Try again") { retry++ }
                !isThread && model.conversations.isEmpty() ->
                    CenteredMessage("No conversations yet. Open a reservation to send a message.")
                !isThread -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(model.conversations, key = { it.id }) { entry ->
                        OrangeCard(Modifier.clickable { selected = entry }) {
                            Text(counterpartName(entry, role, model), style = MaterialTheme.typography.titleMedium)
                            Text(stayLabel(entry), color = CamplyPalette.Ink)
                        }
                    }
                }
                conversation != null -> {
                    val counterpart = counterpartName(conversation, role, model)
                    val counterpartId =
                        if (role == ConversationRole.OWNER) conversation.entrantId else conversation.ownerId
                    Text(counterpart, style = MaterialTheme.typography.titleMedium)
                    Text(stayLabel(conversation), color = CamplyPalette.Ink)
                    Spacer(Modifier.height(8.dp))
                    val listState = rememberLazyListState()
                    LaunchedEffect(model.messages.lastOrNull()?.id) {
                        if (model.messages.isNotEmpty()) listState.scrollToItem(model.messages.lastIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (model.messages.isEmpty()) item { CenteredMessage("No messages yet.") }
                        items(model.messages, key = { it.id }) { message ->
                            OrangeCard {
                                val sender = when (message.senderId) {
                                    userId -> "You"
                                    counterpartId -> counterpart
                                    else -> "Unknown sender"
                                }
                                CardLabel(sender)
                                Text(message.text, color = CamplyPalette.Ink)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    model.sendError?.let { ErrorMessage(it) }
                    OutlinedTextField(
                        value = model.draft,
                        onValueChange = controller::editDraft,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Message") },
                        enabled = !model.isSending,
                        maxLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = if (model.isSending) "Sending…" else "Send",
                        onClick = {
                            focusManager.clearFocus()
                            scope.launch { controller.send(userId) }
                        },
                        enabled = model.draft.isNotBlank() && !model.isSending
                    )
                }
            }
            if (isThread && !model.isSending) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(if (reservationId != null) "Back to reservations" else "Back to Inbox", back)
            }
        }
    }

    private fun counterpartName(conversation: Conversation, role: ConversationRole, model: InboxModel): String =
        when (role) {
            ConversationRole.OWNER -> conversation.entrantName.ifBlank { "Entrant" }
            ConversationRole.ENTRANT -> model.ownerNames[conversation.ownerId] ?: "Campsite owner"
        }

    private fun stayLabel(conversation: Conversation): String =
        "${conversation.campsiteName.ifBlank { "Campsite" }} · ${conversation.stayFrom} to ${conversation.stayTo}"
}

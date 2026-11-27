package com.example.camply.messaging.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A campsite owner and the owner's reservation conversations that contain messages. */
data class MessageLogOwner(val id: String, val name: String, val conversations: List<Conversation>)

/** State for the admin owner list and its read-only message history. */
class MessageLogModel {
    var owners by mutableStateOf(emptyList<MessageLogOwner>())
        internal set
    var messages by mutableStateOf(emptyList<ConversationMessage>())
        internal set
    var isLoading by mutableStateOf(true)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
}

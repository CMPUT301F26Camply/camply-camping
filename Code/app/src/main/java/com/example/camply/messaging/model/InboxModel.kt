package com.example.camply.messaging.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** State for the reservation Inbox and its currently selected conversation. */
class InboxModel {
    var conversations by mutableStateOf(emptyList<Conversation>())
        internal set
    var conversation by mutableStateOf<Conversation?>(null)
        internal set
    /** Cached owner names by user ID. A null value means the lookup returned no name. */
    var ownerNames by mutableStateOf(emptyMap<String, String?>())
        internal set
    var messages by mutableStateOf(emptyList<ConversationMessage>())
        internal set
    var draft by mutableStateOf("")
        internal set
    var isLoading by mutableStateOf(true)
        internal set
    var isSending by mutableStateOf(false)
        internal set
    var loadError by mutableStateOf<String?>(null)
        internal set
    var sendError by mutableStateOf<String?>(null)
        internal set
}

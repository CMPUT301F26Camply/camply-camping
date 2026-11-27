package com.example.camply.messaging.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** The role used to open or list reservation conversations. */
enum class ConversationRole(val firestoreField: String) {
    OWNER("ownerId"),
    ENTRANT("entrantId");

    /** Returns the ID of the conversation participant in this role. */
    fun userId(conversation: Conversation): String = when (this) {
        OWNER -> conversation.ownerId
        ENTRANT -> conversation.entrantId
    }
}

/** A reservation conversation. Its Firestore document ID is the reservation ID. */
data class Conversation(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val entrantId: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val entrantName: String = "",
    val stayFrom: String = "",
    val stayTo: String = "",
    val updatedAt: Timestamp? = null
)

/** A message in a reservation conversation, ordered by its server timestamp. */
data class ConversationMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val sentAt: Timestamp? = null
) {
    companion object {
        const val MAX_LENGTH = 2000
    }
}

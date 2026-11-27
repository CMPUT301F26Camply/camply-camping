package com.example.camply.messaging.repository

import com.example.camply.messaging.model.Conversation
import com.example.camply.messaging.model.ConversationMessage
import com.example.camply.messaging.model.ConversationRole
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore storage shared by reservation messaging entry points. */
class FirestoreConversationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ConversationRepository, MessageLogRepository {
    private val conversations = db.collection("conversations")

    override suspend fun loadLoggedConversations(adminId: String): List<Conversation> {
        requireLogAccess(adminId)
        val snapshots = conversations.get(Source.SERVER).await().documents
        return snapshots.chunked(10).flatMap { batch ->
            coroutineScope {
                batch.map { document ->
                    async {
                        // Older threads have no message count. Check for a message without reading history.
                        val messages = document.reference.collection("messages").limit(1).get(Source.SERVER).await()
                        if (messages.isEmpty) null else document.toObject(Conversation::class.java)
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }

    override suspend fun loadMessageHistory(adminId: String, conversationId: String): List<ConversationMessage> {
        requireLogAccess(adminId)
        require(conversationId.isNotBlank() && '/' !in conversationId)
        return conversations.document(conversationId).collection("messages")
            .orderBy("sentAt").get(Source.SERVER).await().toObjects(ConversationMessage::class.java)
    }

    private suspend fun requireLogAccess(adminId: String) {
        val user = auth.currentUser
        if (user == null || user.uid != adminId ||
            user.getIdToken(true).await().claims["messageLogAdmin"] != true
        ) {
            throw SecurityException("Message log access is restricted.")
        }
    }

    override fun observeConversations(userId: String, role: ConversationRole): Flow<List<Conversation>> =
        observe(conversations.whereEqualTo(role.firestoreField, userId), userId) { query ->
            query.toObjects(Conversation::class.java)
                .sortedWith(compareByDescending<Conversation> { it.updatedAt }.thenBy { it.id })
        }

    override suspend fun openForReservation(
        reservationId: String,
        userId: String,
        role: ConversationRole
    ): Conversation {
        requireSignedIn(userId)
        require(reservationId.isNotBlank() && '/' !in reservationId)
        val reference = conversations.document(reservationId)
        return db.runTransaction { transaction ->
            val existing = transaction.get(reference).toObject(Conversation::class.java)
            if (existing != null) {
                check(role.userId(existing) == userId) { "This conversation is unavailable." }
                return@runTransaction existing
            }

            val reservation = transaction.get(db.collection("reservations").document(reservationId))
                .toObject(Reservation::class.java)
            check(reservation != null && reservation.status != ReservationStatus.UNINITIALIZED &&
                reservation.campsiteId.isNotBlank() && reservation.entrantId.isNotBlank()) {
                "This reservation is unavailable."
            }
            val campsite = transaction.get(db.collection("campsites").document(reservation.campsiteId))
            val ownerId = campsite.getString("ownerId").orEmpty()
            check(ownerId.isNotBlank()) { "The campsite owner is unavailable." }
            check(userId == if (role == ConversationRole.OWNER) ownerId else reservation.entrantId) {
                "This reservation is unavailable."
            }
            val entrant = transaction.get(db.collection("users").document(reservation.entrantId))
            val conversation = Conversation(
                id = reservationId,
                ownerId = ownerId,
                entrantId = reservation.entrantId,
                campsiteId = reservation.campsiteId,
                campsiteName = campsite.getString("name").orEmpty(),
                entrantName = entrant.getString("name").orEmpty(),
                stayFrom = reservation.dates.from,
                stayTo = reservation.dates.to
            )
            transaction.set(reference, mapOf(
                "ownerId" to conversation.ownerId,
                "entrantId" to conversation.entrantId,
                "campsiteId" to conversation.campsiteId,
                "campsiteName" to conversation.campsiteName,
                "entrantName" to conversation.entrantName,
                "stayFrom" to conversation.stayFrom,
                "stayTo" to conversation.stayTo,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            conversation
        }.await()
    }

    override fun observeMessages(conversation: Conversation, userId: String): Flow<List<ConversationMessage>> =
        observe(conversations.document(conversation.id).collection("messages").orderBy("sentAt"), userId) { query ->
            query.toObjects(ConversationMessage::class.java)
        }

    override suspend fun sendMessage(conversation: Conversation, senderId: String, text: String) {
        requireSignedIn(senderId)
        require(senderId == conversation.ownerId || senderId == conversation.entrantId)
        val body = text.trim()
        require(body.isNotEmpty() && body.length <= ConversationMessage.MAX_LENGTH)
        val reference = conversations.document(conversation.id)
        val message = reference.collection("messages").document()
        db.runBatch { batch ->
            batch.set(message, mapOf(
                "senderId" to senderId,
                "text" to body,
                "sentAt" to FieldValue.serverTimestamp()
            ))
            batch.update(reference, "updatedAt", FieldValue.serverTimestamp())
        }.await()
    }

    private fun requireSignedIn(uid: String) {
        check(uid.isNotBlank() && auth.currentUser?.uid == uid) { "Sign in to view your conversations." }
    }

    private fun <T> observe(
        query: Query,
        uid: String,
        parse: (com.google.firebase.firestore.QuerySnapshot) -> List<T>
    ): Flow<List<T>> = callbackFlow {
        requireSignedIn(uid)
        val registration = query.addSnapshotListener { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> {
                    try {
                        trySend(parse(snapshot))
                    } catch (exception: Exception) {
                        close(exception)
                    }
                }
            }
        }
        awaitClose { registration.remove() }
    }
}

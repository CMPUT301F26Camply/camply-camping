package com.example.camply.user.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


interface ProfileRemovalRepository {
    /** Deletes one entrant's entry and lowers that lottery's waiting count when the entry was waiting. */
    suspend fun deleteLotteryEntry(lotteryId: String, entrantId: String)

    /** Deletes one reservation document. */
    suspend fun deleteReservation(reservationId: String)

    /** Deletes the user document. Called only after the owner's campsites or the entrant's active records are gone. */
    suspend fun deleteUser(userId: String)
}

/**
 * Firestore implementation of [ProfileRemovalRepository].
 *
 * @param db Firestore instance. Defaults to the app's shared instance.
 */
class FirestoreProfileRemovalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProfileRemovalRepository {
    override suspend fun deleteLotteryEntry(lotteryId: String, entrantId: String) {
        require(lotteryId.isNotBlank() && '/' !in lotteryId) { "Invalid lottery ID." }
        require(entrantId.isNotBlank() && '/' !in entrantId) { "Invalid entrant ID." }
        val lotteryRef = db.collection("lotteries").document(lotteryId)
        val entryRef = lotteryRef.collection("entries").document(entrantId)
        // One transaction so a failed count update cannot leave the entry already gone,
        // and a join that lands at the same time cannot have its count overwritten.
        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
            val entry = transaction.get(entryRef)
            if (!entry.exists()) return@runTransaction
            val waiting = entry.getString("status") == "waiting"
            transaction.delete(entryRef)
            if (!waiting || !lottery.exists()) return@runTransaction
            val count = lottery.getLong("waitlistCount") ?: return@runTransaction
            if (count > 0) transaction.update(lotteryRef, "waitlistCount", count - 1)
        }.await()
    }

    override suspend fun deleteReservation(reservationId: String) {
        require(reservationId.isNotBlank() && '/' !in reservationId) { "Invalid reservation ID." }
        db.collection("reservations").document(reservationId).delete().await()
    }

    override suspend fun deleteUser(userId: String) {
        require(userId.isNotBlank() && '/' !in userId) { "Invalid user ID." }
        db.collection("users").document(userId).delete().await()
    }
}
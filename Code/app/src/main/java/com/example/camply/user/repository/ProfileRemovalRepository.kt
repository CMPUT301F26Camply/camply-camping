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
        val entry = entryRef.get().await()
        if (!entry.exists()) return
        val wasWaiting = entry.getString("status") == "waiting"
        entryRef.delete().await()
        if (!wasWaiting) return
        val count = lotteryRef.get().await().getLong("waitlistCount") ?: return
        if (count > 0) lotteryRef.update("waitlistCount", count - 1).await()
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
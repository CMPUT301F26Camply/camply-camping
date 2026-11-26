package com.example.camply.lottery.repository

import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.WaitlistEntrant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Defines operations for reading an owner's lotteries and their waitlists (US 02.09.01).
 */
interface OwnerWaitlistRepository {
    /**
     * Retrieves the lotteries created by an owner.
     *
     * @param ownerId The owner's Firebase user ID.
     * @return The owner's lotteries.
     */
    suspend fun getOwnerLotteries(ownerId: String): List<Lottery>

    /**
     * Retrieves the entrants who joined a lottery.
     *
     * Names are not stored in lottery entries, so each entrant's name is null.
     *
     * @param lotteryId The lottery's document ID.
     * @return The entrants who joined the lottery.
     */
    suspend fun getEntrants(lotteryId: String): List<WaitlistEntrant>
}

/**
 * Reads an owner's lotteries and their waitlists from Cloud Firestore.
 *
 * @param db The Firestore database instance used to read lotteries.
 */
class FirestoreOwnerWaitlistRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : OwnerWaitlistRepository {
    private val lotteries = db.collection("lotteries")

    override suspend fun getOwnerLotteries(ownerId: String): List<Lottery> {
        require(ownerId.isNotBlank()) { "Invalid owner ID." }
        return lotteries.whereEqualTo("ownerId", ownerId).get().await()
            .toObjects(Lottery::class.java)
    }

    override suspend fun getEntrants(lotteryId: String): List<WaitlistEntrant> {
        require(lotteryId.isNotBlank() && '/' !in lotteryId) { "Invalid lottery ID." }
        // Leaving a waitlist deletes the entry, so every remaining entry is a joined entrant.
        return lotteries.document(lotteryId).collection("entries").get().await()
            .documents.map { entry ->
                WaitlistEntrant(
                    entrantId = entry.getString("entrantId") ?: entry.id,
                    name = null,
                    status = entry.getString("status") ?: "",
                    joinedAt = entry.getTimestamp("joinedAt")?.toDate()
                )
            }
    }
}

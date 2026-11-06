package com.example.camply.lottery.repository

import com.example.camply.lottery.model.LotteryEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Represents the outcome of declining a reservation opportunity (US 01.04.03).
 */
enum class DeclineResult {
    DECLINED,
    NOT_ELIGIBLE
}

/**
 * Defines operations for reading lottery entries and declining reservation opportunities.
 */
interface LotteryEntryRepository {
    /**
     * Retrieves an entrant's entry in a lottery.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     * @return The entry, or null if the entrant has no entry in the lottery.
     */
    suspend fun getEntry(lotteryId: String, entrantId: String): LotteryEntry?

    /**
     * Declines an entrant's reservation opportunity if it can still be declined.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     * @return Whether the opportunity was declined.
     */
    suspend fun declineOpportunity(lotteryId: String, entrantId: String): DeclineResult
}

/**
 * Reads and updates lottery entries in Cloud Firestore.
 *
 * @param db The Firestore database instance used to access lottery entries.
 */
class FirestoreLotteryEntryRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LotteryEntryRepository {

    override suspend fun getEntry(lotteryId: String, entrantId: String): LotteryEntry? =
        entryRef(lotteryId, entrantId).get().await().toObject(LotteryEntry::class.java)

    override suspend fun declineOpportunity(lotteryId: String, entrantId: String): DeclineResult {
        val entryRef = entryRef(lotteryId, entrantId)

        return db.runTransaction { transaction ->
            val entry = transaction.get(entryRef).toObject(LotteryEntry::class.java)

            if (entry == null || !entry.canDecline()) {
                DeclineResult.NOT_ELIGIBLE
            } else {
                // Only these fields change, so the original entry and draw result are preserved.
                transaction.update(
                    entryRef,
                    "opportunityStatus", LotteryEntry.OPPORTUNITY_DECLINED,
                    "declinedAt", FieldValue.serverTimestamp()
                )
                DeclineResult.DECLINED
            }
        }.await()
    }

    private fun entryRef(lotteryId: String, entrantId: String) =
        db.collection("lotteries").document(lotteryId).collection("entries").document(entrantId)
}

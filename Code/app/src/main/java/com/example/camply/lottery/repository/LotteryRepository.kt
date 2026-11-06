package com.example.camply.lottery.repository

import com.example.camply.lottery.model.Lottery
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Defines operations for retrieving campsite lotteries
 */
interface LotteryRepository {
    /**
     * Retrieves all lotteries from Firestore
     *
     * @return The list of lotteries
     */
    suspend fun getLotteries(): List<Lottery>

    /**
     * Retrieves a lottery using its document ID
     *
     * @param id The lottery's document ID
     * @return The lottery, or null if it does not exist
     */
    suspend fun getLottery(id: String): Lottery?
    /**
     * Writes one lottery document and returns its new id.
     *
     * @param lottery the listing to save. Its id may still be empty.
     * @return the Firestore document id assigned to the saved lottery
     */
    suspend fun create(lottery: Lottery): String

    /**
     * Retrieves lotteries that the specified entrant has joined.
     *
     * @param entrantId The entrant's Firebase user ID
     * @return The lotteries containing an entry for this entrant
     */
    suspend fun getJoinedLotteries(entrantId: String): List<Lottery>
}

/**
 * Retrieves campsite lottery information from Cloud Firestore
 *
 * @param db The Firestore database instance used to retrieve lotteries
 */
class FirestoreLotteryRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LotteryRepository {
    private val lotteries = db.collection("lotteries")

    override suspend fun getLotteries(): List<Lottery> =
        lotteries.get().await().toObjects(Lottery::class.java)

    override suspend fun getLottery(id: String): Lottery? =
        lotteries.document(id).get().await().toObject(Lottery::class.java)

    override suspend fun create(lottery: Lottery): String {
        val document = lotteries.document()
        document.set(lottery.copy(id = document.id)).await()
        return document.id
    }

    override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> {
        require(entrantId.isNotBlank() && !entrantId.contains('/')) {
            "Invalid entrant ID."
        }

        val joinedLotteries = mutableListOf<Lottery>()

        for (lottery in lotteries.get().await().documents) {
            val entry = lottery.reference
                .collection("entries")
                .document(entrantId).get().await()

            if (entry.exists()) {
                val joinedLottery = lottery.toObject(Lottery::class.java)

                if (joinedLottery != null) {
                    joinedLotteries.add(joinedLottery)
                }
            }
        }

        return joinedLotteries
    }
}
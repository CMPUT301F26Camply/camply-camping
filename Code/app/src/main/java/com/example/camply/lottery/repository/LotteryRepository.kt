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
}
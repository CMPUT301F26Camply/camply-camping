package com.example.camply.lottery.model
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface LotteryRepository {
    /**
     * Writes one lottery document and returns its new id.
     *
     * @param lottery the listing to save. Its id may still be empty.
     * @return the Firestore document id assigned to the saved lottery
     */
    suspend fun create(lottery: Lottery): String
}
/**
 * Firestore implementation of LotteryRepository.
 * Collection: "lotteries". The collection appears when the first document is written.
 *
 * @param db the Firestore database. Defaults to the app's Firebase project.
 */
class FirestoreLotteryRepository(db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LotteryRepository {

    private val lotteries = db.collection("lotteries")

    override suspend fun create(lottery: Lottery): String {
        val document = lotteries.document()
        document.set(lottery.copy(id = document.id)).await()
        return document.id
    }
}
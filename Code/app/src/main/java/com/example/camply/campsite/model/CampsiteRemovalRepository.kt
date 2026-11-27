package com.example.camply.campsite.model

import com.example.camply.lottery.model.Lottery
import com.example.camply.reservations.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface CampsiteRemovalRepository {
    /** Returns every lottery whose campsiteIds array contains [campsiteId]. */
    suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery>
    /** Returns the entrant ids stored on a lottery's entries subcollection. */
    suspend fun getLotteryEntrantIds(lotteryId: String): List<String>
    /** Removes [campsiteId] from a lottery that still has other campsites. */
    suspend fun removeCampsiteFromLottery(lotteryId: String, campsiteId: String)
    /** Deletes a lottery's entry documents, then the lottery document. */
    suspend fun deleteLottery(lotteryId: String)
    /** Returns every reservation stored for [campsiteId], including past ones. */
    suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation>
    /** Deletes one reservation document. */
    suspend fun deleteReservation(reservationId: String)
}
/**
 * Firestore implementation of [CampsiteRemovalRepository].
 *
 * @param db Firestore instance. Defaults to the app's shared instance.
 */
class FirestoreCampsiteRemovalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CampsiteRemovalRepository {

    override suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery> {
        require(campsiteId.isNotBlank()) { "Invalid campsite ID." }
        return db.collection("lotteries")
            .whereArrayContains("campsiteIds", campsiteId)
            .get().await()
            .toObjects(Lottery::class.java)
    }

    override suspend fun getLotteryEntrantIds(lotteryId: String): List<String> {
        require(lotteryId.isNotBlank() && '/' !in lotteryId) { "Invalid lottery ID." }
        return db.collection("lotteries").document(lotteryId)
            .collection("entries").get().await()
            .documents.map { it.getString("entrantId") ?: it.id }
            .filter { it.isNotBlank() }
    }

    override suspend fun removeCampsiteFromLottery(lotteryId: String, campsiteId: String) {
        val ref = db.collection("lotteries").document(lotteryId)
        val lottery = ref.get().await().toObject(Lottery::class.java) ?: return
        ref.update("campsiteIds", lottery.campsiteIds.filter { it != campsiteId }).await()
    }

    override suspend fun deleteLottery(lotteryId: String) {
        val ref = db.collection("lotteries").document(lotteryId)
        val entries = ref.collection("entries").get().await().documents
        for (entry in entries) {
            entry.reference.delete().await()
        }
        ref.delete().await()
    }

    override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> {
        require(campsiteId.isNotBlank()) { "Invalid campsite ID." }
        return db.collection("reservations")
            .whereEqualTo("campsiteId", campsiteId)
            .get().await()
            .toObjects(Reservation::class.java)
    }

    override suspend fun deleteReservation(reservationId: String) {
        require(reservationId.isNotBlank() && '/' !in reservationId) { "Invalid reservation ID." }
        db.collection("reservations").document(reservationId).delete().await()
    }
}
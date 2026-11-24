package com.example.camply.reservations.repository

import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Defines operations for retrieving campsite reservations.
 */
interface ReservationRepository {
    /**
     * Retrieves reservations belonging to an entrant.
     *
     * @param entrantId The entrant's user ID.
     * @return The entrant's reservations.
     */
    suspend fun getReservationsForEntrant(entrantId: String): List<Reservation>

    /**
     * Retrieves reservations associated with a campsite.
     *
     * @param campsiteId The campsite's ID.
     * @return The campsite's reservations.
     */
    suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation>

    /**
     * Saves a campsite reservation.
     *
     * @param reservation The reservation to save.
     * @return The ID of the new reservation document.
     */
    suspend fun createReservation(reservation: Reservation): String
}

/**
 * Retrieves reservation information from Cloud Firestore.
 */
class FirestoreReservationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReservationRepository {
    private val reservations = db.collection("reservations")

    override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
        reservations.whereEqualTo("entrantId", entrantId).get().await()
            .toObjects(Reservation::class.java)

    override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
        reservations.whereEqualTo("campsiteId", campsiteId).get().await()
            .toObjects(Reservation::class.java)


    /**
     * Atomically confirms a reservation and marks its lottery opportunity as booked.
     *
     * Both changes succeed together or neither is saved. This prevents concurrent
     * booking or decline requests from consuming the same lottery opportunity twice.
     *
     * @param reservation The confirmed reservation to create.
     * @return The new reservation document ID.
     */
    override suspend fun createReservation(reservation: Reservation): String {
        require(reservation.entrantId.isNotBlank() && '/' !in reservation.entrantId)
        require(reservation.lotteryId.isNotBlank() && '/' !in reservation.lotteryId)
        require(reservation.campsiteId.isNotBlank() && '/' !in reservation.campsiteId)
        require(reservation.status == ReservationStatus.CONFIRMED)

        val entryRef = db.collection("lotteries")
            .document(reservation.lotteryId)
            .collection("entries")
            .document(reservation.entrantId)

        val reservationRef = reservations.document()

        db.runTransaction { transaction ->
            val entry = transaction.get(entryRef).toObject(LotteryEntry::class.java)

            if (entry?.canConfirmReservation() != true) {
                throw IllegalStateException(
                    "This lottery opportunity has already been used or is no longer available."
                )
            }

            transaction.set(reservationRef, reservation)
            transaction.update(
                entryRef,
                "opportunityStatus",
                LotteryEntry.OPPORTUNITY_BOOKED
            )
        }.await()

        return reservationRef.id
    }

}

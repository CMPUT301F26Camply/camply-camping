package com.example.camply.reservations.repository

import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Defines operations for retrieving, creating, and rejecting campsite reservations.
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

    /**
     * Rejects an active reservation after checking its current state and campsite ownership.
     *
     * @param reservationId The reservation document ID.
     * @param ownerId The owner requesting rejection.
     * @param today The current date in YYYY-MM-DD format.
     * @return The saved reservation with Rejected status.
     * @throws IllegalArgumentException If the reservation is missing, inactive, or not owned by the caller.
     */
    suspend fun rejectReservation(reservationId: String, ownerId: String, today: String): Reservation
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

    override suspend fun rejectReservation(
        reservationId: String,
        ownerId: String,
        today: String
    ): Reservation {
        require(reservationId.isNotBlank() && ownerId.isNotBlank()) { "Missing reservation or owner." }
        val reference = reservations.document(reservationId)
        return db.runTransaction { transaction ->
            val reservation = transaction.get(reference).toObject(Reservation::class.java)
            requireNotNull(reservation) { "This reservation no longer exists." }
            require(reservation.campsiteId.isNotBlank()) { "The campsite is unavailable." }
            val campsite = transaction.get(db.collection("campsites").document(reservation.campsiteId))
            require(campsite.getString("ownerId") == ownerId) { "You do not own this campsite." }
            require(reservation.isActive(today)) { "Only active reservations can be rejected. Refresh and try again." }
            transaction.update(reference, "status", ReservationStatus.REJECTED.name)
            reservation.copy(id = reservationId, status = ReservationStatus.REJECTED)
        }.await()
    }
}

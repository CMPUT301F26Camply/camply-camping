package com.example.camply.reservations.repository

import com.example.camply.reservations.model.Reservation
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
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReservationRepository {
    private val reservations = db.collection("reservations")

    override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> =
        reservations.whereEqualTo("entrantId", entrantId).get().await()
            .toObjects(Reservation::class.java)

    override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> =
        reservations.whereEqualTo("campsiteId", campsiteId).get().await()
            .toObjects(Reservation::class.java)

    /**
     * Saves a reservation to Cloud Firestore.
     */
    override suspend fun createReservation(reservation: Reservation): String {
        val document = reservations.add(reservation).await()
        return document.id
    }
}

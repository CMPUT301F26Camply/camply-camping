package com.example.camply.reservations.model

import com.example.camply.campsite.model.DateRange
import com.google.firebase.firestore.DocumentId

/**
 * Represents the current status of a campsite reservation.
 */
enum class ReservationStatus {
    UNINITIALIZED,
    CONFIRMED,
    CANCELLED,
    REJECTED
}

/**
 * Represents a campsite reservation made by a lottery winner.
 *
 * Stores the entrant's ID, associated campsite and lottery IDs,
 * reserved dates, reservation status, and rules acknowledgement.
 */
data class Reservation(
    @DocumentId val id: String = "",
    val entrantId: String = "",
    val campsiteId: String = "",
    val lotteryId: String = "",
    val dates: DateRange = DateRange(),
    val status: ReservationStatus = ReservationStatus.UNINITIALIZED,
    val rulesAcknowledged: Boolean = false
) {
    /**
     * Checks if this reservation is already confirmed and its stay has not ended (US 01.06.01).
     *
     * @param today Today's date is an ISO string, like "2027-06-01".
     * @return True if the reservation is active on the given date.
     */
    fun isActive(today: String): Boolean =
        status == ReservationStatus.CONFIRMED && dates.to >= today

    /**
     * Checks if a confirmed reservation has already ended.
     *
     * @param today The current date in YYYY-MM-DD format.
     * @return True if the reservation has ended.
     */
    fun isPast(today: String): Boolean =
        status == ReservationStatus.CONFIRMED && dates.to < today
}

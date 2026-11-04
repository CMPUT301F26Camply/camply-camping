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
)

package com.example.camply.lottery.model

import com.google.firebase.firestore.DocumentId


/**
 * Represents the current stage of a lottery.
 */
enum class LotteryStatus {
    OPEN,
    DRAWN,
    BOOKING,
    CLOSED,
    UNINITIALIZED
}

/**
 * Represents a lottery created by a campsite owner.
 *
 * Stores the participating campsites, registration period,
 * booking period, number of winners, and current lottery status.
 */
data class Lottery(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: String = "",
    val registrationEnd: String = "",
    val bookingStart: String = "",
    val bookingEnd: String = "",
    val numberOfWinners: Int = 0,
    val status: LotteryStatus = LotteryStatus.UNINITIALIZED
)


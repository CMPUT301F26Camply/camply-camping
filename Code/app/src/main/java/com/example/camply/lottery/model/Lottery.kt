package com.example.camply.lottery.model

import com.google.firebase.firestore.DocumentId
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


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
 * One lottery created by a campsite owner.
 *
 * Stores the campsites, the stay window winners can reserve,
 * the registration period, the booking period, the winner count,
 * and the current status.
 * US 02.04.01, US 02.04.03, US 02.04.04, US 02.04.05.
 */
data class Lottery(
    @DocumentId val id: String = "",
    /** Firebase uid of the owner who created this lottery. */
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: String = "",
    val registrationEnd: String = "",
    val bookingStart: String = "",
    val bookingEnd: String = "",
    val lotteryStart: String = "",
    val lotteryEnd: String = "",
    val numberOfWinners: Int = 0,
    val status: LotteryStatus = LotteryStatus.UNINITIALIZED,
    val waitlistCount: Long = 0,
    val waitlistCapacity: Long? = null
) {
    /** ISO calendar dates, inclusive at both ends. Callers use a UTC date. */
    fun isRegistrationOpen(onDate: String): Boolean {
        if (status != LotteryStatus.OPEN) return false
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        for (date in listOf(registrationStart, registrationEnd, onDate)) {
            require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)) {
                "Registration dates must use YYYY-MM-DD."
            }
            // Strict parsing rejects impossible calendar dates.
            val parsed = format.parse(date)
            require(parsed != null && format.format(parsed) == date) { "Invalid registration date." }
        }
        // Valid, fixed-width ISO dates sort in calendar order.
        require(registrationStart <= registrationEnd) { "Invalid registration period." }
        return onDate in registrationStart..registrationEnd
    }
}


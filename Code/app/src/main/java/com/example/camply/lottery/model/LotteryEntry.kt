package com.example.camply.lottery.model

/**
 * Represents one entrant's entry in a lottery (US 01.04.03).
 *
 * The draw result stays in [status]. Declining will set [opportunityStatus], so the
 * original entry and its result are preserved.
 *
 * @property entrantId The entrant's Firebase user ID.
 * @property lotteryId The ID of the lottery the entry belongs to.
 * @property status The entry's draw status, such as "waiting" or "won".
 * @property opportunityStatus What the winner did with their opportunity, or null if it is unused.
 */
data class LotteryEntry(
    val entrantId: String = "",
    val lotteryId: String = "",
    val status: String = "",
    val opportunityStatus: String? = null
) {
    /**
     * Checks whether this entry's reservation opportunity can be declined.
     *
     * @return True if the entrant won and has not used or declined the opportunity.
     */
    fun canDecline(): Boolean = status == STATUS_WON && opportunityStatus == null

    /**
     * Checks whether this entry's reservation opportunity can be used to confirm a reservation.
     *
     * @return True if the entrant won and has not used or declined the opportunity.
     */
    fun canConfirmReservation(): Boolean = status == STATUS_WON && opportunityStatus == null

    companion object {
        const val STATUS_WON = "won"
        const val OPPORTUNITY_DECLINED = "declined"
    }
}

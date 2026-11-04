package com.example.camply.lottery.model
import com.example.camply.campsite.model.DateRange
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
/**
 * One lottery listing created by a campsite owner.
 *
 * Holds the campsite, the registration period, the booking period,
 * the stay dates winners may reserve, and how many winners to draw.
 *
 * @property id Firestore document id. Empty until the repository assigns it.
 * @property ownerId Firebase uid of the owner who created this lottery.
 * @property campsiteIds Campsites in this lottery. This story stores exactly one id for now.
 * @property registrationStart When entrants may start joining the waitlist.
 * @property registrationEnd When joining the waitlist closes.
 * @property bookingStart When a selected winner may start reserving.
 * @property bookingEnd Deadline for a selected winner to complete the reservation.
 * @property availableStayRanges Stay dates winners can reserve. Each range has from and to, as YYYY-MM-DD.
 * @property winnerCount How many entrants the draw may select. At least 1.
 * @property status "open" when created. Later stories change it to "drawn" or "closed".
 * @property waitlistCount How many entrants have joined. Starts at 0.
 * @property waitlistCapacity Optional maximum waitlist size. Null means no limit.
 * @property drawnAt When the draw finished. Null until the draw story runs.
 */
data class Lottery(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: Timestamp? = null,
    val registrationEnd: Timestamp? = null,
    val bookingStart: Timestamp? = null,
    val bookingEnd: Timestamp? = null,
    val availableStayRanges: List<DateRange> = emptyList(),
    val winnerCount: Int = 1,
    val status: String = "open",
    val waitlistCount: Int = 0,
    val waitlistCapacity: Int? = null,
    val drawnAt: Timestamp? = null
)
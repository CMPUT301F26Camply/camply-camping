package com.example.camply.lottery.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import kotlin.random.Random

/** Alberta local time, including daylight saving. US 02.05.01. */
val ALBERTA: ZoneId = ZoneId.of("America/Edmonton")

/**
 * Decides whether [lotteryEnd] has reached 23:59 Alberta time.
 *
 * [lotteryEnd] is the stay-window end, YYYY-MM-DD. The last minute of that
 * calendar day is the first moment a draw is allowed. An invalid date is not due.
 * US 02.05.01.
 */
fun isDrawDue(lotteryEnd: String, now: ZonedDateTime): Boolean {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(lotteryEnd)) return false
    val date = try {
        LocalDate.parse(lotteryEnd)
    } catch (e: DateTimeParseException) {
        return false
    }
    val deadline = date.atTime(23, 59).atZone(ALBERTA)
    return !now.withZoneSameInstant(ALBERTA).isBefore(deadline)
}

/**
 * Picks the winners from entrants who are still waiting.
 *
 * When fewer people joined than [winnerCount], every id is a winner.
 * A count below 1 selects nobody. The same [random] source makes tests repeatable.
 * US 02.05.01.
 *
 * @return the winning entrant ids. Every other waiting id is a loser.
 */
fun selectWinners(
    waitingIds: List<String>,
    winnerCount: Int,
    random: Random
): Set<String> {
    if (waitingIds.isEmpty() || winnerCount <= 0) return emptySet()
    if (winnerCount >= waitingIds.size) return waitingIds.toSet()
    return waitingIds.shuffled(random).take(winnerCount).toSet()
}
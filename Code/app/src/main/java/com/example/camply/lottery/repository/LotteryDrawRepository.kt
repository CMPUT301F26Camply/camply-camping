package com.example.camply.lottery.repository

import com.example.camply.lottery.model.LotteryStatus
import com.example.camply.lottery.model.isDrawDue
import com.example.camply.lottery.model.selectWinners
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * One lottery whose winner and loser ids are already stored.
 * Notification sending uses this and must not draw again.
 * US 02.05.01.
 */
data class RecordedDraw(
    val lotteryId: String,
    val ownerId: String,
    val winnerIds: List<String>,
    val loserIds: List<String>
)
interface LotteryDrawRepository {
    /**
     * Draws every still-open lottery whose deadline has passed, and returns
     * the saved results of those draws plus lotteries already marked DRAWN.
     * Already-drawn lotteries are included so a failed notification can be sent
     * again without changing who won.
     */
    suspend fun drawDueLotteries(now: ZonedDateTime): List<RecordedDraw>
    suspend fun markDrawn(lotteryId: String)
}
class FirestoreLotteryDrawRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val random: Random = Random.Default
) : LotteryDrawRepository {
    private val lotteries = db.collection("lotteries")
    override suspend fun drawDueLotteries(now: ZonedDateTime): List<RecordedDraw> {
        val open = lotteries.whereEqualTo("status", LotteryStatus.OPEN.name).get().await()
        val results = mutableListOf<RecordedDraw>()
        for (document in open.documents) {
            val draw = drawIfDue(document.reference, now)
            if (draw != null) {
                results.add(draw)
            }
        }
        return results
    }
    /**
     * Draws this lottery when it is still OPEN and lotteryEnd has reached
     * 23:59 Alberta time. Returns the people who won and lost.
     * Returns null when the lottery is missing, already drawn, or not due yet.
     */
    private suspend fun drawIfDue(
        lotteryRef: DocumentReference,
        now: ZonedDateTime
    ): RecordedDraw? {
        val lottery = lotteryRef.get().await()
        if (!lottery.exists()) {
            return null
        }

        val status = lottery.getString("status")
        val lotteryEnd = lottery.getString("lotteryEnd") ?: ""
        if (status != "OPEN") {
            return null
        }
        if (!isDrawDue(lotteryEnd, now)) {
            return null
        }

        val entries = lotteryRef.collection("entries").get().await()
        val waitingIds = mutableListOf<String>()
        for (entry in entries.documents) {
            if (entry.getString("status") == "waiting") {
                waitingIds.add(entrantIdOf(entry))
            }
        }

        val winnerCount = lottery.getLong("numberOfWinners")?.toInt() ?: 0
        val winnerIds = selectWinners(waitingIds, winnerCount, random)

        for (entry in entries.documents) {
            if (entry.getString("status") != "waiting") {
                continue
            }
            val entrantId = entrantIdOf(entry)
            val newStatus = if (entrantId in winnerIds) "won" else "lost"
            entry.reference.update("status", newStatus).await()
        }
        val loserIds = mutableListOf<String>()
        for (id in waitingIds) {
            if (id !in winnerIds) {
                loserIds.add(id)
            }
        }

        return RecordedDraw(
            lotteryId = lotteryRef.id,
            ownerId = lottery.getString("ownerId") ?: "",
            winnerIds = winnerIds.sorted(),
            loserIds = loserIds.sorted()
        )
    }
    /** Marks one lottery DRAWN. Call this only after its result messages are stored. */
    override suspend fun markDrawn(lotteryId: String) {
        lotteries.document(lotteryId).update("status", "DRAWN").await()
    }
    /** The entrant id saved on the entry, or the entry document id when that field is blank. */
    private fun entrantIdOf(entry: com.google.firebase.firestore.DocumentSnapshot): String {
        val savedId = entry.getString("entrantId")
        if (savedId.isNullOrBlank()) {
            return entry.id
        }
        return savedId
    }

}
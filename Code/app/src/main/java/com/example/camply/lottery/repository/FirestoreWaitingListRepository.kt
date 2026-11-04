package com.example.camply.lottery.repository

import com.example.camply.lottery.model.WaitingList
import com.example.camply.lottery.model.WaitingList.JoinResult
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Saves entries under lotteries/{lotteryId}/entries/{entrantId} (US 01.02.01).
 * Lottery fields: registrationStart, registrationEnd, status, waitlistCount,
 * and optional waitlistCapacity, using the agreed shared lottery schema.
 */
class FirestoreWaitingListRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val today: () -> String = {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }
) : WaitingListRepository {
    override fun leave(
        lotteryId: String,
        entrantId: String,
        onComplete: (WaitingList.LeaveResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isDocumentId(lotteryId) || !isDocumentId(entrantId)) {
            onFailure(IllegalArgumentException("Invalid lottery or entrant ID."))
            return
        }
        val lotteryRef = db.collection("lotteries").document(lotteryId)
        val entryRef = lotteryRef.collection("entries").document(entrantId)
        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
            val entry = transaction.get(entryRef)
            check(lottery.exists()) { "Lottery not found." }
            if (!entry.exists()) return@runTransaction WaitingList.LeaveResult.NOT_JOINED
            if (entry.getString("status") != "waiting") {
                return@runTransaction WaitingList.LeaveResult.ENTRY_FINALIZED
            }
            if (!isRegistrationOpen(lottery)) {
                return@runTransaction WaitingList.LeaveResult.REGISTRATION_CLOSED
            }
            val count = checkNotNull(lottery.getLong("waitlistCount")) { "Missing waitlist count." }
            check(count > 0) { "Invalid waitlist count." }
            transaction.delete(entryRef)
            transaction.update(lotteryRef, "waitlistCount", count - 1)
            WaitingList.LeaveResult.LEFT
        }.addOnSuccessListener { onComplete(it) }
            .addOnFailureListener { onFailure(it) }
    }

    override fun findForCampsite(
        campsiteId: String,
        entrantId: String,
        onComplete: (ActiveWaitingList?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // A single-field query avoids requiring a new composite index.
        db.collection("lotteries").whereArrayContains("campsiteIds", campsiteId).get()
            .addOnSuccessListener { result ->
                val active = try {
                    result.documents.filter { isRegistrationOpen(it) }
                        .also { check(it.size <= 1) { "Multiple active lotteries for this campsite." } }
                        .singleOrNull()
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }
                if (active == null) {
                    onComplete(null)
                } else {
                    active.reference.collection("entries").document(entrantId).get()
                        .addOnSuccessListener { entry ->
                            onComplete(ActiveWaitingList(active.id, entry.exists()))
                        }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun join(
        lotteryId: String,
        entrantId: String,
        onComplete: (WaitingList.JoinResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!isDocumentId(lotteryId) || !isDocumentId(entrantId)) {
            onFailure(IllegalArgumentException("A invalid lottery and entrant ID."))
            return
        }

        val lotteryRef = db.collection("lotteries").document(lotteryId)
        val entryRef = lotteryRef.collection("entries").document(entrantId)

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
            val entry = transaction.get(entryRef)
            check(lottery.exists()) { "Lottery not found." }

            // Preserve existing entries, including their recorded draw results.
            if (entry.exists()) return@runTransaction JoinResult.ALREADY_JOINED
            if (!isRegistrationOpen(lottery)) return@runTransaction JoinResult.REGISTRATION_CLOSED

            val count = checkNotNull(lottery.getLong("waitlistCount")) { "Missing waitlist count." }
            val capacity = lottery.getLong("waitlistCapacity")
            check(count in 0 until Long.MAX_VALUE) { "Invalid waitlist count." }
            check(capacity == null || capacity >= 0) { "Invalid capacity." }
            if (capacity != null && count >= capacity) {
                return@runTransaction JoinResult.FULL
            }

            transaction.set(entryRef, mapOf(
                "entrantId" to entrantId,
                "lotteryId" to lotteryId,
                "status" to "waiting",
                "joinedAt" to FieldValue.serverTimestamp()
            ))
            // Every join writes the parent, serializing capacity checks across entrants.
            transaction.update(lotteryRef, "waitlistCount", count + 1)
            JoinResult.JOINED
        }.addOnSuccessListener { result -> onComplete(result) }
            .addOnFailureListener { error -> onFailure(error) }
    }

    /** Uses the shared Lottery model's ISO-date and uppercase status representation. */
    private fun isRegistrationOpen(lottery: DocumentSnapshot): Boolean {
        val status = checkNotNull(lottery.getString("status")) { "Missing lottery status." }
        return Lottery(
            registrationStart = lottery.getString("registrationStart").orEmpty(),
            registrationEnd = lottery.getString("registrationEnd").orEmpty(),
            status = LotteryStatus.valueOf(status)
        ).isRegistrationOpen(today())
    }

    private fun isDocumentId(value: String): Boolean =
        value.isNotBlank() && '/' !in value && value != "." && value != ".."
}

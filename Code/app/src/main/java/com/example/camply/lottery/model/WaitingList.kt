package com.example.camply.lottery.model

/**
 * Membership rules for one lottery's waiting list (US 01.02.01).
 *
 * This model has no UI or Firestore dependency. A repository must load current
 * membership and apply these rules within the same transaction as the saved join.
 * A null capacity means that the lottery has no waitlist limit.
 */
class WaitingList(
    lotteryId: String,
    entrantIds: Set<String> = emptySet(),
    val capacity: Int? = null
) {
    private val members = entrantIds.toMutableSet()

    init {
        require(lotteryId.isNotBlank()) { "Lottery ID must not be blank." }
        require(entrantIds.none { it.isBlank() }) { "Entrant IDs must not be blank." }
        require(capacity == null || capacity >= 0) { "Capacity must not be negative." }
    }

    fun getEntrantIds(): Set<String> = members.toSet()

    fun contains(entrantId: String): Boolean = entrantId in members

    /**
     * Adds an entrant only when registration is open and space is available.
     * The caller supplies registration eligibility from the lottery's date rules.
     * JOINED describes this model change; it does not confirm a database save.
     */
    fun join(entrantId: String, registrationOpen: Boolean): JoinResult {
        require(entrantId.isNotBlank()) { "Entrant ID must not be blank." }
        if (contains(entrantId)) return JoinResult.ALREADY_JOINED
        if (!registrationOpen) return JoinResult.REGISTRATION_CLOSED
        if (capacity != null && members.size >= capacity) return JoinResult.FULL

        members.add(entrantId)
        return JoinResult.JOINED
    }

    enum class JoinResult {
        JOINED,
        ALREADY_JOINED,
        REGISTRATION_CLOSED,
        FULL
    }

    enum class LeaveResult {
        LEFT,
        NOT_JOINED,
        REGISTRATION_CLOSED,
        ENTRY_FINALIZED
    }
}

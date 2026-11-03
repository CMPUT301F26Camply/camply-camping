package com.example.camply.campsite.model

/**
 * Applied filters for browsing (US 01.01.02). A null field means "don't filter on this".
 */
data class CampsiteFilter(
    val location: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val guests: Int? = null
) {
    val isEmpty: Boolean
        get() = location == null && dateFrom == null && dateTo == null && guests == null

    fun matches(campsite: Campsite): Boolean {
        if (location != null && !campsite.location.equals(location, ignoreCase = true)) return false
        if (guests != null && campsite.maxOccupancy < guests) return false
        if (dateFrom != null && dateTo != null &&
            campsite.availability.none { it.covers(dateFrom, dateTo) }
        ) return false
        return true
    }
}

/**US 01.01.02: raw text typed into the filter panel.
 * Nothing is applied until the entrant presses Apply*/
data class FilterDraft(
    val location: String? = null,
    val dateFrom: String = "",
    val dateTo: String = "",
    val guests: String = ""
) {
    /** Returns a user-facing error message, or null if the draft is valid. */
    fun validate(): String? {
        val from = dateFrom.trim()
        val to = dateTo.trim()
        if (from.isEmpty() != to.isEmpty()) return "Enter both a start and an end date."
        if (from.isNotEmpty()) {
            if (!isIsoDate(from) || !isIsoDate(to)) return "Use the format YYYY-MM-DD, e.g. 2027-05-14."
            if (from > to) return "Start date must be before the end date."
        }
        val guestCount = guests.trim().toIntOrNull()
        if (guests.isNotBlank() && (guestCount == null || guestCount < 1)) return "Guests must be at least 1."
        return null
    }

    fun toFilter(): CampsiteFilter = CampsiteFilter(
        location = location,
        dateFrom = dateFrom.trim().ifEmpty { null },
        dateTo = dateTo.trim().ifEmpty { null },
        guests = guests.trim().toIntOrNull()
    )

    private fun isIsoDate(text: String): Boolean {
        val parts = text.split("-")
        if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false
        return parts[0].toIntOrNull() != null && month in 1..12 && day in 1..31
    }
}
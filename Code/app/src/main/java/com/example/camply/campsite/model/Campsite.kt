package com.example.camply.campsite.model

import com.google.firebase.firestore.DocumentId

/**
 * A block of dates an owner has listed as available.
 * Dates are ISO strings ("2027-05-01") so they compare correctly as plain text.
 */
data class DateRange(
    val from: String = "",
    val to: String = ""
) {
    fun covers(stayFrom: String, stayTo: String): Boolean = from <= stayFrom && to >= stayTo
}

/* Adding columns to the firebase */
data class Campsite(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val maxOccupancy: Int = 0,
    val price: Double = 0.0,
    val rating: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val rules: String = "",
    val cancellationPolicy: String = "",
    val ownerId: String = "",
    val availability: List<DateRange> = emptyList()
) {
    /** Implements US 01.01.01. Case-insensitive partial name match, so "ban" finds "Banff Pines". A blank query matches everything. */
    fun matchesName(query: String): Boolean =
        query.isBlank() || name.contains(query.trim(), ignoreCase = true)
}
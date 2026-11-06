package com.example.camply.campsite.model

import android.net.Uri
import com.google.firebase.firestore.DocumentId

/**
 * A block of dates an owner has listed as available.
 * Dates are ISO strings ("2027-05-01") so they compare correctly as plain text.
 */
data class DateRange(
    val from: String = "",
    val to: String = ""
) {
    /** US 01.01.02: true if this range fully contains the requested stay. */
    fun covers(stayFrom: String, stayTo: String): Boolean = from <= stayFrom && to >= stayTo
}

/**
 * A map position for a campsite (US 01.01.05 / 02.01.02).
 * Defaults are required so Firestore can build the object from a document.
 */
data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    /** True if the values are real-world latitude/longitude. */
    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/**
 *
 * US 01.01.05 / 02.01.02: name, location (park or area, e.g. "Banff"), description, amenities,
 * maximum occupancy, map location (latitude/longitude), images, price, tags, rating,
 * rules, cancellation policy. ownerId links the campsite to its owner (US 02.03.01).
 *
 * @param id Firestore document id
 * @param name the campsite name
 * @param location the campsite location e.g. "Banff"
 * @param description free-text description
 * @param amenities a list of campsite amenities
 * @param maxOccupancy is an Int to represent the max occupancy of that campsite
 * @param price price per night
 * @param rating average rating from entrants (not entered by the owner)
 * @param tags are a list of campsite tags (optional)
 * @param tags are a list of campsite tags (optional)
 * @param rules the campsite rules and policies
 * @param cancellationPolicy cancellation policy
 * @param ownerId uid of the owning campsite owner
 * @param availability date ranges the owner has made available
 * @param coordinates optional map location (latitude/longitude); null if the owner didn't set one
 */
class Campsite(
    @DocumentId val id: String = "",
    var name: String = "",
    var location: String = "",
    var description: String = "",
    var amenities: List<String> = emptyList(),
    var maxOccupancy: Int = 0,
    var price: Double = 0.0,
    var rating: Double = 0.0,
    var tags: List<String> = emptyList(),
    var rules: String = "",
    var cancellationPolicy: String = "",
    val ownerId: String = "",
    var availability: List<DateRange> = emptyList(),
    var images: List<String> = emptyList(),
    var coordinates: Coordinates? = null,
) {

    /** US 01.01.01: case-insensitive partial name match. A blank query matches everything. */
    fun matchesName(query: String): Boolean =
        query.isBlank() || name.contains(query.trim(), ignoreCase = true)

    /** US 02.01.01: returns a copy with the image added; blank addresses are ignored. */

    fun addImage(uri:String): Boolean{
        // "uri" reference: https://developer.android.com/reference/android/net/Uri
        if (uri.isBlank() || uri in images){
            return false
        }
        images = images + uri
        return true
    }

    fun removeImage(uri: String): Boolean {
        if(uri !in images) {
            return false
        }
        images = images - uri
        return true
    }
    /**
     * US 02.01.01 / 02.01.03: checks that the owner supplied the required details.
     *
     * @return a list of error messages; empty if the campsite is valid
     */
    fun validationCheck(): List<String> {
        val errorList = mutableListOf<String>()
        if (name.isBlank()){
            errorList.add("Please enter campsite name")
        }
        if (location.isBlank()){
            errorList.add("Please enter campsite location")
        }
        if (rules.isBlank()){
            errorList.add("Please enter rules and policies for this campsite")
        }
        if (maxOccupancy <= 0){
            errorList.add("Please enter a valid occupancy number")
        }
        if (price <= 0){
            errorList.add("Please enter a valid price")
        }
        coordinates?.let {
            if (!it.isValid()) {
                errorList.add("Please enter a valid latitude (-90 to 90) and longitude (-180 to 180)")
            }
        }
        return errorList
    }
    /** True if the owner has set a map location. */
    fun hasCoordinates(): Boolean = coordinates != null

    /** Sets or clears (null) the map location. Returns false if the values are out of range. */
    fun updateCoordinates(newCoordinates: Coordinates?): Boolean {
        if (newCoordinates != null && !newCoordinates.isValid()) {
            return false
        }
        coordinates = newCoordinates
        return true
    }
}
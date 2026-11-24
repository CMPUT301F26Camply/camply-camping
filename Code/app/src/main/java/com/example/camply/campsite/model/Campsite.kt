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
    /** US 01.01.02: true if this range fully contains the requested stay. */
    fun covers(stayFrom: String, stayTo: String): Boolean = from <= stayFrom && to >= stayTo
}
//data class location(){}
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
 * @param rules the campsite rules and policies
 * @param cancellationPolicy cancellation policy
 * @param ownerId uid of the owning campsite owner
 * @param availability date ranges the owner has made available
 * @param images ordered list of Firestore photo document IDs (maximum 5). The first ID represents
 * the campsite's cover photo. Each ID references a full image in the photos subcollection and a
 * smaller display image in the previews subcollection.
 * @param latitude Latitude of the campsite map pin, if selected
 * @param longitude Longitude of the campsite map pin, if selected
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
    var latitude: Double? = null,
    var longitude: Double? = null
) {
    companion object {
        // Maximum number of promotional photos allowed per campsite
        const val MAX_IMAGES = 5
    }

    /** US 01.01.01: case-insensitive partial name match. A blank query matches everything. */
    fun matchesName(query: String): Boolean =
        query.isBlank() || name.contains(query.trim(), ignoreCase = true)

    /**
     * US 02.01.02: Adds a promotional photo ID to the campsite.
     *
     * Photo IDs reference documents in the campsite's photos and
     * previews subcollections in Firestore.
     *
     * Rejects blank IDs, duplicate IDs, and additions exceeding
     * the maximum of five photos.
     *
     * This method only updates the local list of photo IDs.
     * Uploading photo data is handled separately by the repository.
     *
     * @param photoId Firestore document ID of the photo to add
     * @return true if the photo ID was added, false otherwise
     */
    fun addImage(photoId: String): Boolean {
        if (photoId.isBlank() || photoId in images || images.size >= MAX_IMAGES) {
            return false
        }

        images = images + photoId
        return true
    }

    /**
     * US 02.01.03: Removes a promotional photo ID from the campsite.
     *
     * Only removes the ID from the local list. Deleting the corresponding Firestore
     * photo and preview documents is handled separately by the repository.
     *
     * @param photoId Firestore document ID of the photo to remove
     * @return true if the photo ID was removed, false if not found
     */
    fun removeImage(photoId: String): Boolean {
        if (photoId !in images) {
            return false
        }

        images = images - photoId
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

        if (images.size > MAX_IMAGES) {
            errorList.add("A campsite can have at most $MAX_IMAGES photos")
        }

        if (images.any { it.isBlank() } || images.size != images.distinct().size) {
            errorList.add("Campsite photos must have a unique, non blank IDs")
        }

        if ((latitude == null) != (longitude == null)) {
            errorList.add("Campsite latitude and longitude must both be provided")
        }

        latitude?.let {
            if (it !in -90.0..90.0) {
                errorList.add("Please enter a valid latitude")
            }
        }

        longitude?.let {
            if (it !in -180.0..180.0) {
                errorList.add("Please enter a valid longitude")
            }
        }

        if (description.isBlank()) {
            errorList.add("Please enter campsite description")
        }

        if (cancellationPolicy.isBlank()) {
            errorList.add("Please enter campsite cancellation policy")
        }

        if (latitude == null && longitude == null) {
            errorList.add("Please select a campsite location on the map")
        }

        return errorList
    }
}
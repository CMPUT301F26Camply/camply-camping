package com.example.camply.user.model

import com.example.camply.campsite.model.Campsite

/**
 * Represents a Camply user registered as a campsite owner.
 *
 * Inherits shared user information from User and implements
 * the CampsiteOwner interface for owner related functionality.
 *
 * @param uid the user's Firebase Authentication UID
 * @param profile the user's personal profile information
 */
class CampsiteOwnerUser(
    uid: String,
    profile: Profile
) : User(uid, profile), CampsiteOwner {

    /**
     * Returns the campsite owner account role.
     *
     * @return "owner"
     */
    override fun getRole(): String {
        return "owner"
    }

    private val _campsites = mutableListOf<Campsite>()

    val campsites: List<Campsite>
        get() = _campsites

    fun loadExistingCampsite(loadedCampsites: List<Campsite>) {
        _campsites.clear()
        _campsites.addAll(loadedCampsites)
    }

    override fun registerCampsite(campsite: Campsite): Boolean {
        if (campsite.validationCheck().isNotEmpty()) {
            return false
        }
        _campsites.add(campsite)
        return true
    }

    override fun removeCampsite(campsite: Campsite) {
        _campsites.remove(campsite)
    }

    override fun updateExistingCampsite(campsite: Campsite, edit: Campsite): Boolean {
        // if there is no existing campsites for this owner
        if (!_campsites.contains(campsite)) {
            return false
        }
        if (edit.validationCheck().isNotEmpty()) {
            return false
        }
        campsite.name = edit.name
        campsite.location = edit.location
        campsite.description = edit.description
        campsite.amenities = edit.amenities
        campsite.cancellationPolicy = edit.cancellationPolicy
        campsite.price = edit.price
        campsite.rules = edit.rules
        campsite.maxOccupancy = edit.maxOccupancy
        campsite.tags = edit.tags
        campsite.images = edit.images
        campsite.availability = edit.availability
        campsite.coordinates = edit.coordinates

        return true
    }
}

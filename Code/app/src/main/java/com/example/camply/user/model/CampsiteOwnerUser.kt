package com.example.camply.user.model

import com.google.firebase.firestore.DocumentId
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

    override fun registerCampsite(campsite: Campsite): Boolean {
        if (campsite.validationCheck().isNotEmpty()){
            return false
        }
        _campsites.add(campsite)
        return true
    }

    override fun removeCampsite(campsite: Campsite){
        _campsites.remove(campsite)
    }

    override fun updateExistingCampsite(campsite: Campsite, edit: Campsite): Boolean {
        if (edit.validationCheck().isNotEmpty()){
            return false
        }
        campsite.name = edit.name
        campsite.location = edit.location
        campsite.amenities = edit.amenities
        campsite.rules = edit.rules
        campsite.maxOccupancy = edit.maxOccupancy
        campsite.tags = edit.tags
        return true
    }

}


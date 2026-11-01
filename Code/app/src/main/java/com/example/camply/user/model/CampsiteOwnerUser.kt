package com.example.camply.user.model

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

    override fun registerCampsite(campsite: Campsite): Boolean{
        if (campsite.validationCheck().isNotEmpty()){
            return false
        }
        _campsites.add(campsite)
        return true
    }

    override fun removeCampsite(campsite: Campsite){
        _campsites.remove(campsite)
    }

    override fun updateExistingCampsite(campsite: Campsite, edit: Campsite): Boolean{
        /*validation check to see if the new information is valid*/
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

/**
* This class represents a campsite
 * @param name the campsite name
 * @param location the campsite location
 * @param amenities a list of campsite amenities
 * @param rules the campsite rules and policies
 * @param maxOccupancy is an Int to represent the max occupancy of that campsite
 * @param tags are a list of campsite tags
 * @param images is a list of campsite images
* */
class Campsite(
    var name: String,
    var location: String,
    var amenities: List<String>,
    var maxOccupancy: Int,
    var rules: String,
    var tags: List<String> = emptyList(), /*tags are optional*/
    // The following "images" implementation is from Anthropic, Claude, "How to have a function that has the option to add multiple images in kotlin", 2026-10-31
    images: List<String> = emptyList() /*images are optional as well*/
) {
    private val _images = mutableListOf<String>()
    init {
        for (uri in images){
            addImage(uri)
        }
    }
    val images: List<String>
        get() = _images
    fun addImage(uri:String): Boolean{
        // "uri" reference: https://developer.android.com/reference/android/net/Uri
        //
        if (uri.isBlank()){
            return false
        }
        _images.add(uri)
        return true
    }

    fun removeImage(uri: String) {
        _images.remove(uri)
    }


    /*need to ensure that the necessary details are provided by the campsite owner*/
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
        return errorList
    }
}
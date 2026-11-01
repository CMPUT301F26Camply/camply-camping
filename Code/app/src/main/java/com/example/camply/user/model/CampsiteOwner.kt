package com.example.camply.user.model

/**
 * Represents the campsite owner role in Camply.
 *
 * Classes implementing this interface can perform owner related
 * actions, such as registering campsites and managing lotteries.
 */
interface CampsiteOwner {
    fun registerCampsite(campsite: Campsite): Boolean
    fun removeCampsite(campsite: Campsite)
    fun updateExistingCampsite(campsite: Campsite, edit: Campsite): Boolean
}
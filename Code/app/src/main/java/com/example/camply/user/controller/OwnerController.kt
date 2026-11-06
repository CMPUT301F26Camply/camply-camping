package com.example.camply.user.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.user.model.CampsiteOwnerUser
import kotlinx.coroutines.CancellationException

/**
 * This class acts as a controller
 * for campsite owner's actions and forwards them to the campsite owner model
 *
 * @param campsiteOwner representing the current owner
 * @param repository representing the current owner's campsite repository
 */
class OwnerController(
    private val campsiteOwner: CampsiteOwnerUser,
    private val repository: CampsiteRepository
) {
    var campsites by mutableStateOf<List<Campsite>>(emptyList())
    var errors by mutableStateOf<List<String>>(emptyList())
    var loadingCheck by mutableStateOf(false)

    // load campsites for the owner from the firebase db
    suspend fun loadCampsite() {
        if(campsites.isEmpty()){
            loadingCheck = true
        }
        try {
            campsiteOwner.loadExistingCampsite(repository.getCampsitesByOwner(campsiteOwner.getUid()))
            campsites = campsiteOwner.campsites.toList()
            errors = emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors = listOf(e.message ?: "Could not load campsites")
        }
        finally {
            loadingCheck = false
        }
    }

    suspend fun registerCampsite(
        name: String,
        location: String,
        description: String,
        amenities: String,
        maxOccupancy: String,
        price: String,
        tags: String,
        rules: String,
        cancellationPolicy: String,
        images: List<String> = emptyList(),
        availability: List<DateRange> = emptyList(),
    ): Boolean {
        val campsite = buildCampsiteRecord(
            id = "",
            name = name,
            location = location,
            description = description,
            amenities = amenities,
            maxOccupancy = maxOccupancy,
            price = price,
            tags = tags,
            rules = rules,
            cancellationPolicy = cancellationPolicy,
            images = images,
            availability = availability,
        )

        val problems = campsite.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        val newId: String
        try {
            newId = repository.saveCampsite(campsite) // get the id of the new campsite from firestore
        } catch (e: CancellationException){
            throw e
        } catch (e: Exception) {
            errors = listOf(e.message ?: "Could not save campsite")
            return false
        }

        val newCampsite = buildCampsiteRecord(
            id = newId,
            name = name,
            location = location,
            description = description,
            amenities = amenities,
            maxOccupancy = maxOccupancy,
            price = price,
            tags = tags,
            rules = rules,
            cancellationPolicy = cancellationPolicy,
            images = images,
            availability = availability,
        )

        if(!campsiteOwner.registerCampsite(newCampsite)){
            errors = listOf("Could not add the new campsite to list")
            return false
        }
        errors = emptyList()
        campsites = campsiteOwner.campsites.toList()
        return true
    }

    suspend fun updateCampsite(
        existingSite: Campsite,
        name: String,
        location: String,
        description: String,
        amenities: String,
        maxOccupancy: String,
        price: String,
        tags: String,
        rules: String,
        cancellationPolicy: String,
        images: List<String> = existingSite.images,
        availability: List<DateRange> = existingSite.availability,
    ): Boolean {
        val edit = buildCampsiteRecord(
            id = existingSite.id,
            name = name,
            location = location,
            description = description,
            amenities = amenities,
            maxOccupancy = maxOccupancy,
            price = price,
            tags = tags,
            rules = rules,
            cancellationPolicy = cancellationPolicy,
            images = images,
            availability = availability,
        )

        val problems = edit.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        try {
            repository.saveCampsite(edit)
        } catch (e: CancellationException){
            throw e
        } catch (e: Exception){
            errors = listOf(e.message ?: "Could not save changes to campsite")
            return false
        }

        // copies the edited values into existingSite
        if (!campsiteOwner.updateExistingCampsite(existingSite, edit)) {
            errors = listOf("Campsite not found")
            return false
        }

        loadCampsite()
        return true
    }

    suspend fun removeCampsite(campsite: Campsite) {
        try {
            repository.deleteCampsite(campsite.id)
        } catch (e: CancellationException){
            throw e
        } catch (e: Exception){
            errors= listOf(e.message ?: "Could not delete campsite.")
            return
        }

        campsiteOwner.removeCampsite(campsite)
        campsites = campsiteOwner.campsites.toList()
    }

    fun removeErrorsFromList() {
        errors = emptyList()
    }

    private fun buildCampsiteRecord(
        id: String,
        name: String,
        location: String,
        description: String,
        amenities: String,
        maxOccupancy: String,
        price: String,
        tags: String,
        rules: String,
        cancellationPolicy: String,
        images: List<String>,
        availability: List<DateRange>,
    ): Campsite {
        // The following function is from Anthropic, Claude, "How do I ensure that anything not valid turns into 0 so that my validation check can catch it?", 2026-11-05
        val occupancy = maxOccupancy.trim().toIntOrNull() ?: 0
        val priceForSite = price.trim().toDoubleOrNull() ?: 0.0
        //.map functionality: https://kotlinlang.org/docs/collection-transformations.html#map
        //*
        //   Author: Cristian https://stackoverflow.com/users/244296/cristian
        //   Title: "Removing spaces from string"
        //   Answer: https://stackoverflow.com/questions/6932163/removing-spaces-from-string
        //   Date: 2011-07-03
        //   License: CC-BY-SA 4.0 (International)
        //*/
        val amenityList = amenities.split(",").map {
            it.trim()
        }.filter { it.isNotEmpty() }

        val tagList = tags.split(",").map {
            it.trim()
        }.filter { it.isNotEmpty() }

        val campsite = Campsite(
            id = id,
            name = name,
            location = location,
            description = description,
            amenities = amenityList,
            maxOccupancy = occupancy,
            price = priceForSite,
            tags = tagList,
            rules = rules,
            cancellationPolicy = cancellationPolicy,
            ownerId = campsiteOwner.getUid(),
            images = images,
            availability = availability,
        )
        return campsite
    }
}
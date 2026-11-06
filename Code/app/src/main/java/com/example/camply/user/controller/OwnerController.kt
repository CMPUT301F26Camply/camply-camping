package com.example.camply.user.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.user.model.CampsiteOwnerUser

/**
 * Handles campsite owner's actions and forwards them to the campsite owner model
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

    // load campsites for the owner from the firebase db
    suspend fun loadCampsite() {
        campsiteOwner.loadExistingCampsite(repository.getCampsitesByOwner(campsiteOwner.getUid()))
        campsites = campsiteOwner.campsites.toList()
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
        )

        val problems = campsite.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        repository.saveCampsite(campsite)
        errors = emptyList()
        loadCampsite()
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
        images: List<String> = emptyList(),
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
        )

        val problems = edit.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        // copies the edited values into existingSite
        if (!campsiteOwner.updateExistingCampsite(existingSite, edit)) {
            errors = listOf("Campsite not found")
            return false
        }

        repository.saveCampsite(existingSite)
        errors = emptyList()
        campsites = campsiteOwner.campsites.toList()
        return true
    }

    suspend fun removeCampsite(campsite: Campsite) {
        repository.deleteCampsite(campsite.id)
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
    ): Campsite {
        
        val occupancy = maxOccupancy.trim().toIntOrNull()
        val priceValue = price.trim().toDoubleOrNull()
        //.map functionality: https://kotlinlang.org/docs/collection-transformations.html#map

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
            price = priceValue,
            tags = tagList,
            rules = rules,
            cancellationPolicy = cancellationPolicy,
            ownerId = campsiteOwner.getUid(),
            images = images,
        )

        return campsite
    }
}

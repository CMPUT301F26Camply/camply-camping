package com.example.camply.user.controller

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.service.CampsiteImageService
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.user.model.CampsiteOwnerUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val imageService = CampsiteImageService()

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
        photosToSave: Map<String, Uri> = emptyMap(),
        context: Context? = null,
        latitude: Double? = null,
        longitude: Double? = null
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
            latitude = latitude,
            longitude = longitude
        )

        val problems = campsite.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        val newId: String
        try {
            val preparedPhotos = preparePhotos(context, photosToSave)
            newId = repository.saveCampsite(campsite, preparedPhotos)
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
            latitude = latitude,
            longitude = longitude
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
        photosToSave: Map<String, Uri> = emptyMap(),
        context: Context? = null,
        latitude: Double? = existingSite.latitude,
        longitude: Double? = existingSite.longitude
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
            latitude = latitude,
            longitude = longitude
        )

        val problems = edit.validationCheck()
        if (problems.isNotEmpty()) {
            errors = problems
            return false
        }

        // Find existing photos that are no longer in the edited campsite.
        val photosToRemove = existingSite.images.filter { it !in images }

        try {
            val preparedPhotos = preparePhotos(context, photosToSave)
            repository.saveCampsite(edit, preparedPhotos, photosToRemove)
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


    /**
     * US 02.01.02 / 02.01.03: Prepares new or replacement campsite photos.
     *
     * Converts selected image URIs into full and display JPEG versions
     * using CampsiteImageService. Existing unchanged photos are not processed.
     *
     * All images must be processed successfully before any Firestore
     * changes are made. Processing runs off the main thread.
     *
     * @param context Android context for reading selected images
     * @param photosToSave selected image URIs mapped by photo ID
     * @return prepared JPEG images mapped by photo ID
     * @throws IllegalArgumentException if an image cannot be processed
     */
    private suspend fun preparePhotos(
        context: Context?,
        photosToSave: Map<String, Uri>
    ): Map<String, PreparedCampsiteImages> {
        if (photosToSave.isEmpty()) {
            return emptyMap()
        }

        val imageContext = requireNotNull(context) {"Image context is required to process campsite photos"}

        return withContext(Dispatchers.IO) { photosToSave.mapValues { (_, uri) ->
                imageService.prepareImage(imageContext, uri)
                    ?: throw IllegalArgumentException("Could not process one of the selected campsite photos")
            }
        }
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
        latitude: Double?,
        longitude: Double?
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
            latitude = latitude,
            longitude = longitude
        )
        return campsite
    }
}
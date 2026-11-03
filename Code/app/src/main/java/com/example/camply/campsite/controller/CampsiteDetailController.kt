package com.example.camply.campsite.controller

import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import kotlin.coroutines.cancellation.CancellationException

/** Loads a single campsite for the details screen (US 01.01.05). */
class CampsiteDetailController(
    private val model: CampsiteDetailModel,
    private val repository: CampsiteRepository
) {
    suspend fun loadCampsite(campsiteId: String) {
        model.showLoading()
        try {
            val campsite = repository.getCampsite(campsiteId)
            if (campsite != null) model.showCampsite(campsite) else model.showError("Campsite not found.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load campsite.")
        }
    }
}
package com.example.camply.campsite.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** State for the campsite details screen (US 01.01.05). */
class CampsiteDetailModel {

    var campsite: Campsite? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(true)
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set

    fun showLoading() { campsite = null; isLoading = true; errorMessage = null }
    fun showCampsite(value: Campsite) { campsite = value; isLoading = false; errorMessage = null }
    fun showError(message: String) { campsite = null; isLoading = false; errorMessage = message }
}
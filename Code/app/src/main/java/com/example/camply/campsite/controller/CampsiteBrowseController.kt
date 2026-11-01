package com.example.camply.campsite.controller

import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.campsite.model.CampsiteFilter
import com.example.camply.campsite.model.CampsiteRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles user actions on the Explore campsites screen (US 01.01.01 - 01.01.04)
 * and updates [CampsiteBrowseModel]. Contains no UI code.
 */
class CampsiteBrowseController(
    private val model: CampsiteBrowseModel,
    private val repository: CampsiteRepository
) {

    /** Loads campsites. Only shows the spinner on first load so returning to the screen doesn't flash. */
    suspend fun loadCampsites() {
        if (model.campsites.isEmpty()) model.showLoading()
        try {
            model.showCampsites(repository.getCampsites())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load campsites.")
        }
    }
    // US 01.01.01: called on every keystroke in the search field.
    fun onSearchQueryChanged(query: String) = model.updateSearchQuery(query)

    //US 01.01.02: updates the draft filter
    fun onFilterButtonClicked() = model.changeFilterPanelVisibility(!model.isFilterPanelVisible)

    fun onLocationSelected(location: String?) =
        model.updateFilterDraft(model.filterDraft.copy(location = location))

    fun onDateFromChanged(text: String) = model.updateFilterDraft(model.filterDraft.copy(dateFrom = text))

    fun onDateToChanged(text: String) = model.updateFilterDraft(model.filterDraft.copy(dateTo = text))

    fun onGuestsChanged(text: String) =
        model.updateFilterDraft(model.filterDraft.copy(guests = text.filter(Char::isDigit)))

    /** Validates the draft; applies it and closes the panel, or shows an error. */
    fun onApplyFilter() {
        val error = model.filterDraft.validate()
        if (error != null) {
            model.updateFilterError(error)
            return
        }
        model.updateAppliedFilter(model.filterDraft.toFilter())
        model.changeFilterPanelVisibility(false)
    }

    fun onClearFilters() {
        model.updateFilterDraft(model.filterDraft.copy(location = null, dateFrom = "", dateTo = "", guests = ""))
        model.updateAppliedFilter(CampsiteFilter())
    }
}
package com.example.camply.campsite.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State for the Explore campsites screen. Holds data only; the controller changes it,
 * the view reads it. Compose state means the view redraws automatically.
 */
class CampsiteBrowseModel {

    var campsites: List<Campsite> by mutableStateOf(emptyList())
        private set
    var isLoading: Boolean by mutableStateOf(true)
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set


    //US 01.01.01: the text currently typed in the search field.
    var searchQuery: String by mutableStateOf("")
        private set
    var appliedFilter: CampsiteFilter by mutableStateOf(CampsiteFilter())
        private set

    //US 01.01.02: the filter table is hidden by default.
    var isFilterPanelVisible: Boolean by mutableStateOf(false)
        private set
    var filterDraft: FilterDraft by mutableStateOf(FilterDraft())
        private set
    var filterError: String? by mutableStateOf(null)
        private set

    /** US 01.01.02 Locations offered as filter options, taken from the loaded campsites. */
    val locations: List<String>
        get() = campsites.map { it.location }.filter { it.isNotBlank() }.distinct().sorted()

    /** Dropdown matches for the search text (US 01.01.01). */
    val suggestions: List<Campsite>
        get() = if (searchQuery.isBlank()) emptyList() else campsites.filter { it.matchesName(searchQuery) }

    /** Campsites that satisfy the filters and search text (US 01.01.03). */
    val results: List<Campsite>
        get() = campsites.filter { appliedFilter.matches(it) && it.matchesName(searchQuery) }

    fun showLoading() { isLoading = true; errorMessage = null }
    fun showCampsites(list: List<Campsite>) { campsites = list; isLoading = false; errorMessage = null }
    fun showError(message: String) { isLoading = false; errorMessage = message }

    fun updateSearchQuery(query: String) { searchQuery = query }
    fun updateFilterDraft(draft: FilterDraft) { filterDraft = draft; filterError = null }
    fun updateFilterError(message: String?) { filterError = message }
    fun updateAppliedFilter(filter: CampsiteFilter) { appliedFilter = filter }
    fun changeFilterPanelVisibility(visible: Boolean) { isFilterPanelVisible = visible }
}
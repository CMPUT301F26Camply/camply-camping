package com.example.camply.user.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.campsite.model.Campsite

/**
 * The available pages within the admin image browser
 */
enum class AdminImageBrowsePage {
    CATEGORIES,
    OWNERS,
    CAMPSITES,
    GALLERY,
    PROFILES
}

/**
 * The possible outcomes of an image removal request.
 */
enum class ImageRemovalResult {
    SUCCESS,
    FAILURE,
    NOT_IMPLEMENTED
}

/**
 * Stores the uploaded images, browsing state, selections and filtered results for the admin image browser (US 03.06.01).
 */
class AdminImageBrowseModel {

    // Currently displayed page in the image browser.
    var page by mutableStateOf(AdminImageBrowsePage.CATEGORIES)
        private set

    // All registered users retrieved from Firestore.
    var users by mutableStateOf<List<User>>(emptyList())
        private set

    // All campsites retrieved from Firestore.
    var campsites by mutableStateOf<List<Campsite>>(emptyList())
        private set

    // Uploaded profile image bytes indexed by user ID.
    var profileImages by mutableStateOf<Map<String, ByteArray>>(emptyMap())
        private set

    // Currently selected campsite owner.
    var selectedOwner by mutableStateOf<User?>(null)
        private set

    // Currently selected campsite for browsing promotional images.
    var selectedCampsite by mutableStateOf<Campsite?>(null)
        private set

    // Currently selected user whose profile image is being viewed.
    var selectedProfile by mutableStateOf<User?>(null)
        private set

    // Search query for campsite owners.
    var ownerSearch by mutableStateOf("")
        private set

    // Search query for the selected owner's campsites.
    var campsiteSearch by mutableStateOf("")
        private set

    // Search query for users with uploaded profile images.
    var profileSearch by mutableStateOf("")
        private set

    // Indicates whether browsing data is being loaded.
    var isLoading by mutableStateOf(false)
        private set

    // Error message to display, or null if no error has occurred.
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Owners and campsite images

    // Users registered as owners or associated with an existing campsite, regardless of their current role.
    val owners: List<User>
        get() = users.filter { user ->
            user.getRole() == "owner" || campsites.any { it.ownerId == user.getUid() }
        }

    // Owners with at least one uploaded campsite promotional image.
    val ownersWithImages: List<User>
        get() = owners.filter { imageCountForOwner(it.getUid()) > 0 }

    // Owners with images matching the search query, sorted by name.
    val filteredOwners: List<User>
        get() = ownersWithImages
            .filter { matchesUser(it, ownerSearch) }
            .sortedBy { it.getProfile().getName().lowercase() }

    // All campsites belonging to the selected owner.
    val ownerCampsites: List<Campsite>
        get() = selectedOwner?.let { campsitesForOwner(it.getUid()) }.orEmpty()

    // Selected owner's campsites containing at least one promotional image.
    val ownerCampsitesWithImages: List<Campsite>
        get() = ownerCampsites.filter { it.images.isNotEmpty() }

    // Campsites with images matching the name or location search query,
    // sorted by campsite name.
    val filteredCampsites: List<Campsite>
        get() = ownerCampsitesWithImages
            .filter {
                it.matchesName(campsiteSearch) ||
                        it.location.contains(
                            campsiteSearch.trim(),
                            ignoreCase = true
                        )
            }
            .sortedBy { it.name.lowercase() }

    // Total number of owners, including those without uploaded images.
    val totalOwnerCount: Int
        get() = owners.size

    // Number of owners with uploaded campsite promotional images.
    val ownersWithImagesCount: Int
        get() = ownersWithImages.size

    // Total promotional images across all identified owners' campsites.
    val totalCampsiteImageCount: Int
        get() = owners.sumOf { imageCountForOwner(it.getUid()) }

    // User profile images

    // Registered users with uploaded profile images, regardless of role.
    val usersWithImages: List<User>
        get() = users.filter { profileImages.containsKey(it.getUid()) }

    // Users with profile images matching the search query, sorted by name.
    val filteredProfiles: List<User>
        get() = usersWithImages
            .filter { matchesUser(it, profileSearch) }
            .sortedBy { it.getProfile().getName().lowercase() }

    // Total number of registered users, including those without profile images.
    val totalUserCount: Int
        get() = users.size

    // Number of registered users with uploaded profile images.
    val usersWithImagesCount: Int
        get() = usersWithImages.size

    // Combined number of browsable campsite promotional and profile images.
    val totalImageCount: Int
        get() = totalCampsiteImageCount + usersWithImagesCount


    /**
     * Returns the campsites registered by the specified owner.
     *
     * @param ownerId unique ID of the campsite owner
     * @return list of campsites belonging to the owner
     */
    fun campsitesForOwner(ownerId: String): List<Campsite> = campsites.filter { it.ownerId == ownerId }

    /**
     * Counts the owner's campsites that contain promotional images.
     *
     * @param ownerId unique ID of the campsite owner
     * @return number of campsites containing at least one image
     */
    fun campsiteImageCountForOwner(ownerId: String): Int = campsitesForOwner(ownerId).count { it.images.isNotEmpty() }

    /**
     * Counts all promotional images across the owner's campsites.
     *
     * @param ownerId unique ID of the campsite owner
     * @return total number of promotional images
     */
    fun imageCountForOwner(ownerId: String): Int = campsitesForOwner(ownerId).sumOf { it.images.size }

    /**
     * Checks whether a user's name or email matches a search query.
     * Matching is case-insensitive and ignores surrounding whitespace.
     *
     * @param user user being searched
     * @param query search text
     * @return true if the name or email contains the search text
     */
    private fun matchesUser(user: User, query: String): Boolean {
        val profile = user.getProfile()
        val search = query.trim()

        return profile.getName().contains(search, true) || profile.getEmail().contains(search, true)
    }

    /**
     * Replaces the loaded users used for browsing and filtering.
     *
     * @param list users retrieved from Firestore
     */
    fun showUsers(list: List<User>) {
        users = list
    }

    /**
     * Replaces the loaded campsites used for browsing and image counts.
     *
     * @param list campsites retrieved from Firestore
     */
    fun showCampsites(list: List<Campsite>) {
        campsites = list
    }

    /**
     * Stores uploaded profile images for browsing and inspection.
     *
     * @param images map of user IDs to their uploaded image bytes
     */
    fun showProfileImages(images: Map<String, ByteArray>) {
        profileImages = images
    }

    /**
     * Changes the current image browsing page.
     *
     * @param page destination page
     */
    fun showPage(page: AdminImageBrowsePage) {
        this.page = page
    }

    /**
     * Updates the selected campsite owner.
     *
     * @param owner selected owner, or null to clear the selection
     */
    fun showOwner(owner: User?) {
        selectedOwner = owner
    }

    /**
     * Updates the selected campsite.
     *
     * @param campsite selected campsite, or null to clear the selection
     */
    fun showCampsite(campsite: Campsite?) {
        selectedCampsite = campsite
    }

    /**
     * Updates the selected user whose profile image is being inspected.
     *
     * @param profile selected user, or null to clear the selection
     */
    fun showProfile(profile: User?) {
        selectedProfile = profile
    }

    /**
     * Updates the search query used to filter campsite owners.
     *
     * @param query owner name or email search text
     */
    fun searchOwners(query: String) {
        ownerSearch = query
    }

    /**
     * Updates the search query used to filter an owner's campsites.
     *
     * @param query campsite name or location search text
     */
    fun searchCampsites(query: String) {
        campsiteSearch = query
    }

    /**
     * Updates the search query used to filter user profiles.
     *
     * @param query search text matching a user's profile name or email address
     */
    fun searchProfiles(query: String) {
        profileSearch = query
    }

    /**
     * Updates the loading state for browsing data
     *
     * @param loading whether browsing data is being retrieved
     */
    fun showLoading(loading: Boolean) {
        isLoading = loading
    }

    /**
     * Updates the error message displayed by the image browser.
     *
     * @param message error description, or null to clear the error
     */
    fun showError(message: String?) {
        errorMessage = message
    }

    /**
     * Refreshes the current browsing selections after image data is updated. Returns to the
     * appropriate browsing page if the selected owner or campsite no longer has promotional images.
     *
     * Preserves existing search queries and clears invalid selections.
     */
    fun refreshSelections() {
        selectedProfile?.let { profile ->
            selectedProfile = usersWithImages.find {
                it.getUid() == profile.getUid()
            }
        }

        selectedOwner?.let { owner ->
            selectedOwner = ownersWithImages.find {
                it.getUid() == owner.getUid()
            }

            if (selectedOwner == null) {
                selectedCampsite = null
                page = AdminImageBrowsePage.OWNERS
                return
            }
        }

        selectedCampsite?.let { campsite ->
            selectedCampsite = ownerCampsitesWithImages.find {
                it.id == campsite.id
            }

            if (selectedCampsite == null) {
                page = AdminImageBrowsePage.CAMPSITES
            }
        }
    }

    /**
     * Restores the image browser to its initial state.
     *
     * Clears loaded data, image references, selections, search queries, loading state and errors.
     */
    fun reset() {
        page = AdminImageBrowsePage.CATEGORIES
        users = emptyList()
        campsites = emptyList()
        profileImages = emptyMap()
        selectedOwner = null
        selectedCampsite = null
        selectedProfile = null
        ownerSearch = ""
        campsiteSearch = ""
        profileSearch = ""
        isLoading = false
        errorMessage = null
    }
}



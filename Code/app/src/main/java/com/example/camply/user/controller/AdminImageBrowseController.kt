package com.example.camply.user.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.user.model.AdminImageBrowseModel
import com.example.camply.user.model.AdminImageBrowsePage
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.User
import com.example.camply.user.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * Controls browsing and inspection of uploaded campsite promotional images and user profile images (US 03.06.01).
 *
 * @param model state of the admin image browser
 * @param userRepository source of users and profile images
 * @param campsiteRepository source of campsites and promotional images
 */
class AdminImageBrowseController(
    private val model: AdminImageBrowseModel,
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository
) {


    /**
     * Loads the users, campsites and uploaded profile images required for browsing, searching and calculating image counts.
     *
     * Refreshes existing browsing selections without clearing search queries.
     *
     * @return true if the data was loaded successfully, false otherwise
     */
    suspend fun loadData(): Boolean {
        model.showLoading(true)
        model.showError(null)

        return try {
            val users = loadUsers()
            val campsites = campsiteRepository.getCampsites()
            val profileImages = loadProfileImages()

            model.showUsers(users)
            model.showCampsites(campsites)
            model.showProfileImages(profileImages)
            model.refreshSelections()

            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            model.showError("Unable to load uploaded images.")
            false
        } finally {
            model.showLoading(false)
        }
    }


    /**
     * Retrieves users using the existing callback-based repository.
     *
     * @return list of registered users
     */
    private suspend fun loadUsers(): List<User> =
        suspendCancellableCoroutine { continuation ->
            userRepository.getAllUsers(
                onSuccess = { continuation.resume(it) },
                onFailure = {
                    continuation.resumeWithException(
                        IllegalStateException("Unable to load users.")
                    )
                }
            )
        }

    /**
     * Retrieves uploaded profile images using the existing repository.
     *
     * @return map of user IDs to their uploaded image bytes
     */
    private suspend fun loadProfileImages(): Map<String, ByteArray> =
        suspendCancellableCoroutine { continuation ->
            userRepository.getAllProfileImages(
                onSuccess = { continuation.resume(it) },
                onFailure = {
                    continuation.resumeWithException(
                        IllegalStateException("Unable to load profile images.")
                    )
                }
            )
        }

    /**
     * Opens the list of campsite owners with promotional images.
     */
    fun openOwners() {
        model.searchOwners("")
        model.showPage(AdminImageBrowsePage.OWNERS)
    }

    /**
     * Opens the list of users with uploaded profile images.
     */
    fun openProfiles() {
        model.searchProfiles("")
        model.showPage(AdminImageBrowsePage.PROFILES)
    }

    /**
     * Updates the search query for campsite owners.
     *
     * @param query owner name or email search text
     */
    fun searchOwners(query: String) {
        model.searchOwners(query)
    }

    /**
     * Updates the search query for the selected owner's campsites.
     *
     * @param query campsite name or location search text
     */
    fun searchCampsites(query: String) {
        model.searchCampsites(query)
    }

    /**
     * Updates the search query used to filter user profiles.
     *
     * @param query search text matching a user's profile name or email address
     */
    fun searchProfiles(query: String) {
        model.searchProfiles(query)
    }

    /**
     * Selects an owner and opens their campsites containing images.
     *
     * @param owner selected campsite owner
     */
    fun selectOwner(owner: User) {
        model.showOwner(owner)
        model.searchCampsites("")
        model.showPage(AdminImageBrowsePage.CAMPSITES)
    }

    /**
     * Selects a campsite and opens its promotional image gallery.
     *
     * @param campsite selected campsite
     */
    fun selectCampsite(campsite: Campsite) {
        model.showCampsite(campsite)
        model.showPage(AdminImageBrowsePage.GALLERY)
    }

    /**
     * Retrieves a promotional image preview for browsing.
     *
     * @param campsiteId ID of the campsite containing the photo
     * @param photoId ID of the promotional photo
     * @return preview image bytes, or null if unavailable
     */
    suspend fun loadPhotoPreview(
        campsiteId: String,
        photoId: String
    ): ByteArray? {
        return try {
            campsiteRepository.getCampsitePreview(campsiteId, photoId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieves the full resolution promotional image for the shared photo viewer.
     *
     * @param campsiteId ID of the campsite containing the photo
     * @param photoId ID of the promotional photo
     * @return full resolution image bytes, or null if unavailable
     */
    suspend fun loadPhoto(campsiteId: String, photoId: String): ByteArray? = campsiteRepository.getCampsitePhoto(campsiteId, photoId)

    /**
     * Requests removal of a campsite promotional image and refreshes
     * the browsing data if the removal succeeds.
     *
     * @param campsite campsite containing the selected photo
     * @param photoId ID of the promotional photo to remove
     * @param removePhoto action that performs the actual image removal
     * @return outcome of the image removal request
     */
    suspend fun removeCampsitePhoto(
        campsite: Campsite,
        photoId: String,
        removePhoto: suspend (Campsite, String) -> ImageRemovalResult
    ): ImageRemovalResult {
        return removeImage { removePhoto(campsite, photoId) }
    }

    /**
     * Requests removal of a user's profile image and refreshes
     * the browsing data if the removal succeeds.
     *
     * @param user user whose profile image is being removed
     * @param removeProfile action that performs the actual image removal
     * @return outcome of the image removal request
     */
    suspend fun removeProfileImage(
        user: User,
        removeProfile: suspend (User) -> ImageRemovalResult
    ): ImageRemovalResult {
        return removeImage { removeProfile(user) }
    }

    /**
     * Executes an image removal action and refreshes the browsing data following successful removal.
     *
     * @param remove action that performs the actual image removal
     * @return outcome of the image removal request
     */
    private suspend fun removeImage(
        remove: suspend () -> ImageRemovalResult
    ): ImageRemovalResult {
        val result = try {
            remove()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImageRemovalResult.FAILURE
        }

        if (result == ImageRemovalResult.SUCCESS) {
            closeProfileImage()

            if (!loadData()) {
                model.showError(
                    "Image removed, but unable to refresh uploaded images."
                )
            }
        }

        return result
    }

    /**
     * Selects a user whose profile image will be opened directly in the shared full screen image viewer.
     *
     * @param profile user whose uploaded image will be inspected
     */
    fun selectProfile(profile: User) {
        if (!model.profileImages.containsKey(profile.getUid())) return

        model.showProfile(profile)
    }

    /**
     * Closes the selected profile image viewer and returns to the user profile image
     * list without clearing the search query.
     */
    fun closeProfileImage() {
        model.showProfile(null)
    }

    /**
     * Returns to the previous image browsing page or closes the selected profile image viewer.
     * Preserves search queries when returning to a list.
     *
     * @return true if navigation was handled within the image browser, or false if the
     * administrator should return to the admin panel
     */
    fun back(): Boolean {
        when (model.page) {
            AdminImageBrowsePage.CATEGORIES -> {
                model.reset()
                return false
            }

            AdminImageBrowsePage.OWNERS -> {
                model.showPage(AdminImageBrowsePage.CATEGORIES)
            }

            AdminImageBrowsePage.PROFILES -> {
                if (model.selectedProfile != null) {
                    closeProfileImage()
                } else {
                    model.showPage(AdminImageBrowsePage.CATEGORIES)
                }
            }

            AdminImageBrowsePage.CAMPSITES -> {
                model.showOwner(null)
                model.showPage(AdminImageBrowsePage.OWNERS)
            }

            AdminImageBrowsePage.GALLERY -> {
                model.showCampsite(null)
                model.showPage(AdminImageBrowsePage.CAMPSITES)
            }
        }

        return true
    }

    /**
     * Clears the browsing state and releases loaded image references.
     */
    fun reset() {
        model.reset()
    }
}

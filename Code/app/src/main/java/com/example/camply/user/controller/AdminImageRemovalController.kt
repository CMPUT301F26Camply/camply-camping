package com.example.camply.user.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.User
import com.example.camply.user.repository.AdminImageRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Removes uploaded images for the admin after they confirm (US 03.03.01).
 *
 * The admin image browser asks for confirmation and shows the result,
 * so this only performs the removal and reports whether it worked.
 *
 * @param repository The repository used to remove images.
 */
class AdminImageRemovalController(
    private val repository: AdminImageRepository
) {

    /**
     * Removes one promotional photo from a campsite.
     *
     * @param campsite The campsite the photo belongs to.
     * @param photoId The ID of the photo to remove.
     * @return Whether the photo was removed.
     */
    suspend fun removeCampsitePhoto(campsite: Campsite, photoId: String): ImageRemovalResult =
        remove { repository.removeCampsitePhoto(campsite.id, photoId) }

    /**
     * Removes a user's profile image.
     *
     * @param user The user whose profile image is removed.
     * @return Whether the image was removed.
     */
    suspend fun removeProfileImage(user: User): ImageRemovalResult =
        remove { repository.removeProfileImage(user.getUid()) }

    private suspend fun remove(action: suspend () -> Unit): ImageRemovalResult =
        try {
            action()
            ImageRemovalResult.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ImageRemovalResult.FAILURE
        }
}

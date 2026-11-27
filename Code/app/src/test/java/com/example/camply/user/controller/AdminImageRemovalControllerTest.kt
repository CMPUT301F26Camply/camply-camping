package com.example.camply.user.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.user.model.EntrantUser
import com.example.camply.user.model.ImageRemovalResult
import com.example.camply.user.model.Profile
import com.example.camply.user.repository.AdminImageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

/**
 * Unit tests for removing uploaded images as an admin (US 03.03.01).
 */
class AdminImageRemovalControllerTest {

    private lateinit var repository: TestAdminImageRepository
    private lateinit var controller: AdminImageRemovalController

    private val campsite = Campsite(id = "campsite1", name = "Banff Pines")
    private val entrant = EntrantUser("entrant1", Profile("Jane Doe", "jane@example.com", "7802222222"))

    /**
     * Creates the controller with a fake repository before each test.
     */
    @Before
    fun setUp() {
        repository = TestAdminImageRepository()
        controller = AdminImageRemovalController(repository)
    }

    /**
     * Checks that removing a campsite photo removes that photo from that campsite.
     */
    @Test
    fun removeCampsitePhoto_success_removesPhoto() = runBlocking {
        val result = controller.removeCampsitePhoto(campsite, "photo1")

        assertEquals(ImageRemovalResult.SUCCESS, result)
        assertEquals(listOf("campsite1" to "photo1"), repository.removedPhotos)
    }

    /**
     * Checks that a failed campsite photo removal is reported so the admin can try again.
     */
    @Test
    fun removeCampsitePhoto_failure_returnsFailure() = runBlocking {
        repository.error = Exception("Could not delete.")

        val result = controller.removeCampsitePhoto(campsite, "photo1")

        assertEquals(ImageRemovalResult.FAILURE, result)
    }

    /**
     * Checks that removing a profile image removes the image for that user.
     */
    @Test
    fun removeProfileImage_success_removesImage() = runBlocking {
        val result = controller.removeProfileImage(entrant)

        assertEquals(ImageRemovalResult.SUCCESS, result)
        assertEquals(listOf("entrant1"), repository.removedProfiles)
    }

    /**
     * Checks that a failed profile image removal is reported so the admin can try again.
     */
    @Test
    fun removeProfileImage_failure_returnsFailure() = runBlocking {
        repository.error = Exception("Could not delete.")

        val result = controller.removeProfileImage(entrant)

        assertEquals(ImageRemovalResult.FAILURE, result)
        assertTrue(repository.removedProfiles.isEmpty())
    }

    /**
     * Checks that a cancelled removal is not reported as a failure.
     */
    @Test
    fun removeImage_cancelled_rethrowsCancellation() {
        repository.error = CancellationException("Left the screen.")

        assertThrows(CancellationException::class.java) {
            runBlocking { controller.removeCampsitePhoto(campsite, "photo1") }
        }
    }

    /**
     * Records removals without accessing Firestore, and fails with a chosen error.
     */
    private class TestAdminImageRepository : AdminImageRepository {
        val removedPhotos = mutableListOf<Pair<String, String>>()
        val removedProfiles = mutableListOf<String>()
        var error: Exception? = null

        override suspend fun removeCampsitePhoto(campsiteId: String, photoId: String) {
            error?.let { throw it }
            removedPhotos.add(campsiteId to photoId)
        }

        override suspend fun removeProfileImage(userId: String) {
            error?.let { throw it }
            removedProfiles.add(userId)
        }
    }
}

package com.example.camply.campsite.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/** Tests the silent refresh used to update the details header's average rating (US 01.13.01). */
class CampsiteDetailControllerRefreshTest {

    private lateinit var repository: FakeCampsiteRepository
    private lateinit var model: CampsiteDetailModel
    private lateinit var controller: CampsiteDetailController

    @Before
    fun setUp() {
        repository = FakeCampsiteRepository(Campsite(id = "site1", name = "Pine Ridge", rating = 3.0))
        model = CampsiteDetailModel()
        controller = CampsiteDetailController(model, repository)
    }

    @Test
    fun refreshCampsite_showsNewRatingWithoutLoadingState() = runBlocking {
        controller.loadCampsite("site1")
        repository.campsite = Campsite(id = "site1", name = "Pine Ridge", rating = 4.5)

        controller.refreshCampsite("site1")

        assertEquals(4.5, model.campsite?.rating ?: 0.0, 0.0001)
        assertEquals(false, model.isLoading)
        assertNull(model.errorMessage)
    }

    @Test
    fun refreshCampsite_failureKeepsCurrentDetails() = runBlocking {
        controller.loadCampsite("site1")
        val shown = model.campsite
        repository.failure = IllegalStateException("Offline")

        controller.refreshCampsite("site1")

        assertSame(shown, model.campsite)
        assertNull(model.errorMessage)
    }

    /** In-memory repository, so no Firebase is needed. */
    private class FakeCampsiteRepository(var campsite: Campsite) : CampsiteRepository {
        var failure: Exception? = null

        override suspend fun getCampsite(id: String): Campsite? {
            failure?.let { throw it }
            return campsite.takeIf { it.id == id }
        }

        override suspend fun getCampsites(): List<Campsite> = emptyList()
        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> = emptyList()
        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = campsite.id
        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null
        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null
        override suspend fun deleteCampsite(id: String) {}
    }
}
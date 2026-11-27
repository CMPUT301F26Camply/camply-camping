
package com.example.camply.user.controller

import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.Profile
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


/**
 * Local unit tests for campsite owner registration, editing, and campsite list loading.
 *
 * US 02.01.01 - 02.01.03: Uses a fake repository so no Firebase
 * connection is required.
 */
class OwnerControllerTest {

    private lateinit var owner: CampsiteOwnerUser
    private lateinit var repository: FakeCampsiteRepository
    private lateinit var controller: OwnerController

    @Before
    fun setUp() {
        owner = CampsiteOwnerUser(
            "owner1",
            Profile("Alex", "alex@example.com", "7801234567")
        )
        repository = FakeCampsiteRepository()
        controller = OwnerController(owner, repository)
    }

    /**
     * Registers a valid campsite without any optional images.
     */
    @Test
    fun registerCampsite_withoutPhotos_savesSuccessfully() = runBlocking {
        val result = registerCampsite()

        assertTrue(result)
        assertEquals(1, repository.saveCalls)
        assertEquals("generated site", controller.campsites.single().id)
        assertTrue(repository.lastPhotosToSave.isEmpty())
        assertTrue(repository.lastPhotosToRemove.isEmpty())
        assertTrue(controller.errors.isEmpty())
    }

    /**
     * Invalid campsite details should prevent a repository save.
     */
    @Test
    fun registerCampsite_invalidName_doesNotSave() = runBlocking {
        val result = registerCampsite(name = "")

        assertFalse(result)
        assertEquals(0, repository.saveCalls)
        assertTrue(controller.campsites.isEmpty())
        assertTrue(controller.errors.isNotEmpty())
    }

    /**
     * Removing a photo during editing sends its ID to the repository.
     * Unchanged photos remain in the final campsite image list.
     */
    @Test
    fun updateCampsite_removedPhoto_sendsRemovalId() = runBlocking {
        repository.sites.add(
            existingCampsite(listOf("photo1", "photo2"))
        )
        controller.loadCampsite()

        val result = updateCampsite(
            controller.campsites.single(),
            images = listOf("photo1")
        )

        assertTrue(result)
        assertEquals(listOf("photo2"), repository.lastPhotosToRemove)
        assertTrue(repository.lastPhotosToSave.isEmpty())
        assertEquals(
            listOf("photo1"),
            repository.lastSavedCampsite?.images
        )
        assertEquals(
            listOf("photo1"),
            controller.campsites.single().images
        )
    }

    /**
     * Editing campsite details without changing images should not
     * upload or remove any photos.
     */
    @Test
    fun updateCampsite_unchangedPhotos_preservesImages() = runBlocking {
        repository.sites.add(existingCampsite(listOf("photo1", "photo2")))
        controller.loadCampsite()

        val result = updateCampsite(
            controller.campsites.single(),
            images = listOf("photo1", "photo2")
        )

        assertTrue(result)
        assertTrue(repository.lastPhotosToSave.isEmpty())
        assertTrue(repository.lastPhotosToRemove.isEmpty())
        assertEquals(
            listOf("photo1", "photo2"),
            controller.campsites.single().images
        )
    }

    /**
     * A repository failure should not add an unsaved campsite
     * to the owner's local list.
     */
    @Test
    fun registerCampsite_saveFailure_keepsLocalListUnchanged() = runBlocking {
        repository.failOnSave = true

        val result = registerCampsite()

        assertFalse(result)
        assertTrue(controller.campsites.isEmpty())
        assertTrue(owner.campsites.isEmpty())
        assertTrue(controller.errors.isNotEmpty())
    }

    /**
     * Registering a campsite without a map pin should fail validation.
     */
    @Test
    fun registerCampsite_missingMapPin_doesNotSave() = runBlocking {
        val result = registerCampsite(latitude = null, longitude = null)

        assertFalse(result)
        assertEquals(0, repository.saveCalls)
        assertTrue(controller.campsites.isEmpty())
        assertTrue(controller.errors.contains("Please select a campsite location on the map"))
    }

    /**
     * Invalid map coordinates should prevent a campsite from being saved.
     */
    @Test
    fun registerCampsite_invalidCoordinates_doesNotSave() = runBlocking {
        val result = registerCampsite(latitude = 95.0, longitude = -200.0)

        assertFalse(result)
        assertEquals(0, repository.saveCalls)
        assertTrue(controller.errors.contains("Please enter a valid latitude"))
        assertTrue(controller.errors.contains("Please enter a valid longitude"))
    }

    /**
     * Editing a campsite should save and reload its updated map coordinates.
     */
    @Test
    fun updateCampsite_changedCoordinates_savesSuccessfully() = runBlocking {
        repository.sites.add(existingCampsite(emptyList()))
        controller.loadCampsite()

        val result = updateCampsite(
            existing = controller.campsites.single(),
            images = emptyList(),
            latitude = 53.5461,
            longitude = -113.4937
        )

        assertTrue(result)
        assertEquals(1, repository.saveCalls)
        assertEquals(53.5461, repository.lastSavedCampsite?.latitude)
        assertEquals(-113.4937, repository.lastSavedCampsite?.longitude)
        assertEquals(53.5461, controller.campsites.single().latitude)
        assertEquals(-113.4937, controller.campsites.single().longitude)
    }

    /**
     * Successfully loading campsites updates the owner's campsite list and clears the
     * loading and error states.
     */
    @Test
    fun loadCampsite_success_updatesCampsites() = runBlocking {
        val campsite = existingCampsite(emptyList())
        repository.sites.add(campsite)

        controller.loadCampsite()

        assertEquals(listOf(campsite), controller.campsites)
        assertEquals(listOf(campsite), owner.campsites)
        assertFalse(controller.loadingCheck)
        assertFalse(controller.loadingCampsiteListFailed)
        assertTrue(controller.errors.isEmpty())
    }

    /**
     * An empty campsite list returned successfully by the repository should not be
     * treated as a loading failure.
     */
    @Test
    fun loadCampsite_emptyList_isNotFailure() = runBlocking {
        controller.loadCampsite()

        assertTrue(controller.campsites.isEmpty())
        assertTrue(owner.campsites.isEmpty())
        assertFalse(controller.loadingCheck)
        assertFalse(controller.loadingCampsiteListFailed)
        assertTrue(controller.errors.isEmpty())
    }

    /**
     * A repository failure should set the loading failure state, record the error and stop loading.
     */
    @Test
    fun loadCampsite_failure_setsErrorState() = runBlocking {
        repository.failOnLoad = true

        controller.loadCampsite()

        assertTrue(controller.loadingCampsiteListFailed)
        assertFalse(controller.loadingCheck)
        assertTrue(controller.campsites.isEmpty())
        assertEquals(
            listOf("Simulated load failure"),
            controller.errors
        )
    }

    /**
     * Successfully retrying a failed campsite load should populate the campsite list
     * and clear the previous error.
     */
    @Test
    fun loadCampsite_retrySuccess_clearsErrorState() = runBlocking {
        repository.failOnLoad = true

        controller.loadCampsite()

        assertTrue(controller.loadingCampsiteListFailed)

        val campsite = existingCampsite(emptyList())
        repository.sites.add(campsite)
        repository.failOnLoad = false

        controller.loadCampsite()

        assertEquals(listOf(campsite), controller.campsites)
        assertEquals(listOf(campsite), owner.campsites)
        assertFalse(controller.loadingCampsiteListFailed)
        assertFalse(controller.loadingCheck)
        assertTrue(controller.errors.isEmpty())
    }

    /**
     * A failed refresh should preserve previously loaded campsites instead of
     * clearing the owner's campsite list.
     */
    @Test
    fun loadCampsite_failure_preservesExistingCampsites() = runBlocking {
        val campsite = existingCampsite(emptyList())
        repository.sites.add(campsite)

        controller.loadCampsite()

        repository.sites.clear()
        repository.failOnLoad = true

        controller.loadCampsite()

        assertEquals(listOf(campsite), controller.campsites)
        assertEquals(listOf(campsite), owner.campsites)
        assertTrue(controller.loadingCampsiteListFailed)
        assertFalse(controller.loadingCheck)
    }

    /**
     * Submits valid registration details with an optional name.
     */
    private suspend fun registerCampsite(
        name: String = "Banff Pines",
        latitude: Double? = 53.3855,
        longitude: Double? = -107.9513
    ): Boolean {
        return controller.registerCampsite(
            name = name,
            location = "Banff",
            description = "Forest campsite",
            amenities = "Water, Parking",
            maxOccupancy = "4",
            price = "25.00",
            tags = "Forest",
            rules = "No open fires",
            cancellationPolicy = "24 hours",
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Updates an existing campsite's final photo ID list.
     */
    private suspend fun updateCampsite(
        existing: Campsite,
        images: List<String>,
        latitude: Double? = existing.latitude,
        longitude: Double? = existing.longitude
    ): Boolean {
        return controller.updateCampsite(
            existingSite = existing,
            name = existing.name,
            location = existing.location,
            description = existing.description,
            amenities = existing.amenities.joinToString(","),
            maxOccupancy = existing.maxOccupancy.toString(),
            price = existing.price.toString(),
            tags = existing.tags.joinToString(","),
            rules = existing.rules,
            cancellationPolicy = existing.cancellationPolicy,
            images = images,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Creates an existing campsite for editing and loading tests.
     */
    private fun existingCampsite(images: List<String>): Campsite {
        return Campsite(
            id = "site1",
            name = "Banff Pines",
            location = "Banff",
            description = "Forest campsite",
            maxOccupancy = 4,
            price = 25.0,
            rules = "No open fires",
            cancellationPolicy = "24 hours",
            ownerId = "owner1",
            images = images,
            latitude = 53.3855,
            longitude = -107.9513
        )
    }

    /**
     * Fake campsite repository that records saves and photo changes and supports simulated
     * save and loading failures. Does not connect to Firestore.
     */
    private class FakeCampsiteRepository : CampsiteRepository {

        val sites = mutableListOf<Campsite>()

        var saveCalls = 0
        var failOnSave = false
        var lastSavedCampsite: Campsite? = null
        var lastPhotosToSave: Map<String, PreparedCampsiteImages> = emptyMap()
        var lastPhotosToRemove: List<String> = emptyList()

        // Simulates a failure when loading the owner's campsites.
        var failOnLoad = false

        override suspend fun getCampsites(): List<Campsite> = sites.toList()

        override suspend fun getCampsite(id: String): Campsite? = sites.find { it.id == id }

        /**
         * Returns the owner's campsites or simulates a repository failure.
         */
        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> {
            if (failOnLoad) {
                error("Simulated load failure")
            }

            return sites.filter { it.ownerId == ownerId }
        }

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String {
            saveCalls++

            if (failOnSave) {
                error("Simulated save failure")
            }

            lastSavedCampsite = campsite
            lastPhotosToSave = photosToSave
            lastPhotosToRemove = photosToRemove

            val id = campsite.id.ifBlank { "generated site" }

            val saved = Campsite(
                id = id,
                name = campsite.name,
                location = campsite.location,
                description = campsite.description,
                amenities = campsite.amenities,
                maxOccupancy = campsite.maxOccupancy,
                price = campsite.price,
                rating = campsite.rating,
                tags = campsite.tags,
                rules = campsite.rules,
                cancellationPolicy = campsite.cancellationPolicy,
                ownerId = campsite.ownerId,
                availability = campsite.availability,
                images = campsite.images,
                latitude = campsite.latitude,
                longitude = campsite.longitude
            )

            sites.removeAll { it.id == id }
            sites.add(saved)

            return id
        }

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun deleteCampsite(id: String) { sites.removeAll { it.id == id } }
    }
}

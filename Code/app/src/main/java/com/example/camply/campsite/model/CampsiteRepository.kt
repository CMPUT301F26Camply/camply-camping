package com.example.camply.campsite.model

import com.example.camply.campsite.service.CampsiteImageService
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Where campsite data comes from and goes to. Controllers depend on this interface, not on Firebase,
 * so the data source can be swapped or faked in tests. One repository serves both phases:
 * 2A (owner registration) uses the save/delete/by-owner methods, 2B (discovery) uses the read methods.
 */
interface CampsiteRepository {
    /** US 01.01.01 - 01.01.04: all campsites, for search, filter and list. */
    suspend fun getCampsites(): List<Campsite>

    /** US 01.01.05: loads one campsite by id. */
    suspend fun getCampsite(id: String): Campsite?

    /** US 02.03.01: the campsites that belong to one owner. */
    suspend fun getCampsitesByOwner(ownerId: String): List<Campsite>

    /**
     * US 02.01.01 - 02.01.03: Creates or updates a campsite and its photos.
     *
     * The campsite contains the final ordered image IDs. Only new or replacement
     * photos need to be uploaded. Existing unchanged photos are preserved.
     *
     * @param campsite campsite to create or update
     * @param photosToSave new or replacement photos mapped by photo ID
     * @param photosToRemove IDs of existing photos to delete
     * @return the campsite's Firestore document ID
     */
    suspend fun saveCampsite(
        campsite: Campsite,
        photosToSave: Map<String, PreparedCampsiteImages> = emptyMap(),
        photosToRemove: List<String> = emptyList()
    ): String

    /**
     * US 01.01.04 / 01.01.05: Retrieves a campsite photo's display version.
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param photoId Firestore document ID of the photo
     * @return compressed display JPEG bytes, or null if unavailable
     */
    suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray?

    /**
     * US 01.01.05: Retrieves a campsite photo's full resolution version.
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param photoId Firestore document ID of the photo
     * @return compressed full JPEG bytes, or null if unavailable
     */
    suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray?

    /** Removes a campsite (owner's removeCampsite action). */
    suspend fun deleteCampsite(id: String)
}

/** Firestore-backed implementation. Collection: "campsites". */
class FirestoreCampsiteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CampsiteRepository {

    private val campsites = db.collection("campsites")

    override suspend fun getCampsites(): List<Campsite> =
        campsites.get().await().toObjects(Campsite::class.java)

    override suspend fun getCampsite(id: String): Campsite? =
        campsites.document(id).get().await().toObject(Campsite::class.java)

    override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
        campsites.whereEqualTo("ownerId", ownerId).get().await().toObjects(Campsite::class.java)


    /**
     * US 02.01.01 - 02.01.03: Creates or updates a campsite and its photos.
     *
     * Uses a Firestore batch to save the campsite, upload new or replacement photos,
     * and remove deleted photos together. Unchanged photo documents are left untouched.
     *
     * @param campsite campsite containing the final ordered photo IDs
     * @param photosToSave new or replacement photos mapped by photo ID
     * @param photosToRemove IDs of existing photos to delete
     * @return the campsite's Firestore document ID
     */
    override suspend fun saveCampsite(
        campsite: Campsite,
        photosToSave: Map<String, PreparedCampsiteImages>,
        photosToRemove: List<String>
    ): String {
        // Validate the campsite's final photo ID list.
        require(
            campsite.images.size <= Campsite.MAX_IMAGES &&
                    campsite.images.all { it.isNotBlank() && '/' !in it } &&
                    campsite.images.size == campsite.images.distinct().size
        ) {
            "Invalid campsite photo IDs"
        }

        // Uploaded photos must appear in the final photo list.
        require(photosToSave.keys.all { it in campsite.images }) {
            "Uploaded photos must belong to the campsite"
        }

        // Removed photos cannot remain in the final photo list.
        require(
            photosToRemove.size == photosToRemove.distinct().size &&
                    photosToRemove.all {
                        it.isNotBlank() &&
                                '/' !in it &&
                                it !in campsite.images &&
                                it !in photosToSave.keys
                    }
        ) {
            "Invalid photo removal IDs"
        }

        // Check that both image versions fit their size limits.
        require(photosToSave.values.all {
            it.fullImage.isNotEmpty() &&
                    it.fullImage.size <= CampsiteImageService.FULL_IMAGE_MAX_BYTES &&
                    it.previewImage.isNotEmpty() &&
                    it.previewImage.size <= CampsiteImageService.PREVIEW_IMAGE_MAX_BYTES
        }) {
            "Campsite photo exceeds the allowed size"
        }

        // Generate a document ID when registering a new campsite.
        val ref = if (campsite.id.isBlank()) {
            campsites.document()
        } else {
            campsites.document(campsite.id)
        }

        // New campsites must provide data for all their photo IDs.
        if (campsite.id.isBlank()) {
            require(photosToRemove.isEmpty()) {
                "A new campsite cannot remove existing photos"
            }

            require(campsite.images.all { it in photosToSave.keys }) {
                "New campsite photos must include image data"
            }
        }

        val batch = db.batch()

        // Save the campsite and its final ordered photo ID list.
        batch.set(ref, campsite)

        // Upload new photos or overwrite existing photos.
        for ((photoId, images) in photosToSave) {
            val fullImageRef = ref.collection("photos").document(photoId)
            val previewRef = ref.collection("previews").document(photoId)

            batch.set(fullImageRef, mapOf("image" to Blob.fromBytes(images.fullImage)))

            batch.set(previewRef, mapOf("image" to Blob.fromBytes(images.previewImage)))
        }

        // Delete both versions of removed photos.
        for (photoId in photosToRemove) {
            batch.delete(ref.collection("photos").document(photoId))
            batch.delete(ref.collection("previews").document(photoId))
        }

        // Commit all campsite and photo changes together.
        batch.commit().await()

        return ref.id
    }

    /**
     * US 01.01.04 / 01.01.05: Retrieves the smaller display version of a campsite photo.
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param photoId Firestore document ID of the photo
     * @return display JPEG bytes, or null if unavailable
     */
    override suspend fun getCampsitePreview(
        campsiteId: String,
        photoId: String
    ): ByteArray? {
        return readPhoto(campsiteId, photoId, "previews")
    }

    /**
     * US 01.01.05: Retrieves the full resolution version of a campsite photo.
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param photoId Firestore document ID of the photo
     * @return full JPEG bytes, or null if unavailable
     */
    override suspend fun getCampsitePhoto(
        campsiteId: String,
        photoId: String
    ): ByteArray? {
        return readPhoto(campsiteId, photoId, "photos")
    }

    /**
     * Retrieves image bytes from a campsite photo subcollection.
     *
     * @param campsiteId Firestore document ID of the campsite
     * @param photoId Firestore document ID of the photo
     * @param collection photo subcollection to read from
     * @return JPEG bytes, or null if unavailable
     */
    private suspend fun readPhoto(
        campsiteId: String,
        photoId: String,
        collection: String
    ): ByteArray? {
        if (campsiteId.isBlank() || photoId.isBlank()) {
            return null
        }

        val document = campsites.document(campsiteId)
            .collection(collection)
            .document(photoId)
            .get()
            .await()

        return document.getBlob("image")?.toBytes()
    }

    /**
     * Removes a campsite and all photos referenced by its image list.
     *
     * @param id Firestore document ID of the campsite to remove
     */
    override suspend fun deleteCampsite(id: String) {
        require(id.isNotBlank()) {
            "Campsite ID cannot be blank"
        }

        val ref = campsites.document(id)

        val campsite = ref.get().await().toObject(Campsite::class.java) ?: return

        val batch = db.batch()

        // Delete both versions of each stored photo.
        for (photoId in campsite.images.distinct()) {
            batch.delete(ref.collection("photos").document(photoId))
            batch.delete(ref.collection("previews").document(photoId))
        }

        // Delete the campsite document
        batch.delete(ref)

        batch.commit().await()
    }
}
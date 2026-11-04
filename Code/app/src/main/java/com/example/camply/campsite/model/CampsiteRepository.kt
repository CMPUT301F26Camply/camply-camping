package com.example.camply.campsite.model

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
     * US 02.01.01 / 02.01.03: creates the campsite (blank id) or overwrites it (existing id).
     *
     * @return the campsite's Firestore document id
     */
    suspend fun saveCampsite(campsite: Campsite): String

    /** Removes a campsite (owner's removeCampsite action). */
    suspend fun deleteCampsite(id: String)
}

/** Firestore-backed implementation. Collection: "campsites". */
class FirestoreCampsiteRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CampsiteRepository {

    private val campsites = db.collection("campsites")

    override suspend fun getCampsites(): List<Campsite> =
        campsites.get().await().toObjects(Campsite::class.java)

    override suspend fun getCampsite(id: String): Campsite? =
        campsites.document(id).get().await().toObject(Campsite::class.java)

    override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
        campsites.whereEqualTo("ownerId", ownerId).get().await().toObjects(Campsite::class.java)

    override suspend fun saveCampsite(campsite: Campsite): String {
        val ref = if (campsite.id.isBlank()) campsites.document() else campsites.document(campsite.id)
        ref.set(campsite).await()
        return ref.id
    }

    override suspend fun deleteCampsite(id: String) {
        campsites.document(id).delete().await()
    }
}
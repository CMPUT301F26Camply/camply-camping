package com.example.camply.campsite.model

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Where campsite data comes from. Controllers depend on this interface, not on Firebase,
 * so the data source can be swapped or faked in tests.
 *
 * Phase 2A (owner registration) should add its save/update methods here.
 */
interface CampsiteRepository {
    suspend fun getCampsites(): List<Campsite>
    suspend fun getCampsite(id: String): Campsite?
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
}
package com.example.camply.user.controller

import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.user.model.AdminModel
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.Profile
import com.example.camply.user.model.User
import com.example.camply.user.repository.UserRepository
import kotlinx.coroutines.CancellationException

/**
 * Loads the campsite owners an admin can browse, and the campsites for the owner they select.
 *
 * @param userRepository source of user documents
 * @param campsiteRepository source of campsite documents
 *
 * Covering US 03.04.01
 */
class AdminController(
    private val model: AdminModel,
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository
) {

    /**
     * Loads every user whose Firestore role is "owner".
     */
    fun loadOwners() {
        userRepository.getUsersByRole(
            role = "owner",
            onSuccess = { documents ->
                model.showOwners(documents.mapNotNull { document ->
                    val name = document.getString("name") ?: return@mapNotNull null
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phoneNumber") ?: ""
                    CampsiteOwnerUser(document.id, Profile(name, email, phone))
                }
                )
            },
            { model.showOwners(emptyList()) }        )
    }

    /**
     * Counts registered campsites for each owner id.
     */
    suspend fun loadCampsiteCounts() {
        try {
            model.showCampsiteCounts(
                campsiteRepository.getCampsites()
                    .groupingBy { it.ownerId }
                    .eachCount()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showCampsiteCounts(emptyMap())
        }
    }

    /**
     * Loads the campsites registered by one owner.
     *
     * @param ownerId Firebase UID of the selected owner
     */
    suspend fun loadCampsitesFor(ownerId: String) {
        try {
            model.showOwnerCampsites(campsiteRepository.getCampsitesByOwner(ownerId))        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showOwnerCampsites(emptyList())
        }
    }
}
package com.example.camply.user.model

import com.example.camply.campsite.model.Campsite
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for admin image browsing (US 03.06.01) and refreshing browsing state following
 * image removal (US 03.03.01).
 */
class AdminImageBrowseModelTest {

    // Model being tested.
    private lateinit var model: AdminImageBrowseModel

    // Owner with promotional and profile images.
    private val owner = CampsiteOwnerUser(
        "owner-1", Profile("Daniel Smith", "daniel@example.com", "7801111111")
    )

    // Registered owner without uploaded images.
    private val emptyOwner = CampsiteOwnerUser(
        "owner-2", Profile("Emily Jones", "emily@example.com", "7802222222")
    )

    // Admin with promotional and profile images.
    private val admin = AdminUser(
        "admin-1", Profile("Alex Brown", "alex@example.com", "7803333333")
    )

    // Entrant with an uploaded profile image.
    private val entrant = EntrantUser(
        "entrant-1", Profile("Chris Wilson", "chris@example.com", "7804444444")
    )

    // Campsite containing multiple promotional images.
    private val pine = Campsite(
        id = "pine",
        name = "Pine Retreat",
        location = "Banff",
        ownerId = owner.getUid(),
        images = listOf("photo-1", "photo-2")
    )

    // Campsite containing a single promotional image.
    private val lake = Campsite(
        id = "lake",
        name = "Lake Retreat",
        location = "Jasper",
        ownerId = owner.getUid(),
        images = listOf("photo-3")
    )

    // Campsite without promotional images.
    private val emptyCampsite = Campsite(
        id = "empty",
        name = "Empty Campsite",
        location = "Edmonton",
        ownerId = owner.getUid()
    )

    // Campsite belonging to a user with the admin role.
    private val adminCampsite = Campsite(
        id = "admin-campsite",
        name = "Mountain Retreat",
        location = "Canmore",
        ownerId = admin.getUid(),
        images = listOf("photo-4")
    )

    /**
     * Initializes a fresh image browsing model before each test.
     */
    @Before
    fun setUp() {
        model = AdminImageBrowseModel()
    }

    /**
     * Loads example users, campsites and profile image bytes into the model.
     */
    private fun loadTestData() {
        model.showUsers(listOf(owner, emptyOwner, admin, entrant))
        model.showCampsites(listOf(pine, lake, emptyCampsite, adminCampsite))
        model.showProfileImages(
            mapOf(
                owner.getUid() to byteArrayOf(1),
                admin.getUid() to byteArrayOf(2),
                entrant.getUid() to byteArrayOf(3)
            )
        )
    }

    // Initial state

    /**
     * Verifies that the model initially contains no loaded data,
     * selections, search queries or errors.
     */
    @Test
    fun initialState_isEmpty() {
        assertEquals(AdminImageBrowsePage.CATEGORIES, model.page)

        assertTrue(model.users.isEmpty())
        assertTrue(model.campsites.isEmpty())
        assertTrue(model.profileImages.isEmpty())

        assertNull(model.selectedOwner)
        assertNull(model.selectedCampsite)
        assertNull(model.selectedProfile)

        assertEquals("", model.ownerSearch)
        assertEquals("", model.campsiteSearch)
        assertEquals("", model.profileSearch)

        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
        assertEquals(0, model.totalImageCount)
    }

    // Owner filtering and searching

    /**
     * Verifies that registered owners and users associated with existing
     * campsites are included regardless of their current account role.
     */
    @Test
    fun owners_includesRegisteredOwnersAndCampsiteOwners() {
        loadTestData()

        assertEquals(3, model.totalOwnerCount)
        assertTrue(model.owners.contains(owner))
        assertTrue(model.owners.contains(emptyOwner))
        assertTrue(model.owners.contains(admin))
        assertFalse(model.owners.contains(entrant))
    }

    /**
     * Verifies that users satisfying both ownership conditions are
     * counted only once, even when they own multiple campsites.
     */
    @Test
    fun owners_doesNotDoubleCountUsers() {
        loadTestData()

        assertEquals(
            1,
            model.owners.count { it.getUid() == owner.getUid() }
        )

        assertEquals(3, model.totalOwnerCount)
    }

    /**
     * Verifies that only owners with uploaded promotional images appear
     * in the image browsing results.
     */
    @Test
    fun ownersWithImages_excludesOwnersWithoutImages() {
        loadTestData()

        assertEquals(2, model.ownersWithImagesCount)
        assertTrue(model.ownersWithImages.contains(owner))
        assertTrue(model.ownersWithImages.contains(admin))
        assertFalse(model.ownersWithImages.contains(emptyOwner))
    }

    /**
     * Verifies that owner searches match profile names and email addresses,
     * ignore capitalization and surrounding whitespace, and exclude
     * owners without uploaded images.
     */
    @Test
    fun filteredOwners_matchesNameAndEmail() {
        loadTestData()

        model.searchOwners("  DANIEL  ")
        assertEquals(listOf(owner), model.filteredOwners)

        model.searchOwners("alex@example.com")
        assertEquals(listOf(admin), model.filteredOwners)

        model.searchOwners("Emily")
        assertTrue(model.filteredOwners.isEmpty())

        model.searchOwners("unknown")
        assertTrue(model.filteredOwners.isEmpty())

        model.searchOwners("")
        assertEquals(listOf(admin, owner), model.filteredOwners)
    }

    // Campsite filtering and searching

    /**
     * Verifies that only campsites belonging to the selected owner
     * are returned, including campsites without promotional images.
     */
    @Test
    fun ownerCampsites_returnsSelectedOwnersCampsites() {
        loadTestData()

        assertTrue(model.ownerCampsites.isEmpty())

        model.showOwner(owner)

        assertEquals(
            listOf(pine, lake, emptyCampsite),
            model.ownerCampsites
        )

        model.showOwner(admin)

        assertEquals(listOf(adminCampsite), model.ownerCampsites)
    }

    /**
     * Verifies that campsites without promotional images are excluded
     * from the selected owner's image browsing results.
     */
    @Test
    fun ownerCampsitesWithImages_excludesEmptyCampsites() {
        loadTestData()
        model.showOwner(owner)

        assertEquals(3, model.ownerCampsites.size)
        assertEquals(2, model.ownerCampsitesWithImages.size)
        assertFalse(model.ownerCampsitesWithImages.contains(emptyCampsite))
    }

    /**
     * Verifies that campsite searches match campsite names and locations
     * while excluding campsites without images and campsites belonging
     * to other owners.
     */
    @Test
    fun filteredCampsites_matchesNameAndLocation() {
        loadTestData()
        model.showOwner(owner)

        model.searchCampsites("  PINE  ")
        assertEquals(listOf(pine), model.filteredCampsites)

        model.searchCampsites("JASPER")
        assertEquals(listOf(lake), model.filteredCampsites)

        model.searchCampsites("Edmonton")
        assertTrue(model.filteredCampsites.isEmpty())

        model.searchCampsites("Canmore")
        assertTrue(model.filteredCampsites.isEmpty())

        model.searchCampsites("")
        assertEquals(listOf(lake, pine), model.filteredCampsites)
    }

    // Profile image filtering and searching

    /**
     * Verifies that uploaded profile images are included regardless of
     * whether the user is an entrant, owner or admin.
     */
    @Test
    fun usersWithImages_includesAllRoles() {
        loadTestData()

        assertEquals(4, model.totalUserCount)
        assertEquals(3, model.usersWithImagesCount)

        assertTrue(model.usersWithImages.contains(owner))
        assertTrue(model.usersWithImages.contains(admin))
        assertTrue(model.usersWithImages.contains(entrant))
        assertFalse(model.usersWithImages.contains(emptyOwner))
    }

    /**
     * Verifies that profile searches match profile names and email
     * addresses, ignore capitalization and surrounding whitespace,
     * and exclude users without uploaded profile images.
     */
    @Test
    fun filteredProfiles_matchesNameAndEmail() {
        loadTestData()

        model.searchProfiles("  CHRIS  ")
        assertEquals(listOf(entrant), model.filteredProfiles)

        model.searchProfiles("daniel@example.com")
        assertEquals(listOf(owner), model.filteredProfiles)

        model.searchProfiles("Emily")
        assertTrue(model.filteredProfiles.isEmpty())

        model.searchProfiles("unknown")
        assertTrue(model.filteredProfiles.isEmpty())

        model.searchProfiles("")
        assertEquals(listOf(admin, entrant, owner), model.filteredProfiles)
    }

    /**
     * Verifies that owner, campsite and profile image browsing results
     * are sorted alphabetically by their respective names.
     */
    @Test
    fun filteredResults_areSortedAlphabetically() {
        loadTestData()
        model.showOwner(owner)

        assertEquals(listOf(admin, owner), model.filteredOwners)
        assertEquals(listOf(lake, pine), model.filteredCampsites)
        assertEquals(listOf(admin, entrant, owner), model.filteredProfiles)
    }

    // Image counts

    /**
     * Verifies that owner, campsite, user, promotional image and profile
     * image counts are calculated correctly from the loaded data.
     */
    @Test
    fun imageCounts_areCalculatedCorrectly() {
        loadTestData()

        assertEquals(3, model.totalOwnerCount)
        assertEquals(2, model.ownersWithImagesCount)
        assertEquals(4, model.totalCampsiteImageCount)

        assertEquals(3, model.campsitesForOwner(owner.getUid()).size)
        assertEquals(2, model.campsiteImageCountForOwner(owner.getUid()))
        assertEquals(3, model.imageCountForOwner(owner.getUid()))

        assertEquals(4, model.totalUserCount)
        assertEquals(3, model.usersWithImagesCount)
        assertEquals(7, model.totalImageCount)
    }

    /**
     * Verifies that filtering browsing results does not change the
     * overall owner, user or uploaded image counts.
     */
    @Test
    fun search_doesNotChangeTotalCounts() {
        loadTestData()
        model.showOwner(owner)

        model.searchOwners("unknown")
        model.searchCampsites("unknown")
        model.searchProfiles("unknown")

        assertTrue(model.filteredOwners.isEmpty())
        assertTrue(model.filteredCampsites.isEmpty())
        assertTrue(model.filteredProfiles.isEmpty())

        assertEquals(3, model.totalOwnerCount)
        assertEquals(2, model.ownersWithImagesCount)
        assertEquals(4, model.totalCampsiteImageCount)

        assertEquals(4, model.totalUserCount)
        assertEquals(3, model.usersWithImagesCount)
        assertEquals(7, model.totalImageCount)
    }

    /**
     * Verifies that changing an owner's role to admin does not prevent
     * their existing promotional or profile images from being browsed.
     */
    @Test
    fun changingOwnerRole_preservesUploadedImages() {
        loadTestData()

        val updatedOwner = AdminUser(
            owner.getUid(),
            owner.getProfile()
        )

        model.showUsers(listOf(updatedOwner, emptyOwner, admin, entrant))

        assertTrue(model.ownersWithImages.contains(updatedOwner))
        assertTrue(model.usersWithImages.contains(updatedOwner))

        assertEquals(2, model.ownersWithImagesCount)
        assertEquals(4, model.totalCampsiteImageCount)
        assertEquals(3, model.usersWithImagesCount)
        assertEquals(7, model.totalImageCount)
    }

    // Refreshing browsing selections

    /**
     * Verifies that refreshing browsing selections replaces the selected
     * campsite with its updated data and recalculates image counts while
     * remaining on the gallery page if promotional images still exist.
     */
    @Test
    fun refreshSelections_updatesSelectedCampsite() {
        loadTestData()

        model.showOwner(owner)
        model.showCampsite(pine)
        model.showPage(AdminImageBrowsePage.GALLERY)

        val updatedPine = Campsite(
            id = pine.id,
            name = pine.name,
            location = pine.location,
            ownerId = pine.ownerId,
            images = listOf("photo-2")
        )

        model.showCampsites(
            listOf(updatedPine, lake, emptyCampsite, adminCampsite)
        )

        model.refreshSelections()

        assertSame(updatedPine, model.selectedCampsite)
        assertEquals(listOf("photo-2"), model.selectedCampsite?.images)
        assertEquals(AdminImageBrowsePage.GALLERY, model.page)
        assertEquals(3, model.totalCampsiteImageCount)
        assertEquals(6, model.totalImageCount)
    }

    /**
     * Verifies that removing a campsite's last promotional image clears
     * the selected campsite and returns to the owner's campsite list
     * when the owner still has other promotional images.
     */
    @Test
    fun refreshSelections_lastCampsiteImageRemoved_returnsToCampsites() {
        loadTestData()

        model.showOwner(owner)
        model.showCampsite(lake)
        model.showPage(AdminImageBrowsePage.GALLERY)

        val updatedLake = Campsite(
            id = lake.id,
            name = lake.name,
            location = lake.location,
            ownerId = lake.ownerId,
            images = emptyList()
        )

        model.showCampsites(
            listOf(pine, updatedLake, emptyCampsite, adminCampsite)
        )

        model.refreshSelections()

        assertEquals(AdminImageBrowsePage.CAMPSITES, model.page)
        assertEquals(owner, model.selectedOwner)
        assertNull(model.selectedCampsite)
        assertEquals(listOf(pine), model.ownerCampsitesWithImages)
        assertEquals(3, model.totalCampsiteImageCount)
    }

    /**
     * Verifies that removing an owner's last promotional image clears
     * the selected owner and campsite and returns to the owner list.
     */
    @Test
    fun refreshSelections_lastOwnerImageRemoved_returnsToOwners() {
        loadTestData()

        model.showOwner(admin)
        model.showCampsite(adminCampsite)
        model.showPage(AdminImageBrowsePage.GALLERY)

        val updatedCampsite = Campsite(
            id = adminCampsite.id,
            name = adminCampsite.name,
            location = adminCampsite.location,
            ownerId = adminCampsite.ownerId,
            images = emptyList()
        )

        model.showCampsites(
            listOf(pine, lake, emptyCampsite, updatedCampsite)
        )

        model.refreshSelections()

        assertEquals(AdminImageBrowsePage.OWNERS, model.page)
        assertNull(model.selectedOwner)
        assertNull(model.selectedCampsite)
        assertEquals(listOf(owner), model.ownersWithImages)
        assertEquals(1, model.ownersWithImagesCount)
        assertEquals(3, model.totalCampsiteImageCount)
    }

    /**
     * Verifies that removing a user's profile image clears the selected
     * profile, excludes the user from browsing results and updates
     * profile and overall image counts.
     */
    @Test
    fun refreshSelections_profileImageRemoved_clearsSelectedProfile() {
        loadTestData()

        model.showProfile(admin)
        model.showPage(AdminImageBrowsePage.PROFILES)

        model.showProfileImages(
            model.profileImages - admin.getUid()
        )

        model.refreshSelections()

        assertNull(model.selectedProfile)
        assertEquals(AdminImageBrowsePage.PROFILES, model.page)
        assertFalse(model.usersWithImages.contains(admin))
        assertEquals(2, model.usersWithImagesCount)
        assertEquals(6, model.totalImageCount)
    }

    /**
     * Verifies that refreshing browsing selections preserves existing
     * search queries, valid selections and the current browsing page.
     */
    @Test
    fun refreshSelections_preservesSearchQueries() {
        loadTestData()

        model.showOwner(owner)
        model.showCampsite(pine)
        model.showPage(AdminImageBrowsePage.GALLERY)

        model.searchOwners("Daniel")
        model.searchCampsites("Pine")
        model.searchProfiles("Alex")

        model.refreshSelections()

        assertEquals("Daniel", model.ownerSearch)
        assertEquals("Pine", model.campsiteSearch)
        assertEquals("Alex", model.profileSearch)

        assertEquals(owner, model.selectedOwner)
        assertEquals(pine, model.selectedCampsite)
        assertEquals(AdminImageBrowsePage.GALLERY, model.page)
    }

    // Resetting browsing state

    /**
     * Verifies that resetting the model clears all loaded data,
     * image references, selections, search queries, loading state
     * and errors, and returns to the categories page.
     */
    @Test
    fun reset_restoresInitialState() {
        loadTestData()

        model.showOwner(owner)
        model.showCampsite(pine)
        model.showProfile(admin)
        model.showPage(AdminImageBrowsePage.GALLERY)

        model.searchOwners("Daniel")
        model.searchCampsites("Pine")
        model.searchProfiles("Alex")

        model.showLoading(true)
        model.showError("Example error")

        model.reset()

        assertEquals(AdminImageBrowsePage.CATEGORIES, model.page)

        assertTrue(model.users.isEmpty())
        assertTrue(model.campsites.isEmpty())
        assertTrue(model.profileImages.isEmpty())

        assertNull(model.selectedOwner)
        assertNull(model.selectedCampsite)
        assertNull(model.selectedProfile)

        assertEquals("", model.ownerSearch)
        assertEquals("", model.campsiteSearch)
        assertEquals("", model.profileSearch)

        assertFalse(model.isLoading)
        assertNull(model.errorMessage)
        assertEquals(0, model.totalImageCount)
    }
}

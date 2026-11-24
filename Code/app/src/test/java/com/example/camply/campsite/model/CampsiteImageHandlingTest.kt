package com.example.camply.campsite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteImageHandlingTest {
    private val img1 = "photo1"
    private val img2 = "photo2"
    private val img3 = "photo3"

    // test for no images for a campsite
    @Test
    fun emptyImageSection(){
        assertTrue(Campsite(name = "Banff Pines").images.isEmpty())

    }

    // test for one image for a campsite
    @Test
    fun singleImageForCampsite(){
        assertEquals(listOf(img2), Campsite(images = listOf(img2)).images)

    }
    // test for multiple images for a campsite
    @Test
    fun multipleImagesForCampsite(){
        assertEquals(listOf(img1, img2, img3), Campsite(images = listOf(img1, img2, img3)).images)
    }

    //test to check if duplicates are rejected
    @Test
    fun rejectDuplicateImages(){
        val campsite = Campsite(images = listOf(img3))
        assertFalse(campsite.addImage(img3))
        assertEquals(listOf(img3), campsite.images)
    }

    // test for adding new image to the list
    @Test
    fun addNewImageToList(){
        val campsite = Campsite(images = listOf(img2))
        assertTrue(campsite.addImage(img3))
        assertEquals(listOf(img2, img3), campsite.images)
    }

    // test to check if only the selected image is deleted
    @Test
    fun removeSelectedImage(){
        val campsite = Campsite(images = listOf(img1,img2,img3))
        campsite.removeImage(img2)
        assertEquals(listOf(img1, img3), campsite.images)
    }

    // Test that blank photo IDs cannot be added
    @Test
    fun rejectBlankImageId() {
        val campsite = Campsite()

        assertFalse(campsite.addImage(""))
        assertFalse(campsite.addImage("   "))
        assertTrue(campsite.images.isEmpty())
    }

    // Test that a campsite can have up to five images
    @Test
    fun allowMaximumFiveImages() {
        val campsite = Campsite()

        assertTrue(campsite.addImage("photo1"))
        assertTrue(campsite.addImage("photo2"))
        assertTrue(campsite.addImage("photo3"))
        assertTrue(campsite.addImage("photo4"))
        assertTrue(campsite.addImage("photo5"))

        assertEquals(Campsite.MAX_IMAGES, campsite.images.size)
    }

    // Test that adding a sixth image is rejected
    @Test
    fun rejectMoreThanFiveImages() {
        val campsite = Campsite(
            images = listOf("photo1", "photo2", "photo3", "photo4", "photo5")
        )

        assertFalse(campsite.addImage("photo6"))
        assertEquals(5, campsite.images.size)
        assertFalse("photo6" in campsite.images)
    }

    // Test that a new image can be added after removing one
    @Test
    fun addImageAfterRemoval() {
        val campsite = Campsite(
            images = listOf("photo1", "photo2", "photo3", "photo4", "photo5")
        )

        assertTrue(campsite.removeImage("photo3"))
        assertTrue(campsite.addImage("photo6"))

        assertEquals(
            listOf("photo1", "photo2", "photo4", "photo5", "photo6"),
            campsite.images
        )
    }

    // Test that removing a nonexistent image returns false
    @Test
    fun rejectRemovingNonexistentImage() {
        val campsite = Campsite(images = listOf("photo1", "photo2"))

        assertFalse(campsite.removeImage("photo3"))
        assertEquals(listOf("photo1", "photo2"), campsite.images)
    }

    // Creates a valid campsite to test image validation
    private fun validCampsite(images: List<String> = emptyList()): Campsite {
        return Campsite(
            name = "Banff Pines",
            location = "Banff",
            description = "A peaceful forest campsite",
            rules = "No open fires",
            cancellationPolicy = "Cancel at least 24 hours before arrival",
            maxOccupancy = 4,
            price = 25.0,
            images = images,
            latitude = 51.1784,
            longitude = -115.5708
        )
    }

    // Test that images remain optional
    @Test
    fun validationAllowsNoImages() {
        val campsite = validCampsite()

        assertTrue(campsite.validationCheck().isEmpty())
    }

    // Test that validation accepts five unique image IDs
    @Test
    fun validationAllowsFiveImages() {
        val campsite = validCampsite(
            listOf("photo1", "photo2", "photo3", "photo4", "photo5")
        )

        assertTrue(campsite.validationCheck().isEmpty())
    }

    // Test that validation rejects more than five image IDs
    @Test
    fun validationRejectsTooManyImages() {
        val campsite = validCampsite(
            listOf("photo1", "photo2", "photo3", "photo4", "photo5", "photo6")
        )

        assertTrue(campsite.validationCheck().contains("A campsite can have at most ${Campsite.MAX_IMAGES} photos"))
    }

    // Test that validation rejects duplicate image IDs
    @Test
    fun validationRejectsDuplicateImages() {
        val campsite = validCampsite(
            listOf("photo1", "photo2", "photo1")
        )

        assertTrue(campsite.validationCheck().any { it.contains("unique") })
    }

    // Test that validation rejects blank image IDs
    @Test
    fun validationRejectsBlankImageIds() {
        val campsite = validCampsite(
            listOf("photo1", " ", "photo3")
        )

        assertTrue(campsite.validationCheck().any { it.contains("non") })
    }


    @Test
    fun validationRejectsMissingMapPin() {
        val campsite = validCampsite()
        campsite.latitude = null
        campsite.longitude = null

        assertTrue(
            campsite.validationCheck()
                .contains("Please select a campsite location on the map")
        )
    }

    @Test
    fun validationRejectsIncompleteMapPin() {
        val campsite = validCampsite()
        campsite.longitude = null

        assertTrue(
            campsite.validationCheck()
                .contains("Campsite latitude and longitude must both be provided")
        )
    }

    @Test
    fun validationRejectsInvalidCoordinates() {
        val campsite = validCampsite()
        campsite.latitude = 95.0
        campsite.longitude = -200.0

        val errors = campsite.validationCheck()

        assertTrue(errors.contains("Please enter a valid latitude"))
        assertTrue(errors.contains("Please enter a valid longitude"))
    }

}
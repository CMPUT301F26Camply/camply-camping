package com.example.camply.campsite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteImageHandlingTest {
    private val img1 = "https://testImg1/img1.jpg"
    private val img2 = "https://testImg2/img2.png"
    private val img3 = "https://testImg3/img3.jpg"

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

}
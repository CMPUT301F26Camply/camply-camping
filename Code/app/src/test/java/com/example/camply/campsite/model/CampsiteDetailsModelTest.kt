package com.example.camply.campsite.model
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test

class CampsiteDetailsModelTest {
    private lateinit var model: CampsiteDetailModel
    private val banff = Campsite(
        id = "1", name = "Banff Pines", location = "Banff"
    )
    private val jasper = Campsite(
        id = "2", name = "Jasper Lakeside", location = "Jasper"
    )

    // Set up campsiteDetails model
    @Before
    fun setUp() {
        model = CampsiteDetailModel()
    }

    // this testcase should show and store that campsite
    @Test
    fun showCampsite_and_storeCampsite() {
        model.showCampsite(banff)

        assertEquals(banff, model.campsite)
        assertFalse(model.isLoading)

    }
    // if user switches between campsite, the first one should be replaced by the next
    @Test
    fun switchCampsite() {
        model.showCampsite(banff)
        model.showCampsite(jasper)

        assertEquals(jasper, model.campsite)
    }

    // if campsite details fail to load
    @Test
    fun campsiteNotFound() {
        model.showCampsite(banff)
        model.showError("Campsite Not Found") // store the error message
        assertNull(model.campsite)// erase old campsite details
        assertEquals("Campsite Not Found", model.errorMessage)
        assertFalse(model.isLoading) //stop loading

    }

}
package com.example.camply

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camply.campsite.controller.CampsiteDetailController
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import com.example.camply.campsite.service.PreparedCampsiteImages
import com.example.camply.campsite.view.CampsiteDetailView
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CampsiteDetailViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var model: CampsiteDetailModel
    private lateinit var repository: MockCampsiteRepository
    private lateinit var controller: CampsiteDetailController
    private lateinit var view: CampsiteDetailView

    private val banff = Campsite(
        id = "abc123",
        name = "Banff Pines",
        location = "Banff",
        description = "Lakeside sites",
        maxOccupancy = 6,
        price = 45.0,
        rules = "Quiet hours 10pm",
        cancellationPolicy = "48 hours notice"
    )

    @Before
    fun setUp() {
        model = CampsiteDetailModel()
        repository = MockCampsiteRepository()
        controller = CampsiteDetailController(model, repository)
        view = CampsiteDetailView(model, controller)
    }

    private fun show(
        campsiteId: String = "abc123",
        showQrButton: Boolean = false,
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                view.Display(
                    campsiteId = campsiteId,
                    onBack = onBack,
                    showQRbutton = showQrButton
                )
            }
        }
    }

    private fun waitForCampsite() {
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Banff Pines")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // The details screen shows the campsite details
    @Test
    fun detailsScreen_showsCampsiteInformation() {
        repository.stored += banff
        show()
        waitForCampsite()

        assertTrue(
            composeRule.onAllNodesWithText("Banff", ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithText("Up to 6 guests").assertExists()
        composeRule.onNodeWithText("$45").assertExists()
        composeRule.onNodeWithText("per night").assertExists()
        composeRule.onNodeWithText("Not yet rated").assertExists()
        composeRule.onNodeWithText("Lakeside sites").assertExists()
        composeRule.onNodeWithText("Quiet hours 10pm").assertExists()
        composeRule.onNodeWithText("48 hours notice").assertExists()
    }

    // A failed load shows the retry option
    @Test
    fun failedLoad() {
        repository.fail = true
        show()

        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Try again").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    // The Back button calls onBack
    @Test
    fun backButton_callsOnBack() {
        repository.stored += banff
        var backCalled = false
        show(onBack = { backCalled = true })
        waitForCampsite()

        composeRule.onNodeWithText("Back").performClick()

        assertTrue(backCalled)
    }

    // The QR button is hidden unless the caller asks for it
    @Test
    fun qrButton_hiddenByDefault() {
        repository.stored += banff
        show(showQrButton = false)
        waitForCampsite()

        composeRule.onNodeWithContentDescription("Generate QR code").assertDoesNotExist()
    }

    // Tapping the QR button opens the dialog, and Close dismisses it
    @Test
    fun qrButton_opensAndCloseTest() {
        repository.stored += banff
        show(showQrButton = true)
        waitForCampsite()

        composeRule.onNodeWithContentDescription("QR code for Banff Pines").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Generate QR code").performClick()
        composeRule.onNodeWithContentDescription("QR code for Banff Pines").assertIsDisplayed()
        composeRule.onAllNodesWithText("Banff Pines").assertCountEquals(2)

        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithContentDescription("QR code for Banff Pines").assertDoesNotExist()
    }

    // Scanning the QR code gives the campsite link
    @Test
    fun generateQrCode_containsCampsiteLink() {
        controller.generateQRcode(banff)

        assertEquals("camply://campsite/abc123", decode())
    }

    // Each campsite gets a different QR code
    @Test
    fun generateQrCode_isUniquePerCampsite() {
        controller.generateQRcode(banff)
        val first = decode()

        controller.generateQRcode(Campsite(id = "xyz789", name = "Jasper Lakeside"))
        val second = decode()

        assertFalse(first == second)
        assertEquals("camply://campsite/xyz789", second)
    }

    private fun decode(): String {
        val image = model.qrCode!!
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        val source = RGBLuminanceSource(image.width, image.height, pixels)
        return MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    private class MockCampsiteRepository : CampsiteRepository {
        val stored = mutableListOf<Campsite>()
        var fail = false

        override suspend fun getCampsites(): List<Campsite> = stored

        override suspend fun getCampsite(id: String): Campsite? {
            if (fail) throw RuntimeException("offline")
            return stored.find { it.id == id }
        }

        override suspend fun getCampsitesByOwner(ownerId: String): List<Campsite> =
            stored.filter { it.ownerId == ownerId }

        override suspend fun saveCampsite(
            campsite: Campsite,
            photosToSave: Map<String, PreparedCampsiteImages>,
            photosToRemove: List<String>
        ): String = error("not used")

        override suspend fun deleteCampsite(id: String) = error("not used")

        override suspend fun getCampsitePreview(campsiteId: String, photoId: String): ByteArray? = null

        override suspend fun getCampsitePhoto(campsiteId: String, photoId: String): ByteArray? = null
    }
}
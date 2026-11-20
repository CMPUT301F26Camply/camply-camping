package com.example.camply.user.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File

/**
 * Instrumented tests for profile image processing (US 01.08.02).
 *
 * Checks image resizing, compression, and invalid image handling.
 */
class ProfileImageServiceTest {

    private val imageService = ProfileImageService()


    /**
     * Tests that a valid image is resized while preserving its aspect ratio
     * and compressed to fit within the 100 KB size limit.
     */
    @Test
    fun prepareImage_validImage_resizesAndCompresses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "test_profile_image.png")

        // Create a 1200 x 800 image for testing.
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)

        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()

        try {
            val imageBytes = requireNotNull(imageService.prepareImage(context, Uri.fromFile(file)))

            // Check that the processed image is under 100 KB
            assertTrue(imageBytes.size <= 100000)

            // Check that the image was resized
            val resizedBitmap = requireNotNull(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size))

            assertEquals(256, maxOf(resizedBitmap.width, resizedBitmap.height))

            // Check that the original aspect ratio was preserved
            val aspectRatio = resizedBitmap.width.toFloat() / resizedBitmap.height
            assertEquals(1.5f, aspectRatio, 0.02f)

            resizedBitmap.recycle()
        } finally {
            file.delete()
        }
    }

    /**
     * Checks that invalid image contents are rejected
     */
    @Test
    fun prepareImage_invalidImage_returnsNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "invalid_profile_image.txt")

        file.writeText("This is not an image")

        try {
            val result = imageService.prepareImage(
                context, Uri.fromFile(file)
            )

            assertNull(result)
        } finally {
            file.delete()
        }
    }

    /**
     * Checks that a missing image file is handled without crashing
     */
    @Test
    fun prepareImage_missingImage_returnsNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "missing_profile_image.png")

        file.delete()

        val result = imageService.prepareImage(
            context, Uri.fromFile(file)
        )

        assertNull(result)
    }
}
package com.example.camply.campsite.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random



// AI Usage Declaration
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Provider: OpenAI
// Tool: ChatGPT 6
// Date Used: November 21, 2026
//
// Prompt:
// "Given my CampsiteImageService.kt implementation and the existing ProfileImageServiceTest.kt
// file as a reference, can you help me write Android instrumented test cases for CampsiteImageService.kt?
// The tests should verify that both full and preview JPEG images are generated correctly, respect
// their respective dimension and file size limits, and preserve the original aspect ratio. I also
// want to test image orientation correction, compression of more complex images (so random color
// pixels in the image, instead of having it be a full blue image), handling of invalid or missing
// images, and ensuring smaller images are not enlarged. Please follow the existing testing
// conventions and avoid introducing unnecessary libraries or complexity."
//
// Purpose:
// Used ChatGPT to assist with writing Android instrumented tests for CampsiteImageService.kt
// to verify image processing, compression, resizing, orientation correction, and error handling.
//
// Extent of Use:
// Substantive assistance with implementing instrumented test cases, including generating temporary
// test images, verifying JPEG output, dimensions, file sizes, aspect ratios, orientation, and handling
// invalid images. The generated tests were reviewed and adjusted for the project.

/**
 * Instrumented tests for campsite promotional image processing (US 02.01.02).
 *
 * Checks full and preview image generation, resizing, compression,
 * aspect ratio, orientation, and invalid image handling.
 */
class CampsiteImageServiceTest {

    private val imageService = CampsiteImageService()

    /**
     * Creates a test image with the specified dimensions and format.
     */
    private fun createTestImage(
        file: File,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)

        try {
            file.outputStream().use {
                bitmap.compress(format, 100, it)
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Decodes a processed image and verifies its JPEG format and byte limit.
     */
    private fun checkProcessedImage(bytes: ByteArray, maxBytes: Int): Bitmap {
        assertTrue(bytes.isNotEmpty())
        assertTrue(bytes.size <= maxBytes)

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        assertEquals("image/jpeg", options.outMimeType)

        return requireNotNull(
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        )
    }

    /**
     * Tests that both image versions are generated within their
     * dimension and byte limits while preserving aspect ratio.
     */
    @Test
    fun prepareImage_validImage_createsBothVersions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "test_campsite_image.png")

        createTestImage(file, 1200, 800)

        try {
            val result = requireNotNull(
                imageService.prepareImage(context, Uri.fromFile(file))
            )

            val fullBitmap = checkProcessedImage(
                result.fullImage,
                CampsiteImageService.FULL_IMAGE_MAX_BYTES
            )

            val previewBitmap = checkProcessedImage(
                result.previewImage,
                CampsiteImageService.PREVIEW_IMAGE_MAX_BYTES
            )

            try {
                // Check maximum dimensions
                assertEquals(1024, maxOf(fullBitmap.width, fullBitmap.height))
                assertEquals(800, maxOf(previewBitmap.width, previewBitmap.height))

                // Check that both versions preserve the original 3:2 ratio
                val fullRatio = fullBitmap.width.toFloat() / fullBitmap.height
                val previewRatio = previewBitmap.width.toFloat() / previewBitmap.height

                assertEquals(1.5f, fullRatio, 0.02f)
                assertEquals(1.5f, previewRatio, 0.02f)
            } finally {
                fullBitmap.recycle()
                previewBitmap.recycle()
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Tests that smaller images are not unnecessarily enlarged.
     */
    @Test
    fun prepareImage_smallImage_preservesDimensions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "small_campsite_image.png")

        createTestImage(file, 320, 240)

        try {
            val result = requireNotNull(
                imageService.prepareImage(context, Uri.fromFile(file))
            )

            val fullBitmap = checkProcessedImage(
                result.fullImage,
                CampsiteImageService.FULL_IMAGE_MAX_BYTES
            )

            val previewBitmap = checkProcessedImage(
                result.previewImage,
                CampsiteImageService.PREVIEW_IMAGE_MAX_BYTES
            )

            try {
                assertEquals(320, fullBitmap.width)
                assertEquals(240, fullBitmap.height)

                assertEquals(320, previewBitmap.width)
                assertEquals(240, previewBitmap.height)
            } finally {
                fullBitmap.recycle()
                previewBitmap.recycle()
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Tests that camera orientation metadata is applied before resizing.
     *
     * A landscape image marked for 90-degree rotation should
     * produce correctly oriented portrait images.
     */
    @Test
    fun prepareImage_rotatedImage_correctsOrientation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "rotated_campsite_image.jpg")

        createTestImage(file, 1200, 800, Bitmap.CompressFormat.JPEG)

        try {
            // Simulate a camera image stored with rotation metadata
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString()
            )
            exif.saveAttributes()

            val result = requireNotNull(
                imageService.prepareImage(context, Uri.fromFile(file))
            )

            val fullBitmap = checkProcessedImage(
                result.fullImage,
                CampsiteImageService.FULL_IMAGE_MAX_BYTES
            )

            val previewBitmap = checkProcessedImage(
                result.previewImage,
                CampsiteImageService.PREVIEW_IMAGE_MAX_BYTES
            )

            try {
                // Both versions should now be portrait-oriented
                assertTrue(fullBitmap.height > fullBitmap.width)
                assertTrue(previewBitmap.height > previewBitmap.width)

                assertEquals(1024, fullBitmap.height)
                assertEquals(800, previewBitmap.height)

                // Portrait aspect ratio should be approximately 2:3
                assertEquals(
                    2f / 3f,
                    fullBitmap.width.toFloat() / fullBitmap.height,
                    0.02f
                )

                assertEquals(
                    2f / 3f,
                    previewBitmap.width.toFloat() / previewBitmap.height,
                    0.02f
                )
            } finally {
                fullBitmap.recycle()
                previewBitmap.recycle()
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Tests compression using a complex image with many different pixels.
     *
     * This verifies that both versions remain within their byte limits
     * even when the image is more difficult to compress.
     */
    @Test
    fun prepareImage_complexImage_respectsByteLimits() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "complex_campsite_image.png")

        val bitmap = Bitmap.createBitmap(
            1400, 900, Bitmap.Config.ARGB_8888
        )

        val random = Random(1234L)
        val pixels = IntArray(1400)

        try {
            for (y in 0 until bitmap.height) {
                for (x in pixels.indices) {
                    pixels[x] = Color.rgb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256)
                    )
                }

                bitmap.setPixels(pixels, 0, 1400, 0, y, 1400, 1)
            }

            file.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally {
            bitmap.recycle()
        }

        try {
            val result = requireNotNull(
                imageService.prepareImage(context, Uri.fromFile(file))
            )

            val fullBitmap = checkProcessedImage(
                result.fullImage,
                CampsiteImageService.FULL_IMAGE_MAX_BYTES
            )

            val previewBitmap = checkProcessedImage(
                result.previewImage,
                CampsiteImageService.PREVIEW_IMAGE_MAX_BYTES
            )

            try {
                assertTrue(
                    maxOf(fullBitmap.width, fullBitmap.height) <=
                            CampsiteImageService.FULL_IMAGE_MAX_DIMENSION
                )

                assertTrue(
                    maxOf(previewBitmap.width, previewBitmap.height) <=
                            CampsiteImageService.PREVIEW_IMAGE_MAX_DIMENSION
                )
            } finally {
                fullBitmap.recycle()
                previewBitmap.recycle()
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Tests that a file containing invalid image data is rejected.
     */
    @Test
    fun prepareImage_invalidImage_returnsNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "invalid_campsite_image.txt")

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
     * Tests that a missing image file is handled without crashing.
     */
    @Test
    fun prepareImage_missingImage_returnsNull() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "missing_campsite_image.png")

        file.delete()

        val result = imageService.prepareImage(
            context, Uri.fromFile(file)
        )

        assertNull(result)
    }
}
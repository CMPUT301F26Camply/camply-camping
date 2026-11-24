
package com.example.camply.campsite.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// AI Usage Declaration
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Provider: OpenAI
// Tool: ChatGPT 6
// Date Used: November 21, 2026
//
// Prompt:
// "I need to implement campsite promotional image uploading using Firestore without Firebase
// Storage. Can you help me create a Kotlin service similar to my existing ProfileImageService
// that takes a Context and Uri from the Android photo picker and generates two compressed JPEG
// versions of the selected image? The full image should have a maximum dimension of 1024px and
// be under 400 KB, while the smaller display image should have a maximum dimension of 800px and
// be under 150 KB. Both versions should preserve the original aspect ratio and handle image
// orientation correctly. Please keep the implementation straightforward without introducing
// unnecessary libraries or complexity."
//
// Purpose:
// Used ChatGPT to assist with implementing a service for reading, resizing, and compressing
// campsite promotional images into two JPEG versions.
//
// Extent of Use:
// Substantive assistance with implementing the image processing logic, including bitmap decoding,
// resizing, JPEG compression, image orientation, memory management, and error handling.
// The generated code was reviewed and adjusted for the project.

/**
 * Holds the two compressed versions of a campsite photo.
 *
 * @param fullImage full-resolution JPEG bytes
 * @param previewImage smaller display JPEG bytes
 */
class PreparedCampsiteImages(
    val fullImage: ByteArray,
    val previewImage: ByteArray
)

/**
 * Processes campsite promotional images selected from the device.
 *
 * Generates a full-resolution image and a smaller display image
 * while preserving aspect ratio and correcting image orientation.
 *
 * The processed JPEG bytes can be stored as Firestore Blobs.
 */
class CampsiteImageService {

    companion object {
        const val FULL_IMAGE_MAX_DIMENSION = 1024
        const val FULL_IMAGE_MAX_BYTES = 400_000

        const val PREVIEW_IMAGE_MAX_DIMENSION = 800
        const val PREVIEW_IMAGE_MAX_BYTES = 150_000

        private const val MAX_DECODE_DIMENSION = 2048
    }

    /**
     * Reads and processes a selected campsite photo.
     *
     * Generates both image versions from the selected original.
     * Large images are sampled before decoding to reduce memory usage.
     *
     * This operation should be called off the main thread.
     *
     * @param context Android context used to access the selected image
     * @param uri URI of the selected image
     * @return both compressed JPEG versions, or null if processing fails
     */
    fun prepareImage(context: Context, uri: Uri): PreparedCampsiteImages? {
        try {
            // Read image dimensions without loading the full image.
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true

            val boundsInput = context.contentResolver.openInputStream(uri)
                ?: return null

            boundsInput.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }

            // Reduce memory usage when decoding large images.
            var sampleSize = 1

            while (
                bounds.outWidth / sampleSize > MAX_DECODE_DIMENSION ||
                bounds.outHeight / sampleSize > MAX_DECODE_DIMENSION
            ) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize

            val imageInput = context.contentResolver.openInputStream(uri)
                ?: return null

            val bitmap = imageInput.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            try {
                // Correct the orientation of photos taken with a camera.
                val orientation = readOrientation(context, uri)
                val orientedBitmap = correctOrientation(bitmap, orientation)

                try {
                    // Prepare both versions from the same image.
                    val fullBitmap = resizeBitmap(
                        orientedBitmap,
                        FULL_IMAGE_MAX_DIMENSION
                    )

                    try {
                        val previewBitmap = resizeBitmap(
                            orientedBitmap,
                            PREVIEW_IMAGE_MAX_DIMENSION
                        )

                        try {
                            val fullBytes = compressImage(
                                fullBitmap,
                                FULL_IMAGE_MAX_BYTES
                            ) ?: return null

                            val previewBytes = compressImage(
                                previewBitmap,
                                PREVIEW_IMAGE_MAX_BYTES
                            ) ?: return null

                            return PreparedCampsiteImages(
                                fullImage = fullBytes,
                                previewImage = previewBytes
                            )
                        } finally {
                            if (previewBitmap !== orientedBitmap) {
                                previewBitmap.recycle()
                            }
                        }
                    } finally {
                        if (fullBitmap !== orientedBitmap) {
                            fullBitmap.recycle()
                        }
                    }
                } finally {
                    if (orientedBitmap !== bitmap) {
                        orientedBitmap.recycle()
                    }
                }
            } finally {
                bitmap.recycle()
            }

        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Resizes a bitmap without changing its aspect ratio.
     *
     * Images smaller than the requested maximum are not enlarged.
     *
     * @param bitmap source bitmap
     * @param maxDimension maximum width or height
     * @return resized bitmap, or the original if resizing is unnecessary
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestDimension = maxOf(bitmap.width, bitmap.height)

        if (largestDimension <= maxDimension) {
            return bitmap
        }

        val scaleFactor = maxDimension.toFloat() / largestDimension

        val width = maxOf(1, (bitmap.width * scaleFactor).roundToInt())
        val height = maxOf(1, (bitmap.height * scaleFactor).roundToInt())

        return bitmap.scale(width, height)
    }

    /**
     * Compresses a bitmap into a JPEG within the specified byte limit.
     *
     * First reduces JPEG quality. If necessary, reduces dimensions
     * further until the image fits the limit.
     *
     * @param bitmap bitmap to compress
     * @param maxBytes maximum number of compressed bytes
     * @return JPEG bytes, or null if compression fails
     */
    private fun compressImage(bitmap: Bitmap, maxBytes: Int): ByteArray? {
        var currentBitmap = bitmap
        val output = ByteArrayOutputStream()

        try {
            repeat(6) {
                var quality = 90

                while (quality >= 30) {
                    output.reset()

                    val successful = currentBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        output
                    )

                    if (!successful) {
                        return null
                    }

                    if (output.size() <= maxBytes) {
                        return output.toByteArray()
                    }

                    quality -= 10
                }

                // Reduce dimensions if quality reduction was insufficient.
                if (maxOf(currentBitmap.width, currentBitmap.height) <= 256) {
                    return null
                }

                val width = maxOf(1, (currentBitmap.width * 0.85f).roundToInt())
                val height = maxOf(1, (currentBitmap.height * 0.85f).roundToInt())

                val smallerBitmap = currentBitmap.scale(width, height)

                if (currentBitmap !== bitmap) {
                    currentBitmap.recycle()
                }

                currentBitmap = smallerBitmap
            }

            return null
        } finally {
            if (currentBitmap !== bitmap) {
                currentBitmap.recycle()
            }
        }
    }

    /**
     * Reads camera orientation metadata from a selected image.
     *
     * Returns normal orientation when metadata is unavailable.
     */
    private fun readOrientation(context: Context, uri: Uri): Int {
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return ExifInterface.ORIENTATION_NORMAL

            input.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Applies the image's EXIF rotation or reflection when necessary.
     *
     * @param bitmap source bitmap
     * @param orientation EXIF orientation value
     * @return corrected bitmap, or the original if no correction is needed
     */
    private fun correctOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                matrix.setScale(-1f, 1f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.setRotate(180f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                matrix.setScale(1f, -1f)

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.setRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.setRotate(270f)

            else -> return bitmap
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}

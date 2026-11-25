
package com.example.camply.user.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale


// AI Usage Declaration
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Provider: OpenAI
// Tool: ChatGPT 6
// Date Used: November 20, 2026
//
// Prompt:
// "I need to implement profile image uploading using Firestore without Firebase Storage.
// Can you help me create a Kotlin service that takes a Context and Uri from the Android
// photo picker, reads the selected image, resizes it while maintaining its aspect ratio,
// and compresses it into a JPEG under 100 KB before returning it as a ByteArray?
// Please follow standard Android and Kotlin coding conventions, and keep the
// implementation straightforward"
//
// Purpose:
// Used ChatGPT to assist with implementing a service for reading, resizing,
// and compressing selected profile images before saving them in Firestore.
//
// Extent of Use:
// Substantive assistance with implementing the image processing logic,
// including bitmap decoding, resizing, JPEG compression, and error handling.
// The generated code was reviewed and adjusted for the project.

/**
 * Processes profile images selected from the device
 *
 * Resizes and compresses images before they are saved in Firestore
 */
class ProfileImageService {

    /**
     * Reads, resizes, and compresses a selected profile image
     *
     * @param context the Android context used to access the selected image
     * @param uri the URI of the image selected by the user
     * @return compressed image bytes, or null if processing fails
     */
    fun prepareImage(context: Context, uri: Uri): ByteArray? {
        try {
            // Read the image dimensions without loading the full image.
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true

            val boundsInput = context.contentResolver.openInputStream(uri) ?: return null

            boundsInput.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }

            // Reduce memory usage when reading large images.
            var sampleSize = 1

            while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize

            val imageInput = context.contentResolver.openInputStream(uri) ?: return null

            val bitmap = imageInput.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // Resize while preserving the image's proportions.
            val largestDimension = maxOf(bitmap.width, bitmap.height)

            val width = maxOf(1, bitmap.width * 256 / largestDimension)
            val height = maxOf(1, bitmap.height * 256 / largestDimension)

            val resizedBitmap = bitmap.scale(width, height)

            // Compress until the image fits our 100 KB limit.
            val output = ByteArrayOutputStream()
            var quality = 85

            while (quality >= 25) {
                output.reset()

                val successful = resizedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    output
                )

                if (!successful) return null

                if (output.size() <= 100_000) {
                    return output.toByteArray()
                }

                quality -= 15
            }

            return null

        } catch (e: Exception) {
            return null
        }
    }
}

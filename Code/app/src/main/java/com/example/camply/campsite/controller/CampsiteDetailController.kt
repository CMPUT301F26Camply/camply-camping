package com.example.camply.campsite.controller

import androidx.compose.ui.graphics.asImageBitmap
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlin.coroutines.cancellation.CancellationException

/** Loads a single campsite for the details screen (US 01.01.05). */
class CampsiteDetailController(
    private val model: CampsiteDetailModel,
    private val repository: CampsiteRepository
) {
    suspend fun loadCampsite(campsiteId: String) {
        model.showLoading()
        try {
            val campsite = repository.getCampsite(campsiteId)
            if (campsite != null) model.showCampsite(campsite) else model.showError("Campsite not found.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            model.showError(e.message ?: "Could not load campsite.")
        }
    }

    /**
     * Re-reads the campsite without the loading state, so the screen keeps its place
     * and sections such as ratings and reviews are not rebuilt. Used after an entrant's rating
     * changes the average shown in the header (US 01.13.01). A failed refresh keeps what is shown.
     *
     * @param campsiteId The campsite's Firestore document ID.
     */
    suspend fun refreshCampsite(campsiteId: String) {
        try {
            repository.getCampsite(campsiteId)?.let(model::showCampsite)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The details already on screen stay valid, so a failed refresh is not an error.
        }
    }

    /**
     * Function to generate QR code for each campsite.
     * The code holds the link "camply://campsite/<campsite id>". The id is the campsite's
     * Firestore document id, so every registered campsite gets a different code.
     * Scanning the link opens that campsite's details screen.
     * @param campsite which is the selected campsite
     * Covers US 02.02.01*/
    fun generateQRcode(campsite: Campsite){
        if (campsite.id.isBlank()){
            return
        }
        //Barcode Encoder Implementation: https://github.com/journeyapps/zxing-android-embedded
        val bitmap = BarcodeEncoder().encodeBitmap(
            "camply://campsite/${campsite.id}",
            BarcodeFormat.QR_CODE,
            512, 512
        )

        model.showQRcode(bitmap.asImageBitmap())
    }

    fun closeQRcode() = model.hideQRcode()
}
package com.example.camply.campsite.controller

import androidx.compose.ui.graphics.asImageBitmap
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.CampsiteRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.encoder.QRCode
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
     * Function to generate QR code for each campsite
     * @param campsite which is the selected campsite
     * Covers US 02.02.01*/
    fun generateQRcode(campsite: Campsite){
        if (campsite.id.isBlank()){
            return
        }

        val bitmap = BarcodeEncoder().encodeBitmap(
            "camply://campsite/${campsite.id}",
            BarcodeFormat.QR_CODE,
            512, 512
        )

        model.showQRcode(bitmap.asImageBitmap())
    }

    fun closeQRcode() = model.hideQRcode()
}
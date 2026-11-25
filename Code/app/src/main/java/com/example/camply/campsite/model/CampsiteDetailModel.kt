package com.example.camply.campsite.model

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/** State for the campsite details screen (US 01.01.05). */
class CampsiteDetailModel {

    var campsite: Campsite? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(true)
        private set
    var errorMessage: String? by mutableStateOf(null)
        private set

    /**
     * Holds the QR code image shown on the CampsiteDetailScreen.
     * US 02.02.01 (QR code for each campsite).
     */
    var qrCode: ImageBitmap? by mutableStateOf(null)
    private set

    fun showLoading() { campsite = null; isLoading = true; errorMessage = null }
    fun showCampsite(value: Campsite) { campsite = value; isLoading = false; errorMessage = null }
    fun showError(message: String) { campsite = null; isLoading = false; errorMessage = message }
    /**
     * US 02.02.01: QR code generation. Need to both show and hide qr code as necessary
     * @param image the QR code image to show
     */
    fun showQRcode(image: ImageBitmap){
        qrCode = image
    }
    fun hideQRcode(){
        qrCode = null
    }
}
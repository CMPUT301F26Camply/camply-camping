package com.example.camply

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.camply.ui.theme.CamplyTheme
import com.example.camply.ui.theme.CamplyThemePreset

/**
 * Main entry point for the Camply Android application.
 *
 * Initializes the activity and displays the Camply application.
 */
class MainActivity : ComponentActivity() {

    private var scannedCampsiteId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scannedCampsiteId = campsiteIdFrom(intent)
        setContent {
            CamplyApp(scannedCampsiteId = scannedCampsiteId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        scannedCampsiteId = campsiteIdFrom(intent)
    }

    private fun campsiteIdFrom(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "camply" || data.host != "campsite") {
            return null
        }
        return data.lastPathSegment
    }
}
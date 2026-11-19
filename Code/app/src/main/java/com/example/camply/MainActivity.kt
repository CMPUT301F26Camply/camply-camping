package com.example.camply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.camply.ui.theme.CamplyTheme
import com.example.camply.ui.theme.CamplyThemePreset

/**
 * Main entry point for the Camply Android application.
 *
 * Initializes the activity and displays the Camply application.
 */
class MainActivity : ComponentActivity() {

    /**
     * Creates the activity and sets up the application's Compose interface
     *
     * @param savedInstanceState the previously saved activity state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamplyTheme(
                preset = CamplyThemePreset.BLUE
            ) {
                CamplyApp()
            }
        }

    }
}
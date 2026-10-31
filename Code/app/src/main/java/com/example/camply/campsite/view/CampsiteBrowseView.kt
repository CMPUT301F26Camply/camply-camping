package com.example.camply.campsite.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Displays the campsite browsing screen.
 *
 * Currently serves as a placeholder until the campsite
 * browsing functionality is implemented.
 */
class CampsiteBrowseView {

    /**
     * Displays the campsite browsing placeholder.
     */
    @Composable
    fun Display() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Browse Campsites")
        }
    }
}
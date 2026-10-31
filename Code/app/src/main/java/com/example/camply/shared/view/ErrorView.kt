package com.example.camply.shared.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Displays an error screen when Camply encounters a problem
 */
class ErrorView {

    /**
     * Displays an error message in the center of the screen.
     */
    @Composable
    fun Display() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Error")
        }
    }
}
package com.example.camply.shared.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Reusable building blocks that match the storyboard styling.
 * Any phase can use these so every screen looks the same.
 */

object CamplyPalette {
    val Orange = Color(0xFFF29A4B)
    val Cream = Color(0xFFFCF1DD)
    val Black = Color(0xFF0B0B0B)
    val Danger = Color(0xFF9B5446)
    val GradientTop = Color(0xFFA67C52)
    val GradientBottom = Color(0xFF6F6A33)
    val Ink = Color(0xFF2A1A0B)
}

/** Gradient background + black pill title bar, like every storyboard screen. */
@Composable
fun CamplyScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CamplyPalette.GradientTop, CamplyPalette.GradientBottom)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(CamplyPalette.Black, RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp)
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun OrangeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CamplyPalette.Orange, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun CardLabel(text: String) = Text(
    text = text.uppercase(),
    fontSize = 10.sp,
    letterSpacing = 1.sp,
    color = CamplyPalette.Ink.copy(alpha = 0.7f)
)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CamplyPalette.Black, contentColor = Color.White,
            disabledContainerColor = CamplyPalette.Black.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text(text) }

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Button(
        onClick = onClick, shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CamplyPalette.Cream, contentColor = CamplyPalette.Ink),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text(text) }

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    Button(
        onClick = onClick, shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CamplyPalette.Danger, contentColor = Color.White),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text(text) }

@Composable
fun CenteredMessage(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Color.White, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(actionLabel, onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun camplyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CamplyPalette.Cream, unfocusedContainerColor = CamplyPalette.Cream,
    focusedTextColor = CamplyPalette.Ink, unfocusedTextColor = CamplyPalette.Ink,
    focusedBorderColor = CamplyPalette.Black, unfocusedBorderColor = Color.Transparent,
    cursorColor = CamplyPalette.Black
)

package com.example.camply.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Theme.kt - Camply Theme Redesign
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help modify the existing Jetpack Compose
// Material 3 theme system to support multiple color presets.
// This included assistance with structuring the color data
// class, defining preset color palettes, and updating the
// existing light and dark color schemes.
//
// Prompt:
// "How can I redesign my existing Android Jetpack Compose
// Theme.kt file to support four color themes (orange, green,
// blue and purple) instead of the original fixed colors?
// I want to keep the existing Material 3 theme structure,
// dark mode support and dynamic color functionality."
//
// Changes from the original implementation:
// - Added CamplyThemeColors to group theme colors.
// - Added getThemeColors() to define four color presets.
// - Updated CamplyTheme() to accept a color preset.
// - Replaced the original fixed light and dark color schemes
//   with color schemes based on the selected preset.
// - Disabled dynamic color by default.
//
// The existing Material 3 theme structure was retained.


/**
 * Stores the colors used by a Camply theme preset.
 *
 * @property primary Main branding and action color.
 * @property primaryContainer Background for selected elements.
 * @property background Background color used by screens.
 * @property surface Background color used by cards and fields.
 * @property text Main text color.
 * @property mutedText Secondary text color.
 * @property border Border color for interface elements.
 */
private data class CamplyThemeColors(
    val primary: Color,
    val primaryContainer: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val mutedText: Color,
    val border: Color
)

/**
 * Returns the colors for the specified Camply preset.
 *
 * @param preset The selected color theme.
 * @return The corresponding set of theme colors.
 */
private fun getThemeColors(
    preset: CamplyThemePreset
): CamplyThemeColors {
    return when (preset) {
        CamplyThemePreset.ORANGE -> CamplyThemeColors(
            primary = Color(0xFFC65B19),
            primaryContainer = Color(0xFFFFF0E4),
            background = Color(0xFFF8F7F4),
            surface = Color.White,
            text = Color(0xFF272522),
            mutedText = Color(0xFF756F69),
            border = Color(0xFFE8E3DD)
        )

        CamplyThemePreset.GREEN -> CamplyThemeColors(
            primary = Color(0xFF32684A),
            primaryContainer = Color(0xFFE4F0E8),
            background = Color(0xFFF6F8F5),
            surface = Color.White,
            text = Color(0xFF253027),
            mutedText = Color(0xFF68776C),
            border = Color(0xFFDDE7DE)
        )

        CamplyThemePreset.BLUE -> CamplyThemeColors(
            primary = Color(0xFF2767A5),
            primaryContainer = Color(0xFFE4F0FC),
            background = Color(0xFFF6F8FC),
            surface = Color.White,
            text = Color(0xFF252F3C),
            mutedText = Color(0xFF67778A),
            border = Color(0xFFDFE5EF)
        )

        CamplyThemePreset.PURPLE -> CamplyThemeColors(
            primary = Color(0xFF7652B5),
            primaryContainer = Color(0xFFF1EAFB),
            background = Color(0xFFFAF8FC),
            surface = Color.White,
            text = Color(0xFF2C2636),
            mutedText = Color(0xFF786E84),
            border = Color(0xFFE8E0F0)
        )
    }
}

/**
 * Applies the selected Camply theme to the application.
 *
 * @param darkTheme Whether the dark theme is enabled.
 * @param dynamicColor Whether Android dynamic colors are enabled.
 * @param preset The Camply color preset to apply.
 * @param content Composable content displayed within the theme.
 */
@Composable
fun CamplyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    preset: CamplyThemePreset = CamplyThemePreset.ORANGE,
    content: @Composable () -> Unit
) {
    val colors = getThemeColors(preset)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> darkColorScheme(
            primary = when (preset) {
                CamplyThemePreset.ORANGE -> Color(0xFFFFB77D)
                CamplyThemePreset.GREEN -> Color(0xFF9DD5AF)
                CamplyThemePreset.BLUE -> Color(0xFFA3CBFF)
                CamplyThemePreset.PURPLE -> Color(0xFFD5BAFF)
            },
            onPrimary = Color(0xFF191817),
            primaryContainer = colors.primary,
            onPrimaryContainer = Color.White,
            secondary = colors.primary,
            onSecondary = Color.White,
            background = Color(0xFF191817),
            onBackground = Color(0xFFF5F1ED),
            surface = Color(0xFF252321),
            onSurface = Color(0xFFF5F1ED),
            onSurfaceVariant = Color(0xFFD0C5BC),
            outlineVariant = Color(0xFF46413C),
            error = Color(0xFFFFB4AB)
        )

        else -> lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.text,

            secondary = colors.primary,
            onSecondary = Color.White,
            secondaryContainer = colors.primaryContainer,
            onSecondaryContainer = colors.text,

            background = colors.background,
            onBackground = colors.text,

            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.primaryContainer,
            onSurfaceVariant = colors.mutedText,

            outline = colors.mutedText,
            outlineVariant = colors.border,

            error = Color(0xFFB3261E),
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

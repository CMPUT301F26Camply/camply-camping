package com.example.camply.shared.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.ui.theme.CamplyBackground
import com.example.camply.ui.theme.CamplyBorder
import com.example.camply.ui.theme.CamplyDanger
import com.example.camply.ui.theme.CamplyInk
import com.example.camply.ui.theme.CamplyMutedText
import com.example.camply.ui.theme.CamplyOrange
import com.example.camply.ui.theme.CamplyOrangeDark
import com.example.camply.ui.theme.CamplyOrangeLight
import com.example.camply.ui.theme.CamplySurface

// CamplyComponents.kt - Shared UI Components Redesign
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT 6
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help redesign and improve the existing
// reusable Jetpack Compose UI components to create a more
// consistent appearance throughout the Camply application.
// This included assistance with applying Material 3 theme
// colors, improving component layouts, and creating new
// reusable input and selection components.
//
// Starting Prompt:
// "Can you help me redesign my existing CamplyComponents.kt
// file to give my Android Jetpack Compose app a cleaner,
// more modern and consistent appearance? I want to replace
// the old gradient backgrounds and orange cards with a
// lighter design... I want to improve the styling of buttons,
// cards, and text fields. I also want reusable input and selection
// components while keeping compatibility with my existing screens."
//
// There were many follow-up prompts in order to design everything.
//
// Changes from the original implementation:
// - Updated CamplyPalette to use shared theme color values.
// - Retained legacy color names for compatibility.
// - Redesigned CamplyScreen with a themed background and heading.
// - Changed OrangeCard to use a light surface with rounded
//   corners and a border while retaining its original name.
// - Updated PrimaryButton, SecondaryButton and DangerButton
//   with theme colors, rounded shapes and consistent sizing.
// - Updated CardLabel, CenteredMessage and ErrorMessage styling.
// - Updated text field colors to follow the active theme.
// - Added CamplyTextField for reusable styled text inputs.
// - Added CamplySelectionButton for selectable options with
//   optional icons, subtitles and selected state styling.
// - Expanded documentation for the reusable components.
//
// The existing shared component functions and their general
// purposes were retained, while their visual styling was
// updated to support the Camply redesign.


/*
 * Reusable building blocks that match the storyboard styling.
 * Any phase can use these so every screen looks the same.
 */

/**
 * Shared compatibility palette for Camply views.
 *
 * Existing color names are maintained so older views
 * can continue using the same references.
 *
 * New shared UI components should prefer MaterialTheme
 * colors whenever possible.
 */
object CamplyPalette {
    val Orange = CamplyOrange
    val Cream = CamplyOrangeLight
    val Black = CamplyInk
    val Danger = CamplyDanger
    val Ink = CamplyInk

    // Legacy colors retained for compatibility.
    val GradientTop = CamplyOrange
    val GradientBottom = CamplyOrangeDark

    // New neutral colors.
    val Background = CamplyBackground
    val Surface = CamplySurface
    val Border = CamplyBorder
    val MutedText = CamplyMutedText
}


/**
 * Displays a shared Camply screen layout.
 *
 * Provides a consistent background and page heading.
 * The content is provided by the calling view so
 * existing screen behavior remains unchanged.
 *
 * @param title The title displayed at the top.
 * @param modifier Modifier applied to the screen.
 * @param content Composable content displayed below the title.
 */
@Composable
fun CamplyScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(
                horizontal = 20.dp,
                vertical = 20.dp
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        content()
    }
}



/**
 * Displays content inside a shared Camply card.
 *
 * The existing function name is retained for
 * compatibility with current views.
 *
 * @param modifier Modifier applied to the card.
 * @param content Composable content inside the card.
 */
@Composable
fun OrangeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp),
        content = content
    )
}

/**
 * Displays a small uppercase label above card content.
 *
 * @param text The label to display.
 */
@Composable
fun CardLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )
}

/**
 * Displays the primary Camply action button.
 *
 * @param text Button label.
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button can be clicked.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor =
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor =
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Displays a secondary Camply action button.
 *
 * @param text Button label.
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button can be clicked.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer,
            contentColor =
                MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Displays a destructive action button.
 *
 * @param text Button label.
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier applied to the button.
 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}



/**
 * Displays a centered message and an optional action.
 *
 * @param text Message displayed to the user.
 * @param actionLabel Optional action button label.
 * @param onAction Optional action callback.
 */
@Composable
fun CenteredMessage(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))

            SecondaryButton(
                text = actionLabel,
                onClick = onAction
            )
        }
    }
}

/**
 * Provides consistent Material 3 text field colors.
 */
@Composable
fun camplyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

/**
 * Displays an error message using the Camply theme.
 *
 * @param message The error message displayed to the user.
 */
@Composable
fun ErrorMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}


/**
 * Displays a reusable Camply text input field.
 *
 * Uses the active Camply theme for colors and styling.
 *
 * @param value Current text field value.
 * @param onValueChange Called when the text changes.
 * @param label Label displayed above the text field.
 * @param modifier Modifier applied to the text field.
 * @param placeholder Optional placeholder text.
 * @param enabled Whether the field can be edited.
 * @param singleLine Whether the field accepts only one line.
 * @param isError Whether the field is in an error state.
 * @param keyboardOptions Keyboard configuration for the field.
 */
@Composable
fun CamplyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder)
                }
            },
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            colors = camplyTextFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Displays a selectable Camply button with an optional icon and subtitle.
 *
 * Supports Material icons and drawable resources, allowing the button
 * to display a small icon above its primary text.
 *
 * @param text Primary text displayed inside the button.
 * @param selected Whether the button is selected.
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button can be selected.
 * @param icon Optional Material icon displayed above the text.
 * @param subtitle Optional secondary text displayed below the title.
 * @param iconSize Size of the icon, defaults to 18.dp.
 * @param iconRes Optional drawable resource displayed instead of the Material icon.
 */
@Composable
fun CamplySelectionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    subtitle: String? = null,
    iconSize: Dp = 18.dp,
    iconRes: Int? = null
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(
            horizontal = 10.dp,
            vertical = 10.dp
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Displays a confirmation dialog for a destructive action.
 *
 * @param title The dialog title.
 * @param message The warning displayed to the user.
 * @param confirmText The confirmation button label.
 * @param dismissText The dismiss button label.
 * @param onConfirm Called when the action is confirmed.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}




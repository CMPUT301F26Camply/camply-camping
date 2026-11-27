package com.example.camply.settings.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.camply.settings.controller.SettingsController
import com.example.camply.settings.model.AppPolicy
import com.example.camply.settings.model.SettingsModel
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton

// SettingsView.kt - Settings screen with the "View policy and guidelines" button and the policy
// page (US 01.11.01). Notification preferences (US 01.10.01) will be added as another card.
// Outstanding issues: none.

/**
 * Displays the Settings screen, or the policy and guidelines when requested.
 *
 * @param model The model containing the screen state.
 * @param controller The controller handling the screen's actions.
 */
class SettingsView(
    private val model: SettingsModel,
    private val controller: SettingsController
) {

    /**
     * @param onBack Called when the entrant leaves Settings from its main page.
     */
    @Composable
    fun Display(onBack: () -> Unit) {
        // Leaving the screen should always reopen on the options, not the policy.
        DisposableEffect(Unit) { onDispose { controller.onPolicyBack() } }
        BackHandler { if (model.isPolicyVisible) controller.onPolicyBack() else onBack() }

        CamplyScreen(title = if (model.isPolicyVisible) "App policy" else "Settings") {
            if (model.isPolicyVisible) PolicyContent() else SettingsOptions(onBack)
        }
    }

    @Composable
    private fun SettingsOptions(onBack: () -> Unit) {
        OrangeCard {
            CardLabel("Policies")
            Spacer(Modifier.height(6.dp))
            Text(
                "Read the rules that apply to lotteries, reservations and reviews.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("View policy and guidelines", controller::onViewPolicyClicked)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Back", onBack)
    }

    @Composable
    private fun ColumnScope.PolicyContent() {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPolicy.sections.forEach { section ->
                OrangeCard {
                    CardLabel(section.title)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Back", controller::onPolicyBack)
    }
}
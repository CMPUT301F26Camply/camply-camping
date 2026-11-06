package com.example.camply.lottery.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.controller.LotteryOpportunityController
import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.DangerButton
import kotlinx.coroutines.launch

/**
 * Displays an entrant's lottery result and lets a winner decline their opportunity (US 01.04.03).
 *
 * @param model The model containing the opportunity's state.
 * @param controller The controller used to load and decline the opportunity.
 */
class LotteryOpportunityView(
    private val model: LotteryOpportunityModel,
    private val controller: LotteryOpportunityController
) {

    /**
     * Loads and displays the entrant's result for one lottery.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     */
    @Composable
    fun Display(lotteryId: String, entrantId: String) {
        val scope = rememberCoroutineScope()
        val entry = model.entry
        val error = model.errorMessage
        var showDeclineDialog by remember { mutableStateOf(false) }

        LaunchedEffect(lotteryId, entrantId) { controller.loadEntry(lotteryId, entrantId) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry != null) {
                resultText(entry)?.let { Text(it, color = CamplyPalette.Ink) }

                if (entry.opportunityStatus == LotteryEntry.OPPORTUNITY_DECLINED) {
                    Text("Opportunity declined", color = CamplyPalette.Ink)
                }
            }

            if (model.isLoading) {
                CircularProgressIndicator(color = CamplyPalette.Ink)
            } else if (entry?.canDecline() == true) {
                DangerButton("Decline reservation opportunity", { showDeclineDialog = true })
            }

            if (error != null) {
                Text(error, color = CamplyPalette.Ink)
            }
        }

        if (showDeclineDialog) {
            DeclineDialog(
                onConfirm = {
                    showDeclineDialog = false
                    scope.launch { controller.declineOpportunity(lotteryId, entrantId) }
                },
                onDismiss = { showDeclineDialog = false }
            )
        }
    }

    // Asks the entrant to confirm, because a decline cannot be undone in the app.
    @Composable
    private fun DeclineDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Decline this reservation opportunity?") },
            text = { Text("You won't be able to use it to book this campsite.") },
            confirmButton = { TextButton(onClick = onConfirm) { Text("Decline") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }

    private fun resultText(entry: LotteryEntry): String? =
        when (entry.status) {
            LotteryEntry.STATUS_WON -> "Result: You won"
            "lost" -> "Result: Not selected"
            "waiting" -> "Result: Waiting for the draw"
            else -> null
        }
}

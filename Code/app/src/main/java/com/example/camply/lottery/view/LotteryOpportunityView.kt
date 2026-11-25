package com.example.camply.lottery.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.controller.LotteryOpportunityController
import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.reservations.model.ReservationStatus
import com.example.camply.shared.view.PrimaryButton
import kotlinx.coroutines.launch

/**
 * Displays an entrant's lottery result, related reservation, and opportunity actions (US 01.04.03).
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
     * Winners may book or decline only while no associated reservation exists.
     *
     * @param lotteryId The lottery's document ID.
     * @param entrantId The entrant's Firebase user ID.
     * @param onBook The action to open the reservation booking screen.
     */
    @Composable
    fun Display(lotteryId: String, entrantId: String, onBook: () -> Unit = {})
    {
        val scope = rememberCoroutineScope()
        val entry = model.entry
        val error = model.errorMessage
        val reservation = model.reservation
        var showDeclineDialog by remember(lotteryId, entrantId) { mutableStateOf(false) }

        LaunchedEffect(lotteryId, entrantId) { controller.loadEntry(lotteryId, entrantId) }

        val isConfirmed = reservation?.status == ReservationStatus.CONFIRMED
        val isCancelled = reservation?.status == ReservationStatus.CANCELLED
        val isRejected = reservation?.status == ReservationStatus.REJECTED
        val isDeclined = entry?.opportunityStatus == LotteryEntry.OPPORTUNITY_DECLINED
        val isWinner = entry?.status == LotteryEntry.STATUS_WON

        val statusLabel = when {
            isConfirmed -> "Booked"
            isCancelled -> "Cancelled"
            isRejected -> "Rejected"
            isDeclined -> "Declined"
            isWinner -> "Selected"
            entry?.status == "lost" -> "Not selected"
            entry?.status == "waiting" -> "Waiting"
            else -> "Pending"
        }

        val statusMessage = when {
            isConfirmed -> "Your campsite reservation is confirmed. Manage it in My Reservations."
            isCancelled -> "This reservation was cancelled and is no longer active."
            isRejected -> "This reservation was rejected and is no longer active."
            isDeclined -> "You declined your winning opportunity. It can no longer be booked."
            isWinner -> "You were selected! You can book your campsite or decline your opportunity."
            entry?.status == "lost" -> "You weren't selected for this lottery."
            entry?.status == "waiting" -> "Your entry is in the draw. Check back for your result."
            else -> "Your lottery result is not available yet."
        }

        val statusBackground = when {
            isConfirmed || (isWinner && !isDeclined && reservation == null) -> MaterialTheme.colorScheme.primaryContainer
            isRejected -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val statusTextColor = when {
            isConfirmed || (isWinner && !isDeclined && reservation == null) -> MaterialTheme.colorScheme.onPrimaryContainer
            isRejected -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }


        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (model.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        "Loading your lottery result...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (entry != null || reservation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = statusBackground
                    ) {
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTextColor,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            )
                        )
                    }
                }

                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (reservation != null && reservation.dates.from.isNotBlank() && reservation.dates.to.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Reservation dates",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${reservation.dates.from} to ${reservation.dates.to}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (entry?.canConfirmReservation() == true && reservation == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeclineDialog = true },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp,MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "Decline",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        PrimaryButton(
                            text = "Book Reservation",
                            onClick = onBook,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else if (error == null) {
                Text(
                    "No lottery entry was found for your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
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
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Decline")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}

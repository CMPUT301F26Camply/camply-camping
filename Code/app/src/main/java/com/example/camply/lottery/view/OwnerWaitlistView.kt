package com.example.camply.lottery.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.lottery.controller.OwnerWaitlistController
import com.example.camply.lottery.model.OwnerWaitlistLottery
import com.example.camply.lottery.model.OwnerWaitlistModel
import com.example.camply.lottery.model.WaitlistEntrant
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays the owner's lotteries and the entrants who joined each one (US 02.09.01).
 *
 * @param model The model containing the screen state.
 * @param controller The controller used to load lotteries and their entrants.
 */
class OwnerWaitlistView(
    private val model: OwnerWaitlistModel,
    private val controller: OwnerWaitlistController
) {

    /**
     * Loads and displays the owner's lotteries, or the selected lottery's waitlist.
     *
     * @param ownerId The owner's Firebase user ID.
     */
    @Composable
    fun Display(ownerId: String) {
        val scope = rememberCoroutineScope()
        val error = model.errorMessage
        val selected = model.selectedLottery

        LaunchedEffect(ownerId) { controller.loadLotteries(ownerId) }

        CamplyScreen(title = if (selected != null) "Waitlist" else "Lottery Waitlists") {
            when {
                model.isLoading -> LoadingIndicator()

                error != null -> CenteredMessage(error, "Try again") {
                    scope.launch { controller.loadLotteries(ownerId) }
                }

                selected != null -> WaitlistDetails(selected)

                model.lotteries.isEmpty() -> CenteredMessage("You have no lotteries yet.")

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(model.lotteries, key = { it.lottery.id }) { item ->
                        OrangeCard(
                            Modifier.clickable { scope.launch { controller.onLotterySelected(item) } }
                        ) {
                            LotterySummary(item)
                            Text(
                                "${item.lottery.waitlistCount} joined",
                                fontSize = 13.sp,
                                color = CamplyPalette.Ink
                            )
                        }
                    }
                }
            }
        }
    }

    // Shows the selected lottery and everyone who joined it, whatever their draw status.
    @Composable
    private fun WaitlistDetails(item: OwnerWaitlistLottery) {
        val scope = rememberCoroutineScope()
        val error = model.entrantsError

        BackHandler { controller.onBackToLotteries() }

        SecondaryButton("Back to lotteries", onClick = controller::onBackToLotteries)
        Spacer(Modifier.height(12.dp))

        OrangeCard {
            LotterySummary(item)
            if (!model.isLoadingEntrants && error == null) {
                Spacer(Modifier.height(8.dp))
                CardLabel("Entrants (${model.entrants.size})")
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            model.isLoadingEntrants -> LoadingIndicator()

            error != null -> CenteredMessage(error, "Try again") {
                scope.launch { controller.onLotterySelected(item) }
            }

            model.entrants.isEmpty() ->
                CenteredMessage("No entrants have joined this waitlist yet.")

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(model.entrants, key = { it.entrantId }) { entrant ->
                    OrangeCard { EntrantSummary(entrant) }
                }
            }
        }
    }

    // Shows the lottery's campsites and registration period.
    @Composable
    private fun LotterySummary(item: OwnerWaitlistLottery) {
        Text(
            item.campsiteNames.joinToString(", ").ifEmpty { "Campsite unavailable" },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CamplyPalette.Ink
        )
        Text(
            "Registration: ${item.lottery.registrationStart} to ${item.lottery.registrationEnd}",
            fontSize = 13.sp,
            color = CamplyPalette.Ink
        )
    }

    // Shows one entrant's name, draw status, and when they joined.
    @Composable
    private fun EntrantSummary(entrant: WaitlistEntrant) {
        Text(
            entrant.name ?: "Unknown entrant",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = CamplyPalette.Ink
        )
        Text(
            statusLabel(entrant.status),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CamplyPalette.Ink
        )
        entrant.joinedAt?.let {
            Text("Joined ${formatDate(it)}", fontSize = 13.sp, color = CamplyPalette.Ink)
        }
    }

    @Composable
    private fun LoadingIndicator() = Box(
        Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }

    private fun statusLabel(status: String): String = when (status) {
        "waiting" -> "Waiting"
        "won" -> "Won"
        "lost" -> "Lost"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
}

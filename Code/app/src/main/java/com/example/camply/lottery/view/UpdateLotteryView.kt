
package com.example.camply.lottery.view

import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.camply.lottery.controller.UpdateLotteryController
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.UpdateLotteryModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import kotlinx.coroutines.launch

/**
 * Shows the owner's open lotteries and allows editing.
 *
 * The edit form uses the same two-page layout as LotteryCreateView.
 * Existing lottery values are pre-filled.
 *
 * @param model the lottery model
 * @param controller the lottery controller
 */
class UpdateLotteryView(
    private val model: UpdateLotteryModel,
    private val controller: UpdateLotteryController
) {

    @Composable
    fun Display(
        ownerId: String,
        onAddLottery: () -> Unit,
        onBack: () -> Unit
    ) {
        androidx.compose.runtime.LaunchedEffect(ownerId) {
            controller.loadLotteries(ownerId)
        }

        val editing = model.editingLottery

        if (editing != null) {
            EditForm(editing)
        } else {
            LotteryList(onAddLottery, onBack)
        }
    }
    @Composable
    private fun LotteryList(
        onAddLottery: () -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Lotteries",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Open lotteries you can edit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAddLottery,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create lottery",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (model.isLoading) {
                Text(
                    "Loading lotteries...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            model.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (!model.isLoading &&
                model.errorMessage == null &&
                model.lotteries.isEmpty()
            ) {
                Text(
                    text = "No open lotteries. Press the '+' button to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(model.lotteries, key = { it.id }) { lottery ->
                    LotteryCard(lottery)
                }
            }

            SecondaryButton(text = "Back", onClick = onBack)

            Spacer(Modifier.height(12.dp))
        }
    }
    @Composable
    private fun LotteryCard(lottery: Lottery) {

        val names = model.ownedCampsites
            .filter { it.id in lottery.campsiteIds }
            .joinToString(", ") { it.name }
            .ifBlank { "${lottery.campsiteIds.size} campsite(s)" }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(18.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(18.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    names,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "Registration: ${lottery.registrationStart} to ${lottery.registrationEnd}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Booking: ${lottery.bookingStart} to ${lottery.bookingEnd}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Stay: ${lottery.lotteryStart} to ${lottery.lotteryEnd}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Winners: ${lottery.numberOfWinners}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = { controller.onEdit(lottery) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit lottery for ${lottery.campsiteIds.size} campsite(s)",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    @Composable
    private fun EditForm(lottery: Lottery) {

        val scope = rememberCoroutineScope()

        var page by remember(lottery.id) {
            mutableStateOf(1)
        }

        var saving by remember(lottery.id) {
            mutableStateOf(false)
        }

        // Pre-filled values from the selected lottery.
        var registrationStart by remember(lottery.id) {
            mutableStateOf(lottery.registrationStart)
        }

        var registrationEnd by remember(lottery.id) {
            mutableStateOf(lottery.registrationEnd)
        }

        var bookingStart by remember(lottery.id) {
            mutableStateOf(lottery.bookingStart)
        }

        var bookingEnd by remember(lottery.id) {
            mutableStateOf(lottery.bookingEnd)
        }

        var lotteryStart by remember(lottery.id) {
            mutableStateOf(lottery.lotteryStart)
        }

        var lotteryEnd by remember(lottery.id) {
            mutableStateOf(lottery.lotteryEnd)
        }

        var winnerCount by remember(lottery.id) {
            mutableStateOf(lottery.numberOfWinners.toString())
        }

        CamplyScreen(title = "Update Lottery") {

            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {

                when (page) {
                    1 -> {

                        OrangeCard {

                            Text(
                                "Update the required details",
                                color = CamplyPalette.Ink
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Selected Campsite",
                                color = CamplyPalette.Ink
                            )

                            model.ownedCampsites
                                .filter { it.id in lottery.campsiteIds }
                                .forEach { campsite ->

                                    Text(
                                        text = ". ${campsite.name}",
                                        color = CamplyPalette.Ink,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Lottery Range",
                                color = CamplyPalette.Ink
                            )

                            DateRow(
                                start = lotteryStart,
                                end = lotteryEnd,
                                onStart = { lotteryStart = it },
                                onEnd = { lotteryEnd = it }
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Registration Range",
                                color = CamplyPalette.Ink
                            )

                            DateRow(
                                start = registrationStart,
                                end = registrationEnd,
                                onStart = { registrationStart = it },
                                onEnd = { registrationEnd = it }
                            )
                        }

                        model.errorMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = Color.White)
                        }

                        Spacer(Modifier.height(12.dp))

                        PrimaryButton(
                            "Next",
                            onClick = { page = 2 },
                            enabled = registrationStart.isNotBlank() &&
                                    registrationEnd.isNotBlank() &&
                                    lotteryStart.isNotBlank() &&
                                    lotteryEnd.isNotBlank()
                        )
                    }
                    2 -> {

                        OrangeCard {

                            Text(
                                "Booking Range",
                                color = CamplyPalette.Ink
                            )

                            DateRow(
                                start = bookingStart,
                                end = bookingEnd,
                                onStart = { bookingStart = it },
                                onEnd = { bookingEnd = it }
                            )

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = winnerCount,
                                onValueChange = {
                                    winnerCount = it.filter(Char::isDigit)
                                },
                                label = {
                                    Text("Number of lottery winners")
                                },
                                colors = camplyTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        model.errorMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        SecondaryButton(
                            "Back",
                            onClick = { page = 1 }
                        )

                        Spacer(Modifier.height(8.dp))

                        PrimaryButton(
                            "Save Changes",
                            onClick = {
                                if (saving) return@PrimaryButton

                                saving = true

                                scope.launch {
                                    try {
                                        controller.updateLottery(
                                            lottery,
                                            registrationStart,
                                            registrationEnd,
                                            bookingStart,
                                            bookingEnd,
                                            lotteryStart,
                                            lotteryEnd,
                                            winnerCount
                                        )
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                            enabled = !saving &&
                                    bookingStart.isNotBlank() &&
                                    bookingEnd.isNotBlank() &&
                                    winnerCount.isNotBlank()
                        )
                    }
                }
            }
        }
    }
    @Composable
    private fun DateRow(
        start: String,
        end: String,
        onStart: (String) -> Unit,
        onEnd: (String) -> Unit
    ) {
        Row(Modifier.fillMaxWidth()) {

            DateField(
                label = "Start Date",
                value = start,
                onDate = onStart,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.padding(4.dp))

            DateField(
                label = "End Date",
                value = end,
                onDate = onEnd,
                modifier = Modifier.weight(1f)
            )
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DateField(
        label: String,
        value: String,
        onDate: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {

        var open by remember {
            mutableStateOf(false)
        }

        val initialMillis = remember(value) {
            try {
                val parts = value.split("-").map { it.toInt() }

                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(parts[0], parts[1] - 1, parts[2])
                }.timeInMillis

            } catch (e: Exception) {
                null
            }
        }

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        Box(modifier) {

            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                colors = camplyTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                Modifier
                    .matchParentSize()
                    .clickable { open = true }
            )
        }

        if (open) {

            DatePickerDialog(
                onDismissRequest = { open = false },

                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let {
                                onDate(millisToIso(it))
                            }

                            open = false
                        }
                    ) {
                        Text("OK")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = { open = false }
                    ) {
                        Text("Cancel")
                    }
                }

            ) {
                DatePicker(state = pickerState)
            }
        }
    }
    private fun millisToIso(millis: Long): String {

        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("UTC")
        )

        calendar.timeInMillis = millis

        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
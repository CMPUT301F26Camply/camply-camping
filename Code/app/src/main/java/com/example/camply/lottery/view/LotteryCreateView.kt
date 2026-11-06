package com.example.camply.lottery.view

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.view.Display
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.lottery.controller.LotteryCreateController
import com.example.camply.lottery.model.LotteryCreateModel
import com.example.camply.shared.view.CamplyPalette
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CenteredMessage
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.shared.view.camplyTextFieldColors
import kotlinx.coroutines.launch
import java.util.Date
/**
 * Lottery Registration screen from the storyboard.
 *
 * The first page collects the campsite, lottery range, and registration range.
 * The second page collects the booking range and the winner count.
 * US 02.04.01, US 02.04.03, US 02.04.04, US 02.04.05.
 *
 * @param model the form values this screen displays
 * @param controller checks the form and saves the lottery
 */
class LotteryCreateView (
    private val model: LotteryCreateModel,
    private val controller: LotteryCreateController
){
 /**
 * Shows the form for this owner.
 *
 * Loads that owner's campsites, then shows page 1, page 2, or the saved message.
 *
 * @param ownerId Firebase uid stored on the lottery
 * @param onDone called when the owner leaves the saved screen
 */ 
    @Composable
    fun Display(ownerId: String,onDone:() -> Unit) {
        val scope = rememberCoroutineScope()
        var page by remember { mutableStateOf(1) }
        var saving by remember { mutableStateOf(false) }
        LaunchedEffect(ownerId) { controller.loadOwnedCampsites(ownerId) }
        CamplyScreen(title = "Lottery Registration") {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    model.savedId != null -> CenteredMessage("Lottery saved.","Back to home", onDone)
                    page == 1 -> FirstHalf(onNext = { page = 2 })
                    else -> SecondHalf(
                        onBack = { page = 1 },
                        saving = saving,
                        onConfirm = {
                            if (saving) return@SecondHalf
                            saving = true
                            scope.launch {
                                controller.createLottery(ownerId)
                                saving = false
                            }
                        }
                    )
                }
            }
        }
    }
/** Campsite, lottery range, and registration range. Next opens the second page. */

    @Composable
    private fun FirstHalf(onNext: ()-> Unit){
        OrangeCard {
            Text("Fill in the required details", color = CamplyPalette.Ink)
            Spacer((Modifier.height(12.dp)))
            Text("Selected Campsite", color = CamplyPalette.Ink)
            if(model.ownedCampsites.isEmpty()) {
                Text("Unable to load campsites, or you have no campsites yet", color = CamplyPalette.Ink)
            }else{
                model.ownedCampsites.forEach { campsite ->
                    val selected = campsite.id == model.selectedCampsiteId
                    Text(
                        text = if (selected) ". ${campsite.name}" else campsite.name,
                        color = CamplyPalette.Ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable{controller.onCampsiteSelected(campsite.id)}
                            .padding(vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Lottery Range ", color = CamplyPalette.Ink)
            DateRow(
                start = model.stayFrom,
                end = model.stayTo,
                onStart = controller::onStayFrom,
                onEnd = controller::onStayTo
            )
            Spacer(Modifier.height(12.dp))
            Text("Registration Range ", color = CamplyPalette.Ink)
            DateRow(
                start = model.registrationStart,
                end = model.registrationEnd,
                onStart = controller::onRegistrationStart,
                onEnd = controller::onRegistrationEnd
            )
        }
        model.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))
        val ready = model.selectedCampsiteId != null &&
                model.stayFrom.isNotBlank() && model.stayTo.isNotBlank()&&model.registrationEnd.isNotBlank()&&model.registrationStart.isNotBlank()
        PrimaryButton("Next", onClick = onNext, enabled = ready)

    }
/** Booking range, winner count, and Confirm. Back returns to the first page. */

    @Composable
    private fun SecondHalf(onBack: () -> Unit, saving: Boolean, onConfirm: () -> Unit) {
        OrangeCard {
            Text("Booking Range", color = CamplyPalette.Ink)
            DateRow(
                start = model.bookingStart,
                end = model.bookingEnd,
                onStart = controller::onBookingStart,
                onEnd = controller::onBookingEnd
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = model.winnerCount,
                onValueChange = controller::onWinnerCount,
                label = { Text("Number of lottery winners") },
                colors = camplyTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        model.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Back", onClick = onBack)
        Spacer(Modifier.height(8.dp))
// Ignore extra taps until Firestore returns, so one Confirm creates one lottery.
        PrimaryButton("Confirm", onClick = onConfirm, enabled = !saving)
    }
/** Start and end date boxes for one range. */

    @Composable
    private fun DateRow(
        start: String,
        end: String,
        onStart:(String) -> Unit,
        onEnd:(String)-> Unit
    ){
        Row(Modifier.fillMaxWidth()) {
            DataField("Start Date", start,onStart, Modifier.weight(1f))
            Spacer(Modifier.padding(4.dp))
            DataField("End Date",end,onEnd, Modifier.weight(1f))
        }
    }
/**
 * A read-only date box. Tapping it opens the calendar.
 * The chosen day is written back as YYYY-MM-DD.
 */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DataField(
        label: String,
        value: String,
        onDate: (String) -> Unit,
        modifier: Modifier = Modifier
    ){
        var open by remember { mutableStateOf(false) }
        val pickerState= rememberDatePickerState()

        Box(modifier){
            OutlinedTextField(
                value=value,
                onValueChange = {},
                readOnly = true,
                label={Text(label)},
                colors = camplyTextFieldColors(),
                modifier = Modifier.fillMaxHeight()
            )
            Box(Modifier.matchParentSize().clickable{open = true})
        }
        if(open){
            DatePickerDialog(
                onDismissRequest = {open=false},
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { onDate(millisToIso(it)) }
                        open = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = pickerState) }
        }
    }
/**
 * Turns the calendar's UTC midnight millis into YYYY-MM-DD.
 *
 * @param millis the instant selected in the date picker
 * @return that calendar day
 */
    private fun millisToIso(millis: Long) : String {
        val  calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = millis
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

}
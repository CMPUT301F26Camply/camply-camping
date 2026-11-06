package com.example.camply.user.view

import android.widget.ListView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camply.campsite.model.Campsite


/**
This class displays the list of all campsites that are registered
by a campsite owner

 * @param ownerName name of the campsite owner: To be displayed in the header bar
 * @param campsites the campsite owner's registered campsites
 * @param addCampsite the "+" button in front of the name (also part of the header)
 * @param editCampsite the "pencil" icon in the top-right corner of every campsite card to edit campsite information
 *  @param viewCampsite to view Campsite Details if the campsite card is clicked
 * @param lotteryRegistration called when "List Campsite in lottery" is clicked
 * @param reservationList is for the "View reservation list" button
 * @param viewOrCreateLottery is for the "Create/View Lottery" button
 *
 * Covering US 02.01.04 and US 02.03.01
 */
class CampsiteListView {
    @Composable
    fun Display(
        ownerName: String,
        campsites: List<Campsite>,
        addCampsite: () -> Unit,
        editCampsite: (Campsite) -> Unit,
        viewCampsite: (Campsite) -> Unit,
        lotteryRegistration: (Campsite) -> Unit,
        reservationList: () -> Unit,
        viewOrCreateLottery: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // "Brush" Implementation: https://developer.android.com/develop/ui/compose/graphics/draw/brush
                .background(Brush.verticalGradient(
                    listOf(Color(0xFFAB875F), Color(0xFF6C6A3D))
                    )
                )
        ) {
            //Row for the screen header: will have the Campsite Owner name and the "+" icon to add campsites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp, start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //"Icon" implementation: https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add campsite",
                    tint = Color.White,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .clickable { addCampsite() }
                        .background(Color.Black, CircleShape)
                        .padding(22.dp)
                )
                Text(
                    text = "$ownerName’s Campsites",
                    color = Color.White,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .background(
                            Color.Black,
                            RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                        )
                        .padding(vertical = 26.dp)
                )
            }

            Button(
                onClick = {reservationList()},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA8A443),
                    contentColor = Color.Black
                )
            ){
                    Text (
                        "View Reservation List",
                        fontSize = 16.sp
                    )
                }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {viewOrCreateLottery()},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA8A443),
                    contentColor = Color.Black
                )
            ){
                Text (
                    "Create/View Lottery",
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campsite list: Will be scrollable

            if (campsites.isEmpty()) {
                Text(
                    "No Registered Campsites. Press the '+' button to add campsites",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp).weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
                    ,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(campsites, key = {it.id}) { campsite ->
                        CampsiteCard(campsite, viewCampsite, editCampsite, lotteryRegistration)
                    }
                }
            }
        }
    }

    @Composable
    //To handle individual campsite cards
    private fun CampsiteCard(
        campsite: Campsite,
        viewInfo: (Campsite) -> Unit,
        onEdit: (Campsite) -> Unit,
        addInLottery: (Campsite) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable{viewInfo(campsite)}
                .background(Color(0xFFF4974B), RoundedCornerShape(16.dp))
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // The image box on the card. Will have a placeholder icon if no images for that campsite
                val image = campsite.images.firstOrNull()
                if(image.isNullOrBlank()) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Campsite image",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(width = 145.dp, height = 120.dp)
                            .background(
                                Color(0xFFD3A093),
                                RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                            )
                            .padding(28.dp)
                    )
                } else {
                    // "Async Implementation": https://developer.android.com/develop/ui/compose/graphics/images/loading
                    AsyncImage(
                        model = image,
                        contentDescription = "photo of campsite",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp))
                            .size(width = 145.dp, height = 120.dp)
                    )

                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 15.dp, top = 8.dp)
                ) {
                    Text(campsite.name, fontSize = 24.sp, color = Color.Black)
                    Text(campsite.location, fontSize = 14.sp, color = Color.Black)
                    Text(
                        campsite.amenities.joinToString(", "),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text("Max occupancy: ${campsite.maxOccupancy}", fontSize = 14.sp, color = Color.Black)
                }

                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit ${campsite.name}",
                    tint = Color.Black,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onEdit(campsite) }
                        .padding(4.dp)
                )
            }

            Button(
                onClick = {addInLottery(campsite)},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, bottom = 10.dp, end = 40.dp, top = 10.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C6A3D),
                    contentColor = Color.White
                )
            ){
                Text (
                    "List Campsite in Lottery",
                    fontSize = 16.sp
                )
            }
        }
    }

}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteListViewPreview() {
    MaterialTheme {
        CampsiteListView().Display(
            ownerName = "Owner 1",
            campsites = listOf(
                Campsite(
                    id = "a",
                    name = "Campsite A",
                    location = "Location, A",
                    amenities = listOf("Fireplace", "Trash disposal"),
                    maxOccupancy = 8,
                    rules = "Quiet hours 9pm"
                ),
                Campsite(
                    id = "b",
                    name = "Campsite B",
                    location = "Location, B",
                    amenities = listOf("Washrooms"),
                    maxOccupancy = 5,
                    rules = "No pets"
                ),
                Campsite(
                    id = "c",
                    name = "Campsite C",
                    location = "Location C",
                    amenities = listOf("Picnic Table"),
                    maxOccupancy = 10,
                    rules = "No loud music"
                )
            ),
            addCampsite = {},
            editCampsite = {},
            viewCampsite = {},
            lotteryRegistration = {},
            reservationList = {},
            viewOrCreateLottery = {}
        )
    }
}
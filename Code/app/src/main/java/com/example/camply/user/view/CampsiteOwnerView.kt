package com.example.camply.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.modifier.modifierLocalConsumer

class CampsiteOwnerView {
    /**
     * Displays campsites for that campsite owner
     *
     * @param errors for information validation
     * @param onSubmit to send the information to the controller handling registration actions
     */
    @Composable
    fun Display(onSubmit: (
        name: String, location: String,
            amenities: String, maxOccupancy: String, rules: String, tags: String) -> Unit, errors: List<String>) {

        var name by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var amenities by remember { mutableStateOf("") }
        var maxOccupancy by remember { mutableStateOf("") }
        var rules by remember { mutableStateOf("") }
        var tags by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top

        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween){
                // edit Button
                // "Icon" Implementation: https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    // the following modifier implementation is from Anthropic, Claude, "How to I add a black, circular background to this edit icon?", 2026-10-31
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.Black, CircleShape)
                        .padding(vertical = 20.dp)
                )

                Text(
                    text = "Enter Campsite Name",
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = Color.White,
                    /*
                        Author: Megh Lath https://stackoverflow.com/questions/76625545/pill-shaped-composable-in-jetpack-compose
                        Title: "Pill Shaped Composable in jetpack compose"
                        Answer: https://stackoverflow.com/questions/76625545/pill-shaped-composable-in-jetpack-compose
                        Date: 2023-07-06
                        License: CC-BY-SA 4.0 (International)
                    */
                    modifier = Modifier
                        .width(200.dp)
                        .background(
                            Color.Black,
                            RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                        )
                        .padding(vertical = 20.dp)
                )


            }

            Spacer(Modifier.height(10.dp))
            // "Box" Implementation: https://developer.android.com/develop/ui/compose/layouts/basics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Gray, RoundedCornerShape(15.dp))

            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .background(Color.Gray, RoundedCornerShape(15.dp))

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") }
                    )

                    OutlinedTextField(
                        value = amenities,
                        onValueChange = { amenities = it },
                        label = { Text("Amenities") }
                    )
                    OutlinedTextField(
                        value = maxOccupancy,
                        onValueChange = { maxOccupancy = it },
                        label = { Text("Max Occupancy") }
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = {tags = it },
                        label = { Text("Tags") }
                    )
                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("Rules") }
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(onClick = {
                        onSubmit(name, location, amenities, maxOccupancy, rules, tags)
                    },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    {
                        Text("Confirm")
                    }
            }

        }

        }
    }
    @Preview(showBackground = true, widthDp = 390, heightDp = 844)
    @Composable
    fun CampsiteOwnerViewPreview() {
        MaterialTheme {
            CampsiteOwnerView().Display(
                errors = listOf("Please enter campsite name"),   // sample error so you can see how it looks
                onSubmit = {_,_,_,_,_,_ ->}
            )
        }
    }
}


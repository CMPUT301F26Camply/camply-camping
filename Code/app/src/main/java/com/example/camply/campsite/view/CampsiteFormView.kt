package com.example.camply.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camply.campsite.model.Campsite
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavBarItem
import com.example.camply.shared.navigation.model.NavigationModel

class CampsiteFormView {

    @Composable
    fun Display(
        campsite: Campsite?,
        errors: List<String>,
        onSubmit: (String, String, String, String, String, String, String, String, String) -> Unit,
        onBack: () -> Unit,
        navigationModel: NavigationModel,
        navigationController: NavigationController
    ) {
        var name by remember(campsite) { mutableStateOf(campsite?.name ?: "") }
        var location by remember(campsite) { mutableStateOf(campsite?.location ?: "") }
        var description by remember(campsite) { mutableStateOf(campsite?.description ?: "") }
        var amenities by remember(campsite) { mutableStateOf(campsite?.amenities?.joinToString(", ") ?: "") }
        var occupancy by remember(campsite) { mutableStateOf(campsite?.maxOccupancy?.toString() ?: "") }
        var price by remember(campsite) { mutableStateOf(campsite?.price?.toString() ?: "") }
        var tags by remember(campsite) { mutableStateOf(campsite?.tags?.joinToString(", ") ?: "") }
        var rules by remember(campsite) { mutableStateOf(campsite?.rules ?: "") }
        var cancellation by remember(campsite) { mutableStateOf(campsite?.cancellationPolicy ?: "") }
        var showPolicies by remember { mutableStateOf(false) }

        val fieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black,
            focusedIndicatorColor = Color.Black,
            unfocusedIndicatorColor = Color.Black.copy(alpha = 0.4f),
            cursorColor = Color.Black
        )

        if (showPolicies) {
            AlertDialog(
                modifier = Modifier.testTag("policies_dialog"),
                onDismissRequest = { showPolicies = false },
                title = { Text("Rules & Policies") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = rules,
                            onValueChange = { rules = it },
                            label = { Text("Rules") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("rules_field")
                        )
                        TextField(
                            value = cancellation,
                            onValueChange = { cancellation = it },
                            label = { Text("Cancellation policy") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("cancellation_field")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showPolicies = false },
                        modifier = Modifier.testTag("policies_done_button")
                    ) { Text("Done") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFAB875F), Color(0xFF6C6A3D))))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp, start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit campsite",
                    tint = Color.White,
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.Black, CircleShape)
                        .padding(22.dp)
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter Campsite Name", color = Color.White) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .height(70.dp)
                        .testTag("name_field")
                )
            }

            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF8B5A4A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add image",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Black, CircleShape)
                        .padding(4.dp)
                )
            }

            // Scrollable form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color(0xFFF4974B), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("location_field")
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("description_field")
                )
                TextField(
                    value = amenities,
                    onValueChange = { amenities = it },
                    label = { Text("Amenities (comma separated)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("amenities_field")
                )
                TextField(
                    value = occupancy,
                    onValueChange = { occupancy = it.filter { c -> c.isDigit() } },
                    label = { Text("Maximum Occupancy") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("occupancy_field")
                )
                TextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("price_field")
                )
                TextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("tags_field")
                )
                Button(
                    onClick = { showPolicies = true },
                    modifier = Modifier.fillMaxWidth().testTag("rules_policies_button"),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C6A3D),
                        contentColor = Color.White
                    )
                ) {
                    Text("Rules & Policies")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).testTag("cancel_button"),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF6C6A3D)
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSubmit(name, location, description, amenities, occupancy, price, tags, rules, cancellation)
                        },
                        modifier = Modifier.weight(1f).testTag("save_button"),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C6A3D),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save")
                    }
                }
            }

            if (errors.isNotEmpty()) {
                Text(
                    errors.joinToString("\n"),
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("errors_text")
                )
            }

        }
    }
}

// Add campsite preview
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteFormViewAddPreview() {
    val model = NavigationModel()
    MaterialTheme {
        CampsiteFormView().Display(
            campsite = null,
            errors = emptyList(),
            onSubmit = { _, _, _, _, _, _, _, _, _ -> },
            onBack = {},
            navigationModel = model,
            navigationController = NavigationController(model)
        )
    }
}

// Edit campsite preview
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CampsiteFormViewEditPreview() {
    val model = NavigationModel()
    MaterialTheme {
        CampsiteFormView().Display(
            campsite = Campsite(
                id = "a",
                name = "Campsite A",
                location = "Location, A",
                description = "Lakeside site",
                amenities = listOf("Fireplace", "Trash disposal"),
                maxOccupancy = 8,
                price = 45.0,
                tags = listOf("Lake", "Quiet"),
                rules = "Quiet hours 9pm",
                cancellationPolicy = "Free until 48h before"
            ),
            errors = emptyList(),
            onSubmit = { _, _, _, _, _, _, _, _, _ -> },
            onBack = {},
            navigationModel = model,
            navigationController = NavigationController(model)
        )
    }
}
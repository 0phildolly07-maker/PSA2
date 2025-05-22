package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(navController: NavController, category: String) {
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedAgeGroup by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Refine Your Search",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Category: ${category.replace("-", " ").capitalize()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Location Filter
        FilterSection(
            title = "Location",
            options = listOf("Nearby", "Online", "City Center", "South Side", "North Side"),
            selectedOption = selectedLocation,
            onOptionSelected = { selectedLocation = it }
        )

        // Time Filter
        FilterSection(
            title = "Time",
            options = listOf("Morning", "Afternoon", "Evening", "Weekends", "Flexible"),
            selectedOption = selectedTime,
            onOptionSelected = { selectedTime = it }
        )

        // Age Group Filter
        FilterSection(
            title = "Age Group",
            options = listOf("Youth (13-18)", "Young Adult (19-30)", "Adult (31+)", "Seniors (65+)"),
            selectedOption = selectedAgeGroup,
            onOptionSelected = { selectedAgeGroup = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Button(
            onClick = { 
                navController.navigate("results?category=$category&location=$selectedLocation&time=$selectedTime&age=$selectedAgeGroup")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Results")
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        
        options.forEach { option ->
            FilterChip(
                selected = option == selectedOption,
                onClick = { onOptionSelected(option) },
                label = { Text(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }
    }
} 
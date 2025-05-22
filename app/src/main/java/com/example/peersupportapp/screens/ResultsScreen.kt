package com.example.peersupportapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun ResultsScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Results",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        repeat(3) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Service ${index + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Location: City ${index + 1}")
                    Button(onClick = { navController.navigate("details") }) {
                        Text("View Details")
                    }
                }
            }
        }
    }
} 
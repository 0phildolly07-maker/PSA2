package com.example.peersupportapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun ServiceDetailsScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Service Details",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Service Name: Example Service")
        Text("Location: Example City")
        Text("Description: A helpful service for peer support.")

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Results")
        }
    }
} 
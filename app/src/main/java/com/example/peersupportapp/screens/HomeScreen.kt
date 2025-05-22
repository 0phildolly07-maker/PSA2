package com.example.peersupportapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Find Support Services",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        var searchQuery by remember { mutableStateOf("") }
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("results") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Categories:",
            style = MaterialTheme.typography.titleMedium
        )
        
        CategoryButton("Peer Support", navController)
        CategoryButton("Social Activities", navController)
        CategoryButton("Mental Health Support", navController)
        CategoryButton("Employment & Volunteering", navController)
        CategoryButton("Education & Training", navController)
        CategoryButton("Practical Support", navController)
    }
}

@Composable
private fun CategoryButton(text: String, navController: NavHostController) {
    Button(
        onClick = { navController.navigate("results") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text)
    }
} 
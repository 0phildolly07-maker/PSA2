package com.example.peersupportapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SubmitServiceScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Submit a New Service",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        var serviceName by remember { mutableStateOf("") }
        var serviceLocation by remember { mutableStateOf("") }

        Text("Service Name:")
        BasicTextField(
            value = serviceName,
            onValueChange = { serviceName = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Location:")
        BasicTextField(
            value = serviceLocation,
            onValueChange = { serviceLocation = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Submit")
        }
    }
} 
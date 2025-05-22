package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    navController: NavController,
    serviceId: String,
    searchViewModel: SearchViewModel
) {
    val service = searchViewModel.services.value.find { it.id == serviceId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        IconButton(
            onClick = { navController.navigateUp() }
        ) {
            Text("←")
        }

        service?.let { 
            // Service details
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = service.schedule ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = service.location,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(service.features) { feature ->
                    AssistChip(
                        onClick = { },
                        label = { Text(feature) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyLarge
            )

            service.contact?.let { contact ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium
                )
                if (contact.phone.isNotBlank()) {
                    Text(
                        text = "Phone: ${contact.phone}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (contact.email.isNotBlank()) {
                    Text(
                        text = "Email: ${contact.email}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } ?: run {
            Text("Service not found")
        }
    }
} 
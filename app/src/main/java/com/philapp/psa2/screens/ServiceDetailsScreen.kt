package com.philapp.psa2.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.ui.components.psaInnerTopAppBarColors
import com.philapp.psa2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    navController: NavController,
    serviceId: String,
    searchViewModel: SearchViewModel
) {
    val services by searchViewModel.services.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val service = services.find { it.id == serviceId }
    val context = LocalContext.current
    val websiteUrl = service?.websiteUrl?.takeIf { it.isNotBlank() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(service?.groupName ?: "Service details") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = psaInnerTopAppBarColors()
            )
        }
    ) { padding ->
        if (isLoading && service == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            service?.let {
                Column {
                    Text(
                        text = service.groupName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = service.organizationName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (service.types.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(service.types) { type ->
                                QuietChip(type.getDisplayName())
                            }
                        }
                    }
                }

                if (!service.schedule.isNullOrBlank() || service.location.isNotBlank()) {
                    DetailSection(title = "When & where") {
                        if (!service.schedule.isNullOrBlank()) {
                            DetailMetaRow(Icons.Default.Schedule, service.schedule)
                        }
                        if (service.location.isNotBlank()) {
                            DetailMetaRow(Icons.Default.LocationOn, service.location)
                        }
                        if (service.town.isNotBlank() && !service.location.contains(service.town)) {
                            Text(
                                text = service.town,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }
                    }
                }

                if (service.features.isNotEmpty()) {
                    DetailSection(title = "Features") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(service.features) { feature ->
                                QuietChip(feature)
                            }
                        }
                    }
                }

                if (service.description.isNotBlank()) {
                    DetailSection(title = "About") {
                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val phoneEntries = service.contact?.phone
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                val email = service.contact?.email?.takeIf { it.isNotBlank() }

                if (phoneEntries.isNotEmpty() || !email.isNullOrBlank()) {
                    DetailSection(title = "Contact") {
                        phoneEntries.forEach { entry ->
                            DetailMetaRow(Icons.Filled.Phone, entry)
                        }
                        email?.let { DetailMetaRow(Icons.Filled.Email, it) }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    phoneEntries.forEach { entry ->
                        val callLabel = if (phoneEntries.size == 1) "Call" else "Call $entry"
                        Button(
                            onClick = {
                                val cleanedPhone = entry.replace(Regex("[^0-9+]"), "")
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedPhone"))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Call $entry" }
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(callLabel)
                        }
                    }
                    email?.let {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Email $it" }
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email")
                        }
                    }
                    if (service.location.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(service.location)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Open map for ${service.location}" }
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Maps")
                        }
                    }
                    if (!websiteUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Open website" }
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Website")
                        }
                    }
                    FilledTonalButton(
                        onClick = { navController.navigate("edit_service/${service.id}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Service")
                    }
                }
            } ?: run {
                Text("Service not found")
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailMetaRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuietChip(label: String) {
    AssistChip(
        onClick = { },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.primary
        )
    )
}

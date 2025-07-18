package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.viewmodel.SearchViewModel
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.*
import com.philapp.psa2.viewmodel.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavController,
    searchViewModel: SearchViewModel,
    viewModel: AdminViewModel
) {
    val pendingServices by viewModel.pendingServices.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusUpdateState by viewModel.statusUpdateState.collectAsState()
    val migrationState by viewModel.migrationState.collectAsState()
    val firebaseCheckState by viewModel.firebaseCheckState.collectAsState()
    
    var showDuplicateSection by remember { mutableStateOf(false) }
    val allServices by searchViewModel.services.collectAsState()
    val duplicateServices by remember(allServices) {
        derivedStateOf {
            val services = allServices
            val grouped = services.groupBy { Triple(it.organizationName, it.groupName, it.location) }
            val duplicates = grouped.values.filter { it.size > 1 }
            duplicates
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPendingServices()
        searchViewModel.loadServices()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showDuplicateConfirmDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Show migration feedback
    LaunchedEffect(migrationState) {
        migrationState?.onSuccess { count ->
            snackbarHostState.showSnackbar("Successfully migrated $count documents to descriptive IDs")
            viewModel.clearMigrationState()
        }?.onFailure { e ->
            snackbarHostState.showSnackbar("Migration failed: ${e.message}")
            viewModel.clearMigrationState()
        }
    }

    // Show Firebase check feedback
    LaunchedEffect(firebaseCheckState) {
        firebaseCheckState?.onSuccess { message ->
            snackbarHostState.showSnackbar("Firebase Check: $message")
            viewModel.clearFirebaseCheckState()
        }?.onFailure { e ->
            snackbarHostState.showSnackbar("Firebase check failed: ${e.message}")
            viewModel.clearFirebaseCheckState()
        }
    }

    if (showDuplicateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmDialog = false },
            title = { Text("Remove Duplicates") },
            text = { Text("This will remove all duplicate services, keeping only the first occurrence of each. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDuplicateConfirmDialog = false
                        searchViewModel.removeDuplicateServices()
                        scope.launch {
                            snackbarHostState.showSnackbar("Removing duplicate services...")
                        }
                    }
                ) {
                    Text("Yes, Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add Replace Firebase button
                    Button(
                        onClick = { 
                            searchViewModel.replaceFirebaseWithHardcodedServices()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Replace Firebase")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replace Firebase")
                    }
                    
                    // Add Check Firebase button
                    Button(
                        onClick = { 
                            viewModel.checkAndPopulateFirebase()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Check Firebase")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Firebase")
                    }
                    
                    // Existing Sync Services button
                    Button(
                        onClick = { 
                            searchViewModel.syncAllServicesWithFirebase()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Services")
                    }
                    
                    // Existing Duplicates button
                    Button(
                        onClick = { 
                            showDuplicateSection = !showDuplicateSection
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (duplicateServices.isNotEmpty()) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicates")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duplicates (${duplicateServices.size})")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading services...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "An error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadPendingServices() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                pendingServices.isEmpty() && duplicateServices.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending services or duplicates to review",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (showDuplicateSection && duplicateServices.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                            text = "Duplicate Services (${duplicateServices.size} groups)",
                                    style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = { showDuplicateConfirmDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove All Duplicates")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove All")
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    duplicateServices.forEach { duplicateGroup ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Duplicate Group (${duplicateGroup.size} services):",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                duplicateGroup.forEach { service ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "• ${service.organizationName} - ${service.groupName}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "  Location: ${service.location}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Button(
                                                            onClick = { 
                                                                scope.launch {
                                                                    searchViewModel.deleteService(service.id)
                                                                    snackbarHostState.showSnackbar("Service deleted")
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.error
                                                            ),
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (pendingServices.isNotEmpty()) {
                            Text(
                                text = "Pending Services (${pendingServices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = pendingServices,
                                key = { it.id }
                            ) { service ->
                                PendingServiceCard(
                                    service = service,
                                    onApprove = {
                                        viewModel.approveService(service.id)
                                    },
                                    onReject = {
                                        viewModel.rejectService(service.id)
                                    }
                                )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(statusUpdateState) {
                statusUpdateState?.let { result ->
                    result.onSuccess {
                        snackbarHostState.showSnackbar("Service status updated successfully")
                        viewModel.loadPendingServices()
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Failed to update service status: ${e.message}")
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PendingServiceCard(
    service: Service,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!service.schedule.isNullOrBlank()) {
                Text(
                    text = service.schedule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = service.location,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
                
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
} 
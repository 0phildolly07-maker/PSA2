package com.phild.servicescanner.ui.screens.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.ui.components.appErrorMessage
import com.phild.servicescanner.ui.state.FlyerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    uiState: FlyerUiState.Reviewing,
    onOpenActivity: (Int) -> Unit,
    onRemoveActivity: (Int) -> Unit,
    onGenerateDocument: () -> Unit,
    onGenerationErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val generationError = uiState.generationError
    val errorMessage = generationError?.let { appErrorMessage(it) }

    LaunchedEffect(generationError) {
        if (generationError != null && errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onGenerationErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_activity_list)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.activities_found, uiState.services.size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uiState.services, key = { index, service ->
                    "${index}-${service.serviceName}-${service.times}"
                }) { index, service ->
                    ActivityRow(
                        service = service,
                        onClick = { onOpenActivity(index) },
                        onRemove = { onRemoveActivity(index) }
                    )
                }
            }
            Button(
                onClick = onGenerateDocument,
                enabled = uiState.services.isNotEmpty() && !uiState.isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.isGenerating) {
                        stringResource(R.string.generating_document)
                    } else {
                        stringResource(R.string.generate_n_forms, uiState.services.size)
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActivityRow(
    service: ExtractedService,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val details = listOfNotNull(
        service.days.joinToString(", ").takeIf { it.isNotBlank() },
        service.times,
        service.venue ?: service.address
    ).joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.displayName(),
                    style = MaterialTheme.typography.titleMedium
                )
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_remove_activity)
                )
            }
        }
    }
}

package com.phild.servicescanner.ui.screens.output

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.data.document.DocumentOpener
import com.phild.servicescanner.ui.components.appErrorMessage
import com.phild.servicescanner.ui.state.FlyerUiState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DocumentCreatedScreen(
    uiState: FlyerUiState.DocumentGenerated,
    onCreateAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val opener = remember { DocumentOpener(context.applicationContext) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val file = File(uiState.filePath)
    val openFailed = stringResource(R.string.document_open_failed)
    val shareFailed = stringResource(R.string.document_share_failed)
    var showSentDialog by remember(uiState.filePath, uiState.publishedCount) {
        mutableStateOf(uiState.publishedCount > 0 && uiState.publishError == null)
    }

    if (showSentDialog) {
        AlertDialog(
            onDismissRequest = { showSentDialog = false },
            title = { Text(stringResource(R.string.firebase_sent_dialog_title)) },
            text = {
                Text(
                    if (uiState.publishedCount == 1) {
                        stringResource(R.string.firebase_sent_dialog_body_one)
                    } else {
                        stringResource(R.string.firebase_sent_dialog_body_many, uiState.publishedCount)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showSentDialog = false }) {
                    Text(stringResource(R.string.firebase_sent_dialog_ok))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.document_created_successfully),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            val status = firebaseStatus(uiState)
            if (status.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.publishError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (!file.exists() || !opener.open(file)) {
                        scope.launch { snackbarHostState.showSnackbar(openFailed) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_open_document))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (!file.exists() || !opener.share(file)) {
                        scope.launch { snackbarHostState.showSnackbar(shareFailed) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_share_document))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCreateAnother) {
                Text(stringResource(R.string.action_create_another))
            }
        }
    }
}

@Composable
private fun firebaseStatus(uiState: FlyerUiState.DocumentGenerated): String {
    uiState.publishError?.let { return appErrorMessage(it) }
    val lines = buildList {
        when {
            uiState.publishedCount == 1 -> add(stringResource(R.string.firebase_upload_one))
            uiState.publishedCount > 1 -> add(
                stringResource(R.string.firebase_upload_many, uiState.publishedCount)
            )
        }
        if (uiState.skippedDuplicateCount > 0) {
            add(stringResource(R.string.firebase_upload_duplicates, uiState.skippedDuplicateCount))
        }
        if (uiState.skippedIncompleteCount > 0) {
            add(stringResource(R.string.firebase_upload_incomplete, uiState.skippedIncompleteCount))
        }
    }
    return lines.joinToString("\n")
}

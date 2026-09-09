package com.phild.servicescanner.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phild.servicescanner.BuildConfig
import com.phild.servicescanner.R
import com.phild.servicescanner.ui.SettingsViewModel

private const val GEMINI_KEY_URL = "https://aistudio.google.com/apikey"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_ai_title),
                body = if (uiState.geminiConfigured) {
                    stringResource(R.string.settings_ai_ready)
                } else {
                    stringResource(R.string.settings_ai_sample)
                }
            )
            Text(
                text = stringResource(R.string.settings_gemini_key_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { uriHandler.openUri(GEMINI_KEY_URL) },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(stringResource(R.string.settings_gemini_key_link))
            }
            if (uiState.maskedRuntimeKey.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.settings_gemini_key_saved, uiState.maskedRuntimeKey),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = uiState.draftKey,
                onValueChange = viewModel::onDraftKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_gemini_key_label)) },
                placeholder = { Text(stringResource(R.string.settings_gemini_key_placeholder)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = viewModel::saveDraftKey,
                enabled = uiState.draftKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_gemini_key_save))
            }
            if (uiState.maskedRuntimeKey.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::removeSavedKey,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_gemini_key_remove))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            SettingsSection(
                title = stringResource(R.string.settings_template_title),
                body = if (uiState.templateAvailable) {
                    stringResource(R.string.settings_template_ready)
                } else {
                    stringResource(R.string.settings_template_missing)
                }
            )
            SettingsSection(
                title = stringResource(R.string.settings_privacy_title),
                body = stringResource(R.string.settings_privacy_body)
            )
            SettingsSection(
                title = stringResource(R.string.settings_about_title),
                body = stringResource(
                    R.string.settings_about_body,
                    stringResource(R.string.scanner_app_name),
                    BuildConfig.VERSION_NAME
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    body: String
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
}

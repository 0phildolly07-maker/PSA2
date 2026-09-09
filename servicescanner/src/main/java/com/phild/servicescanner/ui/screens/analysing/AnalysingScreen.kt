package com.phild.servicescanner.ui.screens.analysing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.ui.components.appErrorMessage
import com.phild.servicescanner.ui.state.AnalysingPhase
import com.phild.servicescanner.ui.state.FlyerUiState

@Composable
fun AnalysingScreen(
    uiState: FlyerUiState,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analysing = uiState as? FlyerUiState.Analysing
    val failed = uiState as? FlyerUiState.ExtractionFailed
    val busy = analysing != null

    BackHandler(enabled = busy) { }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (analysing != null) {
                val phaseText = analysingPhaseText(analysing.phase, analysing.isTimetable)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = phaseText }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (analysing.isTimetable) {
                            R.string.analysing_privacy_note_timetable
                        } else {
                            R.string.analysing_privacy_note
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else if (failed != null) {
                Text(
                    text = stringResource(
                        if (failed.isTimetable) {
                            R.string.extraction_failed_title_timetable
                        } else {
                            R.string.extraction_failed_title
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = appErrorMessage(failed.error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.action_try_again))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onChooseAnother) {
                    Text(stringResource(R.string.action_choose_another))
                }
            }
        }
    }
}

@Composable
private fun analysingPhaseText(phase: AnalysingPhase, isTimetable: Boolean): String {
    return when (phase) {
        AnalysingPhase.AnalysingFlyer -> stringResource(
            if (isTimetable) R.string.analysing_timetable else R.string.analysing_flyer
        )
        AnalysingPhase.ReadingInformation -> stringResource(R.string.reading_information)
        AnalysingPhase.OrganisingDetails -> stringResource(R.string.organising_details)
    }
}

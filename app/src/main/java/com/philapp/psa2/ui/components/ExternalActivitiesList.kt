package com.philapp.psa2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.philapp.psa2.model.ExternalActivity
import com.philapp.psa2.model.SearchState

@Composable
fun ExternalActivitiesList(
    activities: List<ExternalActivity>,
    searchState: SearchState,
    onAddActivity: (ExternalActivity) -> Unit,
    modifier: Modifier = Modifier
) {
    when (searchState) {
        is SearchState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is SearchState.Error -> {
            Text(
                text = searchState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(16.dp)
            )
        }
        else -> {
            if (activities.isEmpty()) {
                Text(
                    text = "No external activities found",
                    modifier = modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activities) { activity ->
                        ExternalActivityCard(
                            activity = activity,
                            onAddActivity = onAddActivity
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalActivityCard(
    activity: ExternalActivity,
    onAddActivity: (ExternalActivity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.organization,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.location,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activity.features.forEach { feature ->
                    AssistChip(
                        onClick = { },
                        label = { Text(feature) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onAddActivity(activity) },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary // Red accent
                )
            ) {
                Text("Add to My Services")
            }
        }
    }
} 
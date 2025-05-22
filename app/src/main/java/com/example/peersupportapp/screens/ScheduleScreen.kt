package com.example.peersupportapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.peersupportapp.viewmodel.ScheduleViewModel
import com.example.peersupportapp.viewmodel.ScheduleUiState
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

data class TimeSlot(
    val startTime: String,
    val endTime: String
)

data class GroupSession(
    val id: String,
    val name: String,
    val description: String,
    val capacity: Int? = null,
    val requiresBooking: Boolean = false
)

data class ScheduledGroup(
    val group: GroupSession,
    val dayOfWeek: DayOfWeek,
    val timeSlot: TimeSlot,
    val location: String,
    val isRecurring: Boolean = true
)

data class BookingRequest(
    val sessionId: String,
    val name: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavHostController,
    viewModel: ScheduleViewModel = viewModel()
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var showBookingDialog by remember { mutableStateOf<ScheduledGroup?>(null) }

    // Collect UI state
    val scheduleUiState by viewModel.scheduleUiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Schedule",
            style = MaterialTheme.typography.headlineMedium
        )

        when (scheduleUiState) {
            is ScheduleUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            is ScheduleUiState.Error -> {
                Text(
                    text = (scheduleUiState as ScheduleUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is ScheduleUiState.Success -> {
                // Day selector and schedule display remain the same
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DayOfWeek.values()) { day ->
                        DayTab(
                            day = day,
                            isSelected = selectedDay == day,
                            onClick = { selectedDay = day }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // For now, still using sample data
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(getSampleSchedule(selectedDay)) { session ->
                        SessionCard(session = session) {
                            showBookingDialog = session
                        }
                    }
                }
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text("Back")
        }
    }

    showBookingDialog?.let { session ->
        BookingDialog(
            scheduledGroup = session,
            onDismiss = { showBookingDialog = null },
            onConfirm = { bookingRequest ->
                viewModel.submitBooking(bookingRequest)
                showBookingDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayTab(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary 
               else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(4.dp),
        onClick = onClick
    ) {
        Text(
            text = day.name.take(3),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                   else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: ScheduledGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = session.group.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${session.timeSlot.startTime} - ${session.timeSlot.endTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Location: ${session.location}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDialog(
    scheduledGroup: ScheduledGroup,
    onDismiss: () -> Unit,
    onConfirm: (BookingRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book Session") },
        text = {
            Column {
                Text("${scheduledGroup.group.name}")
                Text("Time: ${scheduledGroup.timeSlot.startTime}")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        BookingRequest(
                            sessionId = scheduledGroup.group.id,
                            name = name,
                            email = email
                        )
                    )
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Sample data
private fun getSampleSchedule(day: DayOfWeek): List<ScheduledGroup> {
    val morningSession = GroupSession(
        id = "morning-1",
        name = "Morning Support Group",
        description = "Start your day with peer support",
        capacity = 10,
        requiresBooking = true
    )

    val afternoonSession = GroupSession(
        id = "afternoon-1",
        name = "Afternoon Activity",
        description = "Creative activities and social time",
        capacity = 15,
        requiresBooking = true
    )

    return listOf(
        ScheduledGroup(
            group = morningSession,
            dayOfWeek = day,
            timeSlot = TimeSlot(
                startTime = "09:00",
                endTime = "10:30"
            ),
            location = "Room 101"
        ),
        ScheduledGroup(
            group = afternoonSession,
            dayOfWeek = day,
            timeSlot = TimeSlot(
                startTime = "14:00",
                endTime = "15:30"
            ),
            location = "Activity Room"
        )
    )
} 
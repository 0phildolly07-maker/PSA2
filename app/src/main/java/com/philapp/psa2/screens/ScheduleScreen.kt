package com.philapp.psa2.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.R
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.Service
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable

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
    val requiresBooking: Boolean = false,
    val websiteUrl: String? = null
)

data class ScheduledGroup(
    val group: GroupSession,
    val dayOfWeek: DayOfWeek,
    val timeSlot: TimeSlot,
    val location: String,
    val isRecurring: Boolean = true
)

data class Service(
    val id: String,
    val organizationName: String,
    val groupName: String,
    val location: String,
    val town: String,
    val description: String,
    val types: List<ServiceType>,
    val features: List<String>,
    val contact: ContactInfo? = null,
    val schedule: String? = null,
    val status: ServiceStatus = ServiceStatus.PENDING,
    val isDuplicate: Boolean = false,
    val websiteUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Schedule",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getSampleSchedule(selectedDay)) { session ->
                SessionCard(session = session)
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Back")
        }
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
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary 
               else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(4.dp)
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
private fun SessionCard(session: ScheduledGroup) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
            session.group.websiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Website",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

private fun getSampleSchedule(day: DayOfWeek): List<ScheduledGroup> {
    val morningSession = GroupSession(
        id = "morning-1",
        name = "Morning Support Group",
        description = "Start your day with peer support",
        capacity = 10,
        requiresBooking = true,
        websiteUrl = "https://www.example.com/morning-support"
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
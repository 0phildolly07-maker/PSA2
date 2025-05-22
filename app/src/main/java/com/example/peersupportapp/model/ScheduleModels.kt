package com.example.peersupportapp.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime
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
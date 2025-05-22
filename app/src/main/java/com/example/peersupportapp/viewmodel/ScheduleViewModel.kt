package com.example.peersupportapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.peersupportapp.screens.BookingRequest
import com.example.peersupportapp.screens.ScheduledGroup

class ScheduleViewModel : ViewModel() {
    private val _scheduleUiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val scheduleUiState: StateFlow<ScheduleUiState> = _scheduleUiState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            try {
                // In a real app, this would fetch data from a repository
                _scheduleUiState.value = ScheduleUiState.Success(emptyList())
            } catch (e: Exception) {
                _scheduleUiState.value = ScheduleUiState.Error("Failed to load schedule")
            }
        }
    }

    fun submitBooking(bookingRequest: BookingRequest) {
        viewModelScope.launch {
            try {
                // In a real app, this would submit to a backend service
                // For now, just print to console
                println("Booking submitted: $bookingRequest")
            } catch (e: Exception) {
                println("Error submitting booking: ${e.message}")
            }
        }
    }
}

sealed class ScheduleUiState {
    data object Loading : ScheduleUiState()
    data class Success(val schedule: List<ScheduledGroup>) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
} 
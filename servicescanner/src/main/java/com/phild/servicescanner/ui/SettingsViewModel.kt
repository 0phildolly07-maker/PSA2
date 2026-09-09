package com.phild.servicescanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phild.servicescanner.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val geminiConfigured: Boolean = false,
    val maskedRuntimeKey: String = "",
    val draftKey: String = "",
    val templateAvailable: Boolean = true
)

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val draftKey = MutableStateFlow("")

    val uiState: StateFlow<SettingsUiState> = combine(
        container.geminiConfigured,
        container.maskedRuntimeKey,
        draftKey
    ) { configured, masked, draft ->
        SettingsUiState(
            geminiConfigured = configured,
            maskedRuntimeKey = masked,
            draftKey = draft,
            templateAvailable = container.templateAvailable
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            geminiConfigured = container.geminiConfigured.value,
            maskedRuntimeKey = container.maskedRuntimeKey.value,
            templateAvailable = container.templateAvailable
        )
    )

    fun onDraftKeyChange(value: String) {
        draftKey.value = value
    }

    fun saveDraftKey() {
        val key = draftKey.value
        if (key.isBlank()) return
        viewModelScope.launch {
            container.saveRuntimeApiKey(key)
            draftKey.value = ""
        }
    }

    fun removeSavedKey() {
        viewModelScope.launch {
            container.clearRuntimeApiKey()
            draftKey.value = ""
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(container) as T
                }
            }
        }
    }
}

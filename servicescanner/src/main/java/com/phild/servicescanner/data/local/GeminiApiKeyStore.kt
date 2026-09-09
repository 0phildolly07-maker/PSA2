package com.phild.servicescanner.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.geminiApiKeyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = GeminiApiKeyStore.DATASTORE_NAME
)

class GeminiApiKeyStore(context: Context) {
    private val dataStore = context.applicationContext.geminiApiKeyDataStore

    fun readBlocking(): String = runBlocking { read() }

    suspend fun read(): String {
        return dataStore.data.first()[KEY].orEmpty()
    }

    suspend fun save(key: String) {
        val trimmed = key.trim()
        dataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(KEY)
            } else {
                prefs[KEY] = trimmed
            }
        }
    }

    companion object {
        const val DATASTORE_NAME = "gemini_api_key"
        private val KEY = stringPreferencesKey("gemini_api_key")
    }
}

object ApiKeyMask {
    fun mask(key: String): String {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.length <= 8) return "••••••••"
        return "••••••••${trimmed.takeLast(4)}"
    }
}

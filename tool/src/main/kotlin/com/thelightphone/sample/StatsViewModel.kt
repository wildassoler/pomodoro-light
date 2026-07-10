package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val KEY_HISTORY_JSON = stringPreferencesKey("pomodoro_history_json")
private val json = Json { ignoreUnknownKeys = true }

class StatsViewModel(
    private val dataStore: DataStore<Preferences>
) : LightViewModel<Unit>() {

    private val _history = MutableStateFlow<List<DailyStats>>(emptyList())
    val history = _history.asStateFlow()

    init {
        viewModelScope.launch {
            loadHistory()
        }
    }

    private suspend fun loadHistory() {
        val prefs = dataStore.data.first()
        val raw = prefs[KEY_HISTORY_JSON]

        val parsed = if (raw != null) {
            try {
                json.decodeFromString(ListSerializer(DailyStats.serializer()), raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        _history.value = parsed.sortedByDescending { it.date }
    }

    // DEV-ONLY: see TestDataSeeder. Remove before shipping.
    fun seedTestData() {
        viewModelScope.launch {
            TestDataSeeder.seedHistory(dataStore)
            loadHistory()
        }
    }
}
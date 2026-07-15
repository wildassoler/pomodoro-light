package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        _history.value = HistoryStore.load(dataStore).sortedByDescending { it.date }
    }

    // DEV-ONLY: see TestDataSeeder. Remove before shipping.
    fun seedTestData() {
        viewModelScope.launch {
            TestDataSeeder.seedHistory(dataStore)
            loadHistory()
        }
    }
}
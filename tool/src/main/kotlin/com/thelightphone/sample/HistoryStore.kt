package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Single source of truth for reading/writing the daily pomodoro history.
// Anything that needs the history (ViewModel, Stats screen, test seeder)
// goes through here, instead of each keeping its own copy of the key/format.
object HistoryStore {

    private val KEY_HISTORY_JSON = stringPreferencesKey("pomodoro_history_json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(dataStore: DataStore<Preferences>): List<DailyStats> {
        val prefs = dataStore.data.first()
        val raw = prefs[KEY_HISTORY_JSON] ?: return emptyList()

        return try {
            json.decodeFromString(ListSerializer(DailyStats.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(dataStore: DataStore<Preferences>, history: List<DailyStats>) {
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = json.encodeToString(ListSerializer(DailyStats.serializer()), history)
        }
    }
}
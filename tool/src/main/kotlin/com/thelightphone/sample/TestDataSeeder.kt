package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.random.Random

// DEV-ONLY utilities for generating fake data during development/testing.
// Not used anywhere in the shipped app flow — call manually from a debug
// button, or delete this file entirely before submitting the tool.
object TestDataSeeder {

    private val KEY_HISTORY_JSON = stringPreferencesKey("pomodoro_history_json")
    private val json = Json { ignoreUnknownKeys = true }

    // Fills the last [days] days with random pomodoro counts (0-5 per day).
    suspend fun seedHistory(dataStore: DataStore<Preferences>, days: Int = 30) {
        val today = LocalDate.now()
        val fakeHistory = (0 until days).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong()).toString()
            val pomodoros = Random.nextInt(0, 6)
            DailyStats(
                date = date,
                pomodorosCompleted = pomodoros,
                focusMinutes = pomodoros * 25,
            )
        }

        dataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = json.encodeToString(ListSerializer(DailyStats.serializer()), fakeHistory)
        }
    }
}
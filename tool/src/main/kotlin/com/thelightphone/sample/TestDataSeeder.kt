package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.time.LocalDate
import kotlin.random.Random

// DEV-ONLY utilities for generating fake data during development/testing.
// Not used anywhere in the shipped app flow — call manually from a debug
// button, or delete this file entirely before submitting the tool.
object TestDataSeeder {

    // Fills the last [days] days with random pomodoro counts (1-5 per day,
    // so every day shows some activity when testing the chart/list).
    suspend fun seedHistory(dataStore: DataStore<Preferences>, days: Int = 30) {
        val today = LocalDate.now()
        val fakeHistory = (0 until days).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong()).toString()
            val pomodoros = Random.nextInt(1, 6)
            DailyStats(
                date = date,
                pomodorosCompleted = pomodoros,
                focusMinutes = pomodoros * 25,
            )
        }

        HistoryStore.save(dataStore, fakeHistory)
    }
}
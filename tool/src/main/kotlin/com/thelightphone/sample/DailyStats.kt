package com.thelightphone.sample

import kotlinx.serialization.Serializable

// One day's worth of completed pomodoro data
@Serializable
data class DailyStats(
    val date: String,          // "yyyy-MM-dd", e.g. "2026-07-10"
    val pomodorosCompleted: Int,
    val focusMinutes: Int,
)

// Keeps only the most recent [maxDays] entries, replacing any existing entry
// for [today] with the updated values.
fun List<DailyStats>.withUpdatedDay(
    today: String,
    pomodorosCompleted: Int,
    focusMinutes: Int,
    maxDays: Int = 30,
): List<DailyStats> {
    val withoutToday = this.filterNot { it.date == today }
    val updated = withoutToday + DailyStats(today, pomodorosCompleted, focusMinutes)
    return updated.sortedByDescending { it.date }.take(maxDays)
}
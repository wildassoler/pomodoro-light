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

// Formats an ISO date ("2026-07-10") as "Today", "Yesterday", or "Jul 10"
fun String.toReadableDate(): String {
    val date = java.time.LocalDate.parse(this)
    val today = java.time.LocalDate.now()

    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> {
            val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
            "$month ${date.dayOfMonth}"
        }
    }
}
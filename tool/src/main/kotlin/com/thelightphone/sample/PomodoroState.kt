package com.thelightphone.sample

// The two phases of a pomodoro cycle
enum class PomodoroMode {
    FOCUS,
    BREAK
}

// Returns the other phase (FOCUS -> BREAK, BREAK -> FOCUS)
fun PomodoroMode.opposite(): PomodoroMode {
    return if (this == PomodoroMode.FOCUS) PomodoroMode.BREAK else PomodoroMode.FOCUS
}

// Represents a sound that should be played once, then cleared
enum class SoundEvent {
    FOCUS_ENDED,
    BREAK_ENDED,
}

// Full state of the timer, read by the UI
data class PomodoroState(
    // Current phase
    val mode: PomodoroMode = PomodoroMode.FOCUS,

    // Chosen focus length in minutes (min 15, steps of 5)
    val focusMinutes: Int = 25,

    // Chosen break length in minutes (steps of 5)
    val breakMinutes: Int = 5,

    // Seconds left in the current countdown
    val remainingSeconds: Int = 25 * 60,

    // Whether the timer is currently ticking
    val isRunning: Boolean = false,

    // Focus sessions completed today
    val pomodorosToday: Int = 0,

    // Total focus minutes completed today
    val totalFocusMinutesToday: Int = 0,

    // Whether we're showing the setup screen (true) or the running timer screen (false)
    val showSetupScreen: Boolean = true,

    // Set when a phase just ended, so the UI can play the matching sound. Cleared right after.
    val pendingSound: SoundEvent? = null,
) {
    // Fraction of the current phase remaining, from 1f (just started) to 0f (finished)
    val progress: Float
        get() {
            val totalSeconds = if (mode == PomodoroMode.FOCUS) focusMinutes * 60 else breakMinutes * 60
            return if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
        }
}

// Formats a duration in seconds as "MM:SS" (e.g. 90 -> "01:30")
fun Int.toTimeLabel(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%02d:%02d".format(minutes, seconds)
}
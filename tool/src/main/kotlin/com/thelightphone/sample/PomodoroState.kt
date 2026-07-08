package com.thelightphone.sample

// The two phases of a pomodoro cycle
enum class PomodoroMode {
    FOCUS,
    BREAK
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

    // Set when a phase just ended, so the UI can play the matching sound. Cleared right after.
    val pendingSound: SoundEvent? = null,

    // Whether we're showing the setup screen (true) or the running timer screen (false)
    val showSetupScreen: Boolean = true,

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
)
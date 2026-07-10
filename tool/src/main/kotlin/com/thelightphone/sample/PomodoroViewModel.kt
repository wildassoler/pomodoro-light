package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

// Keys used to read/write values in the shared DataStore
private val KEY_LAST_DATE = stringPreferencesKey("pomodoro_last_date")
private val KEY_POMODOROS_TODAY = intPreferencesKey("pomodoro_count_today")
private val KEY_TOTAL_MINUTES_TODAY = intPreferencesKey("pomodoro_total_minutes_today")
private val KEY_HISTORY_JSON = stringPreferencesKey("pomodoro_history_json")

private val historyJson = Json { ignoreUnknownKeys = true }

class PomodoroViewModel(
    private val dataStore: DataStore<Preferences>
) : LightViewModel<Unit>() {

    private val _state = MutableStateFlow(PomodoroState())
    val state = _state.asStateFlow()

    // Holds the running countdown coroutine, so we can cancel it on pause
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            loadDailyProgress()
        }
    }

    private suspend fun loadDailyProgress() {
        val today = LocalDate.now().toString()
        val prefs = dataStore.data.first()

        val savedDate = prefs[KEY_LAST_DATE]
        val savedPomodoros = prefs[KEY_POMODOROS_TODAY] ?: 0
        val savedMinutes = prefs[KEY_TOTAL_MINUTES_TODAY] ?: 0

        // If the saved date is not today, the saved progress belongs to a previous day
        val isSameDay = savedDate == today

        _state.value = _state.value.copy(
            pomodorosToday = if (isSameDay) savedPomodoros else 0,
            totalFocusMinutesToday = if (isSameDay) savedMinutes else 0,
        )
    }

    private suspend fun saveDailyProgress() {
        val today = LocalDate.now().toString()
        val prefs = dataStore.data.first()

        val currentHistory = try {
            val raw = prefs[KEY_HISTORY_JSON]
            if (raw != null) historyJson.decodeFromString(ListSerializer(DailyStats.serializer()), raw) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val updatedHistory = currentHistory.withUpdatedDay(
            today = today,
            pomodorosCompleted = _state.value.pomodorosToday,
            focusMinutes = _state.value.totalFocusMinutesToday,
        )

        dataStore.edit { editablePrefs ->
            editablePrefs[KEY_LAST_DATE] = today
            editablePrefs[KEY_POMODOROS_TODAY] = _state.value.pomodorosToday
            editablePrefs[KEY_TOTAL_MINUTES_TODAY] = _state.value.totalFocusMinutesToday
            editablePrefs[KEY_HISTORY_JSON] = historyJson.encodeToString(ListSerializer(DailyStats.serializer()), updatedHistory)
        }
    }

    fun start() {
        if (_state.value.isRunning) return

        _state.value = _state.value.copy(isRunning = true, showSetupScreen = false)

        timerJob = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1.seconds)
                _state.value = _state.value.copy(
                    remainingSeconds = _state.value.remainingSeconds - 1
                )
            }
            onCountdownFinished()
        }
    }

    fun pause() {
        timerJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        timerJob?.cancel()
        val minutes = minutesForCurrentMode()
        _state.value = _state.value.copy(
            isRunning = false,
            remainingSeconds = minutes * 60
        )
    }

    fun setFocusMinutes(minutes: Int) {
        if (_state.value.isRunning) return

        _state.value = _state.value.copy(focusMinutes = minutes)

        if (_state.value.mode == PomodoroMode.FOCUS) {
            _state.value = _state.value.copy(remainingSeconds = minutes * 60)
        }
    }

    fun setBreakMinutes(minutes: Int) {
        if (_state.value.isRunning) return

        _state.value = _state.value.copy(breakMinutes = minutes)

        if (_state.value.mode == PomodoroMode.BREAK) {
            _state.value = _state.value.copy(remainingSeconds = minutes * 60)
        }
    }

    fun skipToNextPhase() {
        timerJob?.cancel()

        val nextMode = if (_state.value.mode == PomodoroMode.FOCUS) {
            PomodoroMode.BREAK
        } else {
            PomodoroMode.FOCUS
        }

        val nextMinutes = if (nextMode == PomodoroMode.FOCUS) {
            _state.value.focusMinutes
        } else {
            _state.value.breakMinutes
        }

        _state.value = _state.value.copy(
            mode = nextMode,
            remainingSeconds = nextMinutes * 60,
            isRunning = false,
        )
    }

    fun backToSetup() {
        timerJob?.cancel()
        val minutes = minutesForCurrentMode()
        _state.value = _state.value.copy(
            isRunning = false,
            remainingSeconds = minutes * 60,
            showSetupScreen = true,
        )
    }

    private fun minutesForCurrentMode(): Int {
        return if (_state.value.mode == PomodoroMode.FOCUS) {
            _state.value.focusMinutes
        } else {
            _state.value.breakMinutes
        }
    }

    private fun onCountdownFinished() {
        val finishedMode = _state.value.mode

        val updatedPomodoros = if (finishedMode == PomodoroMode.FOCUS) {
            _state.value.pomodorosToday + 1
        } else {
            _state.value.pomodorosToday
        }

        val updatedTotalMinutes = if (finishedMode == PomodoroMode.FOCUS) {
            _state.value.totalFocusMinutesToday + _state.value.focusMinutes
        } else {
            _state.value.totalFocusMinutesToday
        }

        val nextMode = if (finishedMode == PomodoroMode.FOCUS) {
            PomodoroMode.BREAK
        } else {
            PomodoroMode.FOCUS
        }

        val nextMinutes = if (nextMode == PomodoroMode.FOCUS) {
            _state.value.focusMinutes
        } else {
            _state.value.breakMinutes
        }

        val soundToPlay = if (finishedMode == PomodoroMode.FOCUS) {
            SoundEvent.FOCUS_ENDED
        } else {
            SoundEvent.BREAK_ENDED
        }

        _state.value = _state.value.copy(
            mode = nextMode,
            remainingSeconds = nextMinutes * 60,
            isRunning = false,
            pomodorosToday = updatedPomodoros,
            totalFocusMinutesToday = updatedTotalMinutes,
            pendingSound = soundToPlay,
        )

        viewModelScope.launch {
            saveDailyProgress()
        }
    }

    fun clearPendingSound() {
        _state.value = _state.value.copy(pendingSound = null)
    }
}
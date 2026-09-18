package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.audio.LightAudio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

private val KEY_LAST_DATE = stringPreferencesKey("pomodoro_last_date")
private val KEY_POMODOROS_TODAY = intPreferencesKey("pomodoro_count_today")
private val KEY_TOTAL_MINUTES_TODAY = intPreferencesKey("pomodoro_total_minutes_today")

class PomodoroViewModel(
    private val dataStore: DataStore<Preferences>,
    lightAudio: LightAudio,
    filesDir: File,
) : LightViewModel<Unit>() {

    private val _state = MutableStateFlow(PomodoroState())
    val state = _state.asStateFlow()

    private var timerJob: Job? = null

    private val audio = TimerAudioController(lightAudio, filesDir, viewModelScope)

    init {
        viewModelScope.launch {
            loadDailyProgress()
        }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        viewModelScope.launch {
            loadDailyProgress()
        }
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    private suspend fun loadDailyProgress() {
        val today = LocalDate.now().toString()
        val prefs = dataStore.data.first()

        val savedDate = prefs[KEY_LAST_DATE]
        val savedPomodoros = prefs[KEY_POMODOROS_TODAY] ?: 0
        val savedMinutes = prefs[KEY_TOTAL_MINUTES_TODAY] ?: 0

        val isSameDay = savedDate == today

        _state.value = _state.value.copy(
            pomodorosToday = if (isSameDay) savedPomodoros else 0,
            totalFocusMinutesToday = if (isSameDay) savedMinutes else 0,
        )
    }

    private suspend fun saveDailyProgress() {
        val today = LocalDate.now().toString()

        val currentHistory = HistoryStore.load(dataStore)
        val updatedHistory = currentHistory.withUpdatedDay(
            today = today,
            pomodorosCompleted = _state.value.pomodorosToday,
            focusMinutes = _state.value.totalFocusMinutesToday,
        )
        HistoryStore.save(dataStore, updatedHistory)

        dataStore.edit { prefs ->
            prefs[KEY_LAST_DATE] = today
            prefs[KEY_POMODOROS_TODAY] = _state.value.pomodorosToday
            prefs[KEY_TOTAL_MINUTES_TODAY] = _state.value.totalFocusMinutesToday
        }
    }

    fun start() {
        if (_state.value.isRunning) return

        _state.value = _state.value.copy(isRunning = true, showSetupScreen = false)
        audio.playAlert("audio/start_click.wav")
        audio.startSession(_state.value.remainingSeconds)

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
        audio.stopSession()
        _state.value = _state.value.copy(isRunning = false)
        audio.playAlert("audio/pause_click.mp3")
    }

    fun reset() {
        timerJob?.cancel()
        audio.stopSession()
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
        audio.stopSession()

        val nextMode = _state.value.mode.opposite()
        val nextMinutes = minutesFor(nextMode)

        _state.value = _state.value.copy(
            mode = nextMode,
            remainingSeconds = nextMinutes * 60,
            isRunning = false,
        )
        audio.playAlert("audio/start_click.wav")
    }

    fun backToSetup() {
        timerJob?.cancel()
        audio.stopSession()
        val minutes = minutesForCurrentMode()
        _state.value = _state.value.copy(
            isRunning = false,
            remainingSeconds = minutes * 60,
            showSetupScreen = true,
        )
    }

    private fun minutesForCurrentMode(): Int {
        return minutesFor(_state.value.mode)
    }

    private fun minutesFor(mode: PomodoroMode): Int {
        return if (mode == PomodoroMode.FOCUS) {
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

        val nextMode = finishedMode.opposite()
        val nextMinutes = minutesFor(nextMode)

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
        val sound = _state.value.pendingSound
        _state.value = _state.value.copy(pendingSound = null)

        when (sound) {
            SoundEvent.FOCUS_ENDED -> audio.playAlert("audio/finished_pomodoro.mp3")
            SoundEvent.BREAK_ENDED -> audio.startAlarmLoop("audio/finished_break.wav")
            null -> return
        }
    }
}
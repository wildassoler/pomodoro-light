package com.thelightphone.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.audio.LightAudioItem
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioSource
import com.thelightphone.sdk.audio.LightMediaMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

// Keys used to read/write today's counters in the shared DataStore.
// (The full history lives in HistoryStore, not here.)
private val KEY_LAST_DATE = stringPreferencesKey("pomodoro_last_date")
private val KEY_POMODOROS_TODAY = intPreferencesKey("pomodoro_count_today")
private val KEY_TOTAL_MINUTES_TODAY = intPreferencesKey("pomodoro_total_minutes_today")

class PomodoroViewModel(
    private val dataStore: DataStore<Preferences>
) : LightViewModel<Unit>() {

    private val _state = MutableStateFlow(PomodoroState())
    val state = _state.asStateFlow()

    // Holds the running countdown coroutine, so we can cancel it on pause
    private var timerJob: Job? = null

    // Holds the looping alarm coroutine (break-ended sound), cancelled once
    // the user presses Start again
    private var alarmLoopJob: Job? = null

    // Set by the UI once the audio player is available (see attachAudioPlayer)
    private var audioPlayer: LightAudioPlayer? = null

    init {
        viewModelScope.launch {
            loadDailyProgress()
        }
    }

    fun attachAudioPlayer(player: LightAudioPlayer) {
        audioPlayer = player
    }

    // Re-checks the date every time the screen becomes visible again (e.g.
    // returning from History, or the app resuming from background). This
    // catches the day rolling over past midnight while the app stayed open.
    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
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

        // Update the full history (delegated to HistoryStore)
        val currentHistory = HistoryStore.load(dataStore)
        val updatedHistory = currentHistory.withUpdatedDay(
            today = today,
            pomodorosCompleted = _state.value.pomodorosToday,
            focusMinutes = _state.value.totalFocusMinutesToday,
        )
        HistoryStore.save(dataStore, updatedHistory)

        // Update today's quick-access counters
        dataStore.edit { prefs ->
            prefs[KEY_LAST_DATE] = today
            prefs[KEY_POMODOROS_TODAY] = _state.value.pomodorosToday
            prefs[KEY_TOTAL_MINUTES_TODAY] = _state.value.totalFocusMinutesToday
        }
    }

    fun start() {
        if (_state.value.isRunning) return

        alarmLoopJob?.cancel()
        alarmLoopJob = null

        _state.value = _state.value.copy(isRunning = true, showSetupScreen = false)
        playSound("audio/start_click.wav")

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
        playSound("audio/pause_click.wav")
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

        val nextMode = _state.value.mode.opposite()
        val nextMinutes = minutesFor(nextMode)

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
        return minutesFor(_state.value.mode)
    }

    // Returns the configured length (in minutes) for the given mode
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

    // Plays the sound matching the phase that just ended, then clears the pending event.
    // Focus alerts play once; break alerts loop until the user presses Start.
    fun clearPendingSound() {
        val sound = _state.value.pendingSound
        _state.value = _state.value.copy(pendingSound = null)

        when (sound) {
            SoundEvent.FOCUS_ENDED -> playSound("audio/finished_pomodoro.mp3")
            SoundEvent.BREAK_ENDED -> startAlarmLoop("audio/finished_break.mp3")
            null -> return
        }
    }

    private fun startAlarmLoop(assetPath: String) {
        alarmLoopJob?.cancel()
        alarmLoopJob = viewModelScope.launch {
            val player = audioPlayer ?: return@launch
            while (true) {
                playSound(assetPath)
                delay(200) // let playback actually start before we watch for it to end
                player.isPlaying.first { !it }
            }
        }
    }

    private fun playSound(assetPath: String) {
        audioPlayer?.setMediaQueue(
            listOf(
                LightAudioItem(
                    source = LightAudioSource.AssetSource(assetPath),
                    metadata = LightMediaMetadata(title = "Pomodoro alert"),
                ),
            ),
        )
        audioPlayer?.play()
    }
}
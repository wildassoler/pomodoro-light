package com.thelightphone.sample

import com.thelightphone.sdk.audio.LightAudio
import com.thelightphone.sdk.audio.LightAudioItem
import com.thelightphone.sdk.audio.LightAudioPlayback
import com.thelightphone.sdk.audio.LightAudioPlayer
import com.thelightphone.sdk.audio.LightAudioSource
import com.thelightphone.sdk.audio.LightMediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

// Owns all audio for the Pomodoro timer: short UI feedback sounds (clicks,
// alerts) and the detached "keep alive" silence track that survives the
// screen turning off. The ViewModel only calls into this — it doesn't know
// about LightAudioPlayer, attachment modes, or the silence file at all.
class TimerAudioController(
    private val lightAudio: LightAudio,
    private val filesDir: File,
    private val scope: CoroutineScope,
) {
    // Short attached player: quick feedback sounds that only need to play
    // while the app is open (click, pause, alert chime).
    private val alertPlayer: LightAudioPlayer = lightAudio.newPlayer()

    // Detached player: plays a silent track for the length of the current
    // session, keeping a MediaSessionService alive independent of this
    // screen/process — our workaround for not having a real "schedule an
    // alert" API.
    private var sessionPlayer: LightAudioPlayer? = null

    private var alarmLoopJob: Job? = null

    // Starts (or restarts) the detached "keep alive" track for a session
    // of this length. Call when the user presses Start.
    fun startSession(durationSeconds: Int) {
        alarmLoopJob?.cancel()
        alarmLoopJob = null

        sessionPlayer?.let {
            it.stop()
            it.release()
        }

        val player = lightAudio.newPlayer(playback = LightAudioPlayback.Detached)
        val silenceFile = SilenceAudio.file(filesDir, durationSeconds)

        player.setMediaQueue(
            listOf(
                LightAudioItem(
                    source = LightAudioSource.FileSource(silenceFile),
                    metadata = LightMediaMetadata(title = "Pomodoro timer"),
                ),
            ),
        )
        player.play()
        sessionPlayer = player
    }

    // Stops the current session track without starting a new one. Call on
    // pause, reset, skip, or when backing out to setup.
    fun stopSession() {
        alarmLoopJob?.cancel()
        alarmLoopJob = null
        sessionPlayer?.let {
            it.stop()
            it.release()
        }
        sessionPlayer = null
    }

    // Plays a short, one-shot feedback sound (click, chime). Used for
    // start/pause/skip clicks and the focus-ended alert.
    fun playAlert(assetPath: String) {
        alertPlayer.setMediaQueue(
            listOf(
                LightAudioItem(
                    source = LightAudioSource.AssetSource(assetPath),
                    metadata = LightMediaMetadata(title = "Pomodoro alert"),
                ),
            ),
        )
        alertPlayer.play()
    }

    // Repeats a sound until the caller stops it (via stopSession/startSession),
    // used for the break-ended alarm.
    fun startAlarmLoop(assetPath: String) {
        alarmLoopJob?.cancel()
        alarmLoopJob = scope.launch {
            while (true) {
                playAlert(assetPath)
                delay(200)
                alertPlayer.isPlaying.first { !it }
            }
        }
    }

    // Releases everything. Deliberately does NOT stop the detached session
    // track — releasing a detached handle disconnects without stopping
    // playback, which is exactly what keeps the "timer" alive after this
    // controller (and its owning ViewModel) is destroyed.
    fun release() {
        alertPlayer.release()
        sessionPlayer?.release()
    }
}
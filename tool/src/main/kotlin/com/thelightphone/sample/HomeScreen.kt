package com.thelightphone.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.audio.rememberLightAudio
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController

@InitialScreen
class HomeScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, PomodoroViewModel>(sealedActivity) {

    override val viewModelClass: Class<PomodoroViewModel>
        get() = PomodoroViewModel::class.java

    override fun createViewModel(): PomodoroViewModel {
        return PomodoroViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        val audio = rememberLightAudio()

        DisposableEffect(audio) {
            val player = audio.newPlayer()
            viewModel.attachAudioPlayer(player)
            onDispose { player.release() }
        }

        LaunchedEffect(state.pendingSound) {
            if (state.pendingSound != null) {
                viewModel.clearPendingSound()
            }
        }

        LightTheme(colors = themeColors) {
            if (state.showSetupScreen) {
                SetupScreenContent(
                    state = state,
                    viewModel = viewModel,
                    onHistoryClick = { navigateTo(::StatsScreen) },
                )
            } else {
                RunningScreenContent(state = state, viewModel = viewModel)
            }
        }
    }
}
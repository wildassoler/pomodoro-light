package com.thelightphone.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.audio.DefaultLightAudio
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController

@InitialScreen
class HomeScreen(private val sealedActivity: SealedLightActivity) : LightScreen<Unit, PomodoroViewModel>(sealedActivity) {

    override val viewModelClass: Class<PomodoroViewModel>
        get() = PomodoroViewModel::class.java

    override fun createViewModel(): PomodoroViewModel {
        return PomodoroViewModel(lightContext.dataStore, DefaultLightAudio(sealedActivity))
    }

    @Composable
    override fun Content() {
        val state by viewModel.state.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

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
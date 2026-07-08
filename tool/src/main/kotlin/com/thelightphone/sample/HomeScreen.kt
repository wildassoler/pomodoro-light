package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

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

        LightTheme(colors = themeColors) {
            if (state.showSetupScreen) {
                SetupScreenContent(state = state, viewModel = viewModel)
            } else {
                RunningScreenContent(state = state, viewModel = viewModel)
            }
        }
    }
}

private enum class PickerTarget { FOCUS, BREAK }

@Composable
private fun SetupScreenContent(state: PomodoroState, viewModel: PomodoroViewModel) {
    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val modeLabel = if (state.mode == PomodoroMode.FOCUS) "Focus" else "Break"

            LightText(
                text = modeLabel,
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            val timeLabel = "%02d:%02d".format(minutes, seconds)

            LightText(
                text = timeLabel,
                variant = LightTextVariant.Heading,
                modifier = Modifier
                    .scale(1.8f)
                    .padding(vertical = 24.dp),
            )

            LightText(
                text = "Start",
                variant = LightTextVariant.Copy,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 24.dp)
                    .lightClickable { viewModel.start() },
            )

            MinutesDropdown(
                label = "Focus",
                selectedMinutes = state.focusMinutes,
                enabled = true,
                onClick = { activePicker = PickerTarget.FOCUS },
                modifier = Modifier
                    .width(160.dp)
                    .padding(bottom = 12.dp),
            )

            MinutesDropdown(
                label = "Break",
                selectedMinutes = state.breakMinutes,
                enabled = true,
                onClick = { activePicker = PickerTarget.BREAK },
                modifier = Modifier.width(160.dp),
            )

            LightText(
                text = "Completed today: ${state.pomodorosToday}",
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        when (activePicker) {
            PickerTarget.FOCUS -> MinutesPickerOverlay(
                options = (15..60 step 5).toList(),
                selectedMinutes = state.focusMinutes,
                onSelected = { viewModel.setFocusMinutes(it) },
                onDismiss = { activePicker = null },
            )
            PickerTarget.BREAK -> MinutesPickerOverlay(
                options = (5..30 step 5).toList(),
                selectedMinutes = state.breakMinutes,
                onSelected = { viewModel.setBreakMinutes(it) },
                onDismiss = { activePicker = null },
            )
            null -> Unit
        }
    }
}

@Composable
private fun RunningScreenContent(state: PomodoroState, viewModel: PomodoroViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background)
    ) {
        LightIcon(
            icon = LightIcons.BACK,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .lightClickable { viewModel.backToSetup() },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            val timeLabel = "%02d:%02d".format(minutes, seconds)

            val totalSecondsForMode = if (state.mode == PomodoroMode.FOCUS) {
                state.focusMinutes * 60
            } else {
                state.breakMinutes * 60
            }
            val progress = if (totalSecondsForMode > 0) {
                state.remainingSeconds.toFloat() / totalSecondsForMode.toFloat()
            } else {
                0f
            }

            val skipLabel = if (state.mode == PomodoroMode.FOCUS) "Skip to Break" else "Skip to Focus"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressRing(
                    progress = progress,
                    trackColor = LightThemeTokens.colors.background,
                    progressColor = LightThemeTokens.colors.content,
                    modifier = Modifier.fillMaxSize(),
                )

                LightText(
                    text = timeLabel,
                    variant = LightTextVariant.Heading,
                    modifier = Modifier.scale(1.4f),
                )
            }

            val startPauseLabel = if (state.isRunning) "Pause" else "Start"

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LightText(
                    text = startPauseLabel,
                    variant = LightTextVariant.Copy,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .lightClickable {
                            if (state.isRunning) viewModel.pause() else viewModel.start()
                        },
                )

                LightText(
                    text = skipLabel,
                    variant = LightTextVariant.Copy,
                    modifier = Modifier.lightClickable { viewModel.skipToNextPhase() },
                )
            }
        }
    }
}
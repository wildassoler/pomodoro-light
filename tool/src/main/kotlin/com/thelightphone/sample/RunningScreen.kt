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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

@Composable
internal fun RunningScreenContent(state: PomodoroState, viewModel: PomodoroViewModel) {
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
            val timeLabel = state.remainingSeconds.toTimeLabel()
            val progress = state.progress
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
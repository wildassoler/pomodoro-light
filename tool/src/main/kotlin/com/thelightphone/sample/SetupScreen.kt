package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border

// Which minutes picker (if any) is currently open as an overlay
internal enum class PickerTarget { FOCUS, BREAK }

// The initial screen: lets the user configure focus/break length,
// and jump into a run or the history screen.
@Composable
internal fun SetupScreenContent(
    state: PomodoroState,
    viewModel: PomodoroViewModel,
    onHistoryClick: () -> Unit,
) {
    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val modeLabel = if (state.mode == PomodoroMode.FOCUS) "Focus" else "Break"

            LightText(
                text = modeLabel,
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            val timeLabel = state.remainingSeconds.toTimeLabel()

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
                    .width(200.dp)
                    .padding(bottom = 12.dp),
            )

            MinutesDropdown(
                label = "Break",
                selectedMinutes = state.breakMinutes,
                enabled = true,
                onClick = { activePicker = PickerTarget.BREAK },
                modifier = Modifier
                    .width(200.dp)
                    .padding(bottom = 12.dp),
            )

            LightText(
                text = "History",
                variant = LightTextVariant.Copy,
                align = TextAlign.Center,
                modifier = Modifier
                    .width(200.dp)
                    .background(
                        color = LightThemeTokens.colors.content.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        border = BorderStroke(1.dp, LightThemeTokens.colors.content.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .lightClickable { onHistoryClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
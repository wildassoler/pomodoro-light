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
    // Local UI-only state: tracks which picker overlay is open, if any.
    // Doesn't live in the ViewModel because it has no meaning outside this screen.
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
            // Shows which phase the upcoming timer will run in
            val modeLabel = if (state.mode == PomodoroMode.FOCUS) "Focus" else "Break"

            LightText(
                text = modeLabel,
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Preview of the countdown, formatted as MM:SS
            val timeLabel = state.remainingSeconds.toTimeLabel()

            LightText(
                text = timeLabel,
                variant = LightTextVariant.Heading,
                modifier = Modifier
                    .scale(1.8f)
                    .padding(vertical = 24.dp),
            )

            // Starts the timer and switches to the running screen
            LightText(
                text = "Start",
                variant = LightTextVariant.Copy,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 24.dp)
                    .lightClickable { viewModel.start() },
            )

            // Opens the focus-length picker overlay
            MinutesDropdown(
                label = "Focus",
                selectedMinutes = state.focusMinutes,
                enabled = true,
                onClick = { activePicker = PickerTarget.FOCUS },
                modifier = Modifier
                    .width(160.dp)
                    .padding(bottom = 12.dp),
            )

            // Opens the break-length picker overlay
            MinutesDropdown(
                label = "Break",
                selectedMinutes = state.breakMinutes,
                enabled = true,
                onClick = { activePicker = PickerTarget.BREAK },
                modifier = Modifier
                    .width(160.dp)
                    .padding(bottom = 12.dp),
            )

            // Navigates to a real, separate LightScreen (with native back button),
            // unlike Setup/Running which just swap Content within this same screen.
            // Styled like the pill buttons above for visual consistency.
            LightText(
                text = "History",
                variant = LightTextVariant.Copy,
                align = TextAlign.Center,
                modifier = Modifier
                    .width(160.dp)
                    .background(
                        color = LightThemeTokens.colors.content.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .lightClickable { onHistoryClick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        // Overlay picker, rendered on top of everything else when active
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
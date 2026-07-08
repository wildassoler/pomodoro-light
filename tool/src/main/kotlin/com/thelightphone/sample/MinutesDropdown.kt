package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

// Trigger button that shows the current selection. Tapping it notifies the
// parent screen, which is responsible for rendering the picker overlay
// (see MinutesPickerOverlay) full-screen and centred.
@Composable
fun MinutesDropdown(
    label: String,
    selectedMinutes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightText(
        text = "$label: $selectedMinutes min",
        variant = LightTextVariant.Copy,
        modifier = modifier
            .background(
                color = LightThemeTokens.colors.content.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            )
            .lightClickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
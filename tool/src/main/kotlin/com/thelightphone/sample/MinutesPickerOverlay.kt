package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable

// Full-screen overlay showing a scrollable, centred list of minute options
// with a dimmed scrim behind it. The currently selected value is highlighted.
@Composable
fun MinutesPickerOverlay(
    options: List<Int>,
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .lightClickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(options) { minutes ->
                val isSelected = minutes == selectedMinutes

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .lightClickable {
                            onSelected(minutes)
                            onDismiss()
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LightText(
                        text = "$minutes min",
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .wrapContentWidth()
                            .width(100.dp)
                            .background(
                                color = if (isSelected) {
                                    LightThemeTokens.colors.content.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
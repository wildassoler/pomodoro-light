package com.thelightphone.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

// A thin circular progress ring. progress goes from 1f (full, just started)
// down to 0f (empty, time's up).
@Composable
fun CircularProgressRing(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 6f,
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidthDp.dp.toPx()
        val diameter = size.minDimension - strokeWidthPx
        val topLeftOffset = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        // Background track (the full circle, always visible)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeftOffset,
            size = arcSize,
            style = Stroke(width = strokeWidthPx),
        )

        // Foreground progress (shrinks as time passes)
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeftOffset,
            size = arcSize,
            style = Stroke(width = strokeWidthPx),
        )
    }
}

private val Float.dp
    get() = androidx.compose.ui.unit.Dp(this)
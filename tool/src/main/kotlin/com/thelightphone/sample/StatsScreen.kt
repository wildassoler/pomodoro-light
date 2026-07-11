package com.thelightphone.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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

class StatsScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, StatsViewModel>(sealedActivity) {

    override val viewModelClass: Class<StatsViewModel>
        get() = StatsViewModel::class.java

    override fun createViewModel(): StatsViewModel {
        return StatsViewModel(lightContext.dataStore)
    }

    @Composable
    override fun Content() {
        val history by viewModel.history.collectAsState()
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(24.dp),
            ) {
                // Back arrow and title share the same row, top-left / top-right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    LightIcon(
                        icon = LightIcons.BACK,
                        modifier = Modifier.lightClickable { goBack() },
                    )

                    LightText(
                        text = "History",
                        variant = LightTextVariant.Heading,
                    )
                }

                // DEV-ONLY: see TestDataSeeder. Remove before shipping.
                LightText(
                    text = "Seed test data",
                    variant = LightTextVariant.Detail,
                    lighten = true,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .lightClickable { viewModel.seedTestData() },
                )

                val lastSevenDays = buildLastSevenDays(history)

                WeekBarChart(
                    days = lastSevenDays,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(bottom = 24.dp),
                )

                val daysWithActivity = history.filter { it.pomodorosCompleted > 0 }

                if (daysWithActivity.isEmpty()) {
                    LightText(
                        text = "No history yet.",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(daysWithActivity) { day ->
                            DayRow(day = day)
                        }
                    }
                }
            }
        }
    }
}

// Builds a 7-entry list for the last 7 calendar days, filling in zeros for
// any day with no saved record. Always returns 7 items so the chart's shape
// stays consistent, even with no history yet.
private fun buildLastSevenDays(history: List<DailyStats>): List<DailyStats> {
    val today = java.time.LocalDate.now()
    return (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong()).toString()
        history.find { it.date == date } ?: DailyStats(date, 0, 0)
    }
}

@Composable
private fun WeekBarChart(days: List<DailyStats>, modifier: Modifier = Modifier) {
    val maxPomodoros = (days.maxOfOrNull { it.pomodorosCompleted } ?: 0).coerceAtLeast(1)

    Box(modifier = modifier) {
        // Subtle baseline at the bottom of the chart
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(LightThemeTokens.colors.content.copy(alpha = 0.15f)),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEach { day ->
                val heightFraction = day.pomodorosCompleted.toFloat() / maxPomodoros.toFloat()

                Column(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    LightText(
                        text = "${day.pomodorosCompleted}",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(LightThemeTokens.colors.content),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DailyStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LightText(
            text = day.date.toReadableDate(),
            variant = LightTextVariant.Copy,
        )

        LightText(
            text = "${day.pomodorosCompleted} pomodoros · ${day.focusMinutes} min",
            variant = LightTextVariant.Copy,
            lighten = true,
        )
    }
}
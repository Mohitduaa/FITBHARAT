package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyLogEntity
import com.example.data.local.DailyMealTotals
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WaterBlue
import com.example.ui.theme.WaterBlueDark
import com.example.util.minusDays
import com.example.util.toDisplayString
import com.example.util.toFixed
import com.example.util.toKey
import com.example.util.todayDate
import com.example.util.withCommas
import kotlinx.datetime.DayOfWeek

private enum class Metric(val label: String) { CALORIES("Calories"), WATER("Water"), STEPS("Steps") }

private fun DayOfWeek.letter() = when (this) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}

/**
 * Daily history of calories, water or steps over the last 7 or 30 days against the daily goal (dashed line).
 * Bars that reach the goal turn green; for calories, bars well over the target use a warmer colour.
 */
@Composable
fun HistoryChartCard(
    dailyLogs: List<DailyLogEntity>,
    mealTotals: List<DailyMealTotals>,
    calorieTarget: Int,
    waterGoalMl: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    var metricIndex by rememberSaveable { mutableIntStateOf(0) }
    var rangeDays by rememberSaveable { mutableIntStateOf(7) }
    val metric = Metric.entries[metricIndex]

    val today = remember { todayDate() }
    var selected by remember(rangeDays) { mutableIntStateOf(rangeDays - 1) }
    val days = remember(rangeDays) { (rangeDays - 1 downTo 0).map { today.minusDays(it) } }
    val logsByDate = remember(dailyLogs) { dailyLogs.associateBy { it.date } }
    val mealsByDate = remember(mealTotals) { mealTotals.associateBy { it.date } }

    val values = days.map { day ->
        val key = day.toKey()
        when (metric) {
            Metric.CALORIES -> mealsByDate[key]?.calories ?: 0
            Metric.WATER -> logsByDate[key]?.waterMl ?: 0
            Metric.STEPS -> logsByDate[key]?.steps ?: 0
        }
    }
    val goal = when (metric) {
        Metric.CALORIES -> calorieTarget
        Metric.WATER -> waterGoalMl
        Metric.STEPS -> stepGoal
    }
    val tracked = values.filter { it > 0 }
    val average = if (tracked.isEmpty()) 0 else tracked.sum() / tracked.size
    val daysAtGoal = when (metric) {
        // "On target" means eating roughly what the plan asks for, not just staying under it.
        Metric.CALORIES -> values.count { it >= goal * 0.8f && it <= goal * 1.1f }
        else -> values.count { it >= goal }
    }
    val scaleMax = maxOf(goal * 1.3f, (values.maxOrNull() ?: 0) * 1.1f, 1f)

    val primary = MaterialTheme.colorScheme.primary
    // Not secondaryContainer: that is near-black and disappears on the dark theme.
    val over = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.surfaceVariant
    val goalLine = MaterialTheme.colorScheme.onSurfaceVariant
    val water = if (isSystemInDarkTheme()) WaterBlueDark else WaterBlue
    val barColor = when (metric) {
        Metric.CALORIES -> primary
        Metric.WATER -> water
        Metric.STEPS -> primary
    }

    fun format(value: Int) = when (metric) {
        Metric.CALORIES -> "${value.withCommas()} kcal"
        Metric.WATER -> "${(value / 1000.0).toFixed(1)} L"
        Metric.STEPS -> value.withCommas()
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("history_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("HISTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = primary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Last $rangeDays days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (tracked.isEmpty()) "No data yet" else "Avg ${format(average)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric.entries.forEach { m ->
                    FilterChip(
                        selected = metric == m,
                        onClick = { metricIndex = m.ordinal },
                        label = { Text(m.label) },
                        modifier = Modifier.testTag("history_metric_${m.name.lowercase()}")
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30).forEach { range ->
                    FilterChip(
                        selected = rangeDays == range,
                        onClick = { rangeDays = range },
                        label = { Text("$range days") },
                        modifier = Modifier.testTag("history_range_$range")
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            if (rangeDays <= 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    values.forEach { value ->
                        Text(
                            when {
                                value <= 0 -> ""
                                metric == Metric.WATER -> (value / 1000.0).toFixed(1)
                                value >= 1000 -> "${value / 100 / 10.0}k"
                                else -> "$value"
                            },
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(values.size) {
                        detectTapGestures { tap ->
                            selected = (tap.x / (size.width / values.size)).toInt().coerceIn(0, values.lastIndex)
                        }
                    }
                    .testTag("history_canvas")
            ) {
                val slot = size.width / values.size
                val barWidth = slot * if (rangeDays <= 7) 0.5f else 0.62f
                values.forEachIndexed { index, value ->
                    val left = index * slot + (slot - barWidth) / 2
                    val radius = CornerRadius(barWidth / 2)
                    if (index == selected) {
                        drawRoundRect(
                            color = primary.copy(alpha = 0.12f),
                            topLeft = Offset(index * slot, 0f),
                            size = Size(slot, size.height),
                            cornerRadius = CornerRadius(slot / 2)
                        )
                    }
                    drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), radius)
                    if (value > 0) {
                        val h = (value / scaleMax).coerceAtMost(1f) * size.height
                        val color = when (metric) {
                            Metric.CALORIES -> if (value > goal * 1.1f) over else primary
                            else -> if (value >= goal) SuccessGreen else barColor
                        }
                        drawRoundRect(color, Offset(left, size.height - h), Size(barWidth, h), radius)
                    }
                }
                val goalY = size.height - (goal / scaleMax) * size.height
                drawLine(
                    color = goalLine,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    val last = index == days.lastIndex
                    val label = when {
                        rangeDays <= 7 -> day.dayOfWeek.letter()
                        index % 5 == 0 || last -> "${day.day}"
                        else -> ""
                    }
                    Text(
                        label,
                        fontSize = if (rangeDays <= 7) 12.sp else 10.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selected) primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        // A 30-day slot is narrower than a two-digit date, so let the label spill over evenly.
                        modifier = Modifier.weight(1f).wrapContentWidth(unbounded = true)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val reachedLabel = if (metric == Metric.CALORIES) "Within target" else "Goal reached"
                Legend(if (metric == Metric.CALORIES) primary else SuccessGreen, reachedLabel)
                if (metric == Metric.CALORIES) Legend(over, "Over target") else Legend(barColor, "Below goal")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Goal ${format(goal)} · $daysAtGoal of $rangeDays days on target",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // The tapped day, with everything logged for it.
            val day = days[selected.coerceIn(0, days.lastIndex)]
            val dayKey = day.toKey()
            val dayLog = logsByDate[dayKey]
            val dayName = when (day) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> day.toDisplayString()
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(14.dp)
                    .testTag("history_day_detail")
            ) {
                Text(dayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Tap a bar to see another day",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayStat("Steps", (dayLog?.steps ?: 0).withCommas(), Modifier.weight(1f))
                    DayStat("Calories eaten", "${(mealsByDate[dayKey]?.calories ?: 0).withCommas()} kcal", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayStat("Water", "${((dayLog?.waterMl ?: 0) / 1000.0).toFixed(1)} L", Modifier.weight(1f))
                    DayStat("Burned", "${(dayLog?.caloriesBurned ?: 0).withCommas()} kcal", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

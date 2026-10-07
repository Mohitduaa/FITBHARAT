package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyMealTotals
import com.example.util.minusDays
import com.example.util.toKey
import com.example.util.todayDate
import com.example.util.withCommas
import kotlinx.datetime.DayOfWeek

private fun DayOfWeek.letter() = when (this) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}

/** Calories eaten on each of the last 7 days against the daily target (dashed line). */
@Composable
fun WeeklyCaloriesChart(totals: List<DailyMealTotals>, calorieTarget: Int, modifier: Modifier = Modifier) {
    val today = todayDate()
    val byDate = totals.associateBy { it.date }
    val days = (6 downTo 0).map { today.minusDays(it) }
    val values = days.map { byDate[it.toKey()]?.calories ?: 0 }
    val logged = values.filter { it > 0 }
    val average = if (logged.isEmpty()) 0 else logged.sum() / logged.size
    val scaleMax = maxOf(calorieTarget * 1.3f, (values.maxOrNull() ?: 0) * 1.1f, 1f)

    val accent = MaterialTheme.colorScheme.primary
    val over = MaterialTheme.colorScheme.secondaryContainer
    val track = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth().testTag("weekly_calories_chart"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("CALORIES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accent)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (logged.isEmpty()) "No meals logged" else "Avg ${average.withCommas()} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            // Values above the bars
            Row(modifier = Modifier.fillMaxWidth()) {
                values.forEach { value ->
                    Text(
                        if (value > 0) (if (value >= 1000) "${value / 100 / 10.0}k" else "$value") else "",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val slot = size.width / values.size
                val barWidth = slot * 0.5f
                values.forEachIndexed { index, value ->
                    val left = index * slot + (slot - barWidth) / 2
                    drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                    if (value > 0) {
                        val h = (value / scaleMax).coerceAtMost(1f) * size.height
                        drawRoundRect(
                            color = if (value > calorieTarget * 1.1f) over else accent,
                            topLeft = Offset(left, size.height - h),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(barWidth / 2)
                        )
                    }
                }
                val targetY = size.height - (calorieTarget / scaleMax) * size.height
                drawLine(
                    color = line,
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    Text(
                        day.dayOfWeek.letter(),
                        fontSize = 12.sp,
                        fontWeight = if (index == days.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(accent, "Within target")
                Legend(over, "Over target")
                Text("- - Target ${calorieTarget.withCommas()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

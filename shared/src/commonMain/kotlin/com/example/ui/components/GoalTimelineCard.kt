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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalTimeline
import com.example.data.model.GoalType
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.SuccessGreen
import com.example.util.toDisplayString
import com.example.util.toFixed
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** "You'll reach 70 kg by 12 Mar 2027": hero date, stats and a chart of weigh-ins plus the projected path. */
@Composable
fun GoalTimelineCard(timeline: GoalTimeline, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val onTrack = !timeline.wrongWay && !timeline.reached
    val (statusText, statusColor) = when {
        timeline.reached -> "Goal reached" to SuccessGreen
        timeline.goal == GoalType.MAINTAIN -> "Maintaining" to SuccessGreen
        timeline.wrongWay -> "Off track" to CarbsAmber
        timeline.fromTrend -> "On track" to SuccessGreen
        else -> "Plan pace" to primary
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("goal_timeline_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GOAL TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = primary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(6.dp))
                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }

            // Hero
            Spacer(Modifier.height(14.dp))
            val target = "${timeline.targetKg.toFixed(1)} kg"
            when {
                timeline.reached -> {
                    Text("You're at your goal", style = MaterialTheme.typography.bodyMedium, color = muted)
                    Text(target, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                }
                timeline.etaDate != null -> {
                    Text(
                        if (timeline.wrongWay) "Back on plan, you'd reach $target by" else "You'll reach $target by",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted
                    )
                    Text(timeline.etaDate.toDisplayString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("in about ${weeksText(timeline.etaDays!!)}", style = MaterialTheme.typography.bodySmall, color = muted)
                }
                else -> {
                    Text("Target", style = MaterialTheme.typography.bodyMedium, color = muted)
                    Text(target, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Chart
            if (timeline.history.isNotEmpty() || timeline.etaDays != null) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)
                ) {
                    TimelineChart(timeline, primary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendLine(primary, dashed = false, label = "Actual")
                        LegendLine(primary, dashed = true, label = "Projected")
                        LegendLine(SuccessGreen, dashed = true, label = "Target")
                    }
                }
            }

            // Stats
            Spacer(Modifier.height(12.dp))
            val pace = timeline.ratePerWeek
            val paceGood = when (timeline.goal) {
                GoalType.LOSE -> pace < 0
                GoalType.GAIN -> pace > 0
                GoalType.MAINTAIN -> abs(pace) < 0.25
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Now", timeline.currentKg.toFixed(1), "kg", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                StatTile("To go", timeline.remainingKg.toFixed(1), "kg", primary, Modifier.weight(1f))
                StatTile(
                    if (timeline.fromTrend) "Your pace" else "Plan pace",
                    "${if (pace > 0) "+" else if (pace < 0) "−" else ""}${abs(pace).toFixed(1)}",
                    "kg/wk",
                    if (paceGood) SuccessGreen else CarbsAmber,
                    Modifier.weight(1f)
                )
            }

            // Insight
            val insight = when {
                timeline.reached -> "Great work. Keep your habits steady to stay here."
                timeline.goal == GoalType.MAINTAIN -> "Stay within ±1 kg of your target by keeping your calories and steps steady."
                timeline.wrongWay && timeline.fromTrend -> "Your weigh-ins are moving away from the goal. Stick to your calorie target and daily steps to turn it around."
                timeline.wrongWay -> "Log your weight every week to see your real timeline."
                timeline.fromTrend -> "Based on your weigh-ins over the last 6 weeks. Keep going!"
                else -> "Based on your plan. Log your weight weekly for a timeline from your real progress."
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (onTrack) Icons.Default.Insights else Icons.Default.Flag,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(insight, style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
    }
}

private fun weeksText(days: Int): String = when {
    days < 14 -> "$days days"
    days < 60 -> "${(days + 6) / 7} weeks"
    else -> "${(days + 15) / 30} months"
}

@Composable
private fun StatTile(label: String, value: String, unit: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            Text(unit, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp), maxLines = 1)
        }
    }
}

@Composable
private fun LegendLine(color: Color, dashed: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(16.dp).height(8.dp)) {
            drawLine(
                color,
                Offset(0f, size.height / 2),
                Offset(size.width, size.height / 2),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) else null
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimelineChart(timeline: GoalTimeline, lineColor: Color) {
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val history = timeline.history
    val firstDay = min(history.firstOrNull()?.day ?: 0, 0)
    val lastDay = max(timeline.etaDays ?: 0, if (firstDay < 0) 0 else 7)
    val weights = history.map { it.weightKg } + timeline.currentKg + timeline.targetKg
    // Round the axis to whole kilos with a little headroom.
    val minW = floor(weights.min() - 1.0)
    val maxW = ceil(weights.max() + 1.0)

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val axisWidth = 30.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val chartW = size.width - axisWidth
        val chartH = size.height - bottomPad
        val span = (lastDay - firstDay).coerceAtLeast(1)
        fun x(day: Int) = (day - firstDay).toFloat() / span * chartW
        fun y(kg: Double) = ((maxW - kg) / (maxW - minW)).toFloat() * chartH

        // Grid lines with kg labels on the right
        for (i in 0..3) {
            val kg = maxW - (maxW - minW) * i / 3
            val gy = y(kg)
            drawLine(grid, Offset(0f, gy), Offset(chartW, gy), 1.dp.toPx())
            val text = measurer.measure(kg.toFixed(0), labelStyle)
            drawText(text, topLeft = Offset(chartW + 6.dp.toPx(), gy - text.size.height / 2f))
        }

        // Target line
        val ty = y(timeline.targetKg)
        drawLine(SuccessGreen, Offset(0f, ty), Offset(chartW, ty), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))

        // Actual weigh-ins: smooth line with a soft gradient underneath
        val points = history.map { Offset(x(it.day), y(it.weightKg)) }
        if (points.size >= 2) {
            val line = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val cur = points[i]
                    val midX = (prev.x + cur.x) / 2
                    cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, chartH)
                lineTo(points.first().x, chartH)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)), endY = chartH))
            drawPath(line, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }

        // Projection from today to the target date
        val now = Offset(x(0), y(timeline.currentKg))
        timeline.etaDays?.let { eta ->
            val end = Offset(x(eta), ty)
            drawLine(lineColor.copy(alpha = 0.7f), now, end, 2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f)))
            drawCircle(SuccessGreen.copy(alpha = 0.25f), 10.dp.toPx(), end)
            drawCircle(SuccessGreen, 5.dp.toPx(), end)
        }

        // Today marker
        drawCircle(surface, 7.dp.toPx(), now)
        drawCircle(lineColor, 5.dp.toPx(), now)

        // X-axis labels: start, today, goal date
        val y0 = chartH + 5.dp.toPx()
        fun label(text: String, cx: Float, align: Int) {
            val m = measurer.measure(text, labelStyle)
            val left = when (align) {
                0 -> cx
                1 -> cx - m.size.width / 2f
                else -> cx - m.size.width
            }.coerceIn(0f, chartW - m.size.width)
            drawText(m, topLeft = Offset(left, y0))
        }
        // Skip the start label when "Today" sits too close to it.
        if (firstDay < 0 && x(0) > 80.dp.toPx()) label("${-firstDay}d ago", 0f, 0)
        val todayAlign = if (firstDay < 0 && timeline.etaDays != null) 1 else if (firstDay < 0) 2 else 0
        label("Today", x(0), todayAlign)
        timeline.etaDate?.let { label(it.toDisplayString(), chartW, 2) }
    }
}

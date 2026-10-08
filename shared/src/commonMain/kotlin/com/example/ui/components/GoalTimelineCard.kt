package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalTimeline
import com.example.data.model.GoalType
import com.example.ui.theme.SuccessGreen
import com.example.util.toDisplayString
import com.example.util.toFixed
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** "At this pace you reach 70 kg by 12 Mar 2027", with past weigh-ins and the projected line to the target. */
@Composable
fun GoalTimelineCard(timeline: GoalTimeline, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxWidth().testTag("goal_timeline_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("GOAL TIMELINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = primary)
            val headline = when {
                timeline.reached -> "You've reached ${timeline.targetKg.toFixed(1)} kg!"
                timeline.goal == GoalType.MAINTAIN -> "Holding near ${timeline.targetKg.toFixed(1)} kg"
                timeline.wrongWay -> "Not moving towards ${timeline.targetKg.toFixed(1)} kg yet"
                else -> "${timeline.targetKg.toFixed(1)} kg by ${timeline.etaDate!!.toDisplayString()}"
            }
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (timeline.reached) SuccessGreen else MaterialTheme.colorScheme.onSurface)
            val pace = "${if (timeline.ratePerWeek > 0) "+" else if (timeline.ratePerWeek < 0) "-" else ""}${abs(timeline.ratePerWeek).toFixed(2)} kg/week"
            val sub = when {
                timeline.reached -> "Great work. Keep your habits steady to stay here."
                timeline.goal == GoalType.MAINTAIN -> "Current pace: $pace"
                timeline.wrongWay && timeline.fromTrend && timeline.etaDate != null ->
                    "Your recent weigh-ins show $pace. Back on your plan (${abs(timeline.plannedRate).toFixed(1)} kg/week), you can reach it by ${timeline.etaDate.toDisplayString()}."
                timeline.wrongWay -> "Log your weight every week to see your timeline."
                timeline.fromTrend -> "At your current pace ($pace from your weigh-ins), ${timeline.remainingKg.toFixed(1)} kg to go, about ${weeksText(timeline.etaDays!!)}."
                else -> "At your plan pace ($pace), ${timeline.remainingKg.toFixed(1)} kg to go, about ${weeksText(timeline.etaDays!!)}. Log weight weekly for a real trend."
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (timeline.history.isNotEmpty() || timeline.etaDays != null) {
                Spacer(Modifier.height(16.dp))
                TimelineChart(timeline, primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Legend(primary, "Your weigh-ins")
                    Spacer(Modifier.width(16.dp))
                    Legend(primary.copy(alpha = 0.5f), "Projected")
                    Spacer(Modifier.width(16.dp))
                    Legend(SuccessGreen, "Target")
                }
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
private fun Legend(color: Color, label: String) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(4.dp))
    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TimelineChart(timeline: GoalTimeline, lineColor: Color) {
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val history = timeline.history
    val firstDay = min(history.firstOrNull()?.day ?: 0, 0)
    val lastDay = max(timeline.etaDays ?: 0, 7)
    val weights = history.map { it.weightKg } + timeline.currentKg + timeline.targetKg
    val minW = weights.min() - 1.0
    val maxW = weights.max() + 1.0

    Row {
        Column(modifier = Modifier.height(140.dp).width(36.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(maxW.toFixed(0), fontSize = 10.sp, color = labelColor)
            Text(minW.toFixed(0), fontSize = 10.sp, color = labelColor)
        }
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                fun x(day: Int) = (day - firstDay).toFloat() / (lastDay - firstDay).coerceAtLeast(1) * size.width
                fun y(kg: Double) = ((maxW - kg) / (maxW - minW)).toFloat() * size.height

                // Target line
                drawLine(SuccessGreen, Offset(0f, y(timeline.targetKg)), Offset(size.width, y(timeline.targetKg)), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                // Today marker
                drawLine(grid, Offset(x(0), 0f), Offset(x(0), size.height), 1.dp.toPx())

                // Actual weigh-ins
                if (history.size >= 2) {
                    val path = Path()
                    history.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p.day), y(p.weightKg)) else path.lineTo(x(p.day), y(p.weightKg)) }
                    drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
                history.forEach { drawCircle(lineColor, 3.5.dp.toPx(), Offset(x(it.day), y(it.weightKg))) }

                // Projection from today to the target date
                timeline.etaDays?.let { eta ->
                    drawLine(
                        lineColor.copy(alpha = 0.5f),
                        Offset(x(0), y(timeline.currentKg)),
                        Offset(x(eta), y(timeline.targetKg)),
                        3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                    drawCircle(SuccessGreen, 6.dp.toPx(), Offset(x(eta), y(timeline.targetKg)))
                }
                drawCircle(lineColor, 5.dp.toPx(), Offset(x(0), y(timeline.currentKg)))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(if (firstDay < 0) "${-firstDay}d ago" else "Today", fontSize = 10.sp, color = labelColor, modifier = Modifier.weight(1f))
                Text(timeline.etaDate?.toDisplayString() ?: "", fontSize = 10.sp, color = labelColor)
            }
        }
    }
}

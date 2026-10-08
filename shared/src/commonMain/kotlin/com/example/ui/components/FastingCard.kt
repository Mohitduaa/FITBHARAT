package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FastingSessionEntity
import com.example.ui.theme.SuccessGreen
import com.example.util.currentTimeMillis
import com.example.util.millisToClock
import kotlinx.coroutines.delay

private val PLANS = listOf(12, 14, 16, 18, 20)

private fun duration(millis: Long): String {
    val totalMinutes = (millis / 60_000).coerceAtLeast(0)
    return "${totalMinutes / 60}h ${(totalMinutes % 60).toString().padStart(2, '0')}m"
}

/** Intermittent fasting: pick a plan (e.g. 16:8), start, and watch the ring fill until the eating window opens. */
@Composable
fun FastingCard(
    active: FastingSessionEntity?,
    lastCompleted: FastingSessionEntity?,
    onStart: (hours: Int) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(currentTimeMillis()) }
    LaunchedEffect(active?.id) {
        while (active != null) {
            now = currentTimeMillis()
            delay(1_000)
        }
    }
    var plan by rememberSaveable { mutableIntStateOf(16) }

    Card(
        modifier = modifier.fillMaxWidth().testTag("fasting_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("FASTING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            if (active == null) {
                Text("Intermittent fasting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    lastCompleted?.endMillis?.let { "Last fast: ${duration(it - lastCompleted.startMillis)}" }
                        ?: "Pick a plan: fasting hours : eating hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    PLANS.forEach { hours ->
                        FilterChip(
                            selected = plan == hours,
                            onClick = { plan = hours },
                            label = { Text("$hours:${24 - hours}", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("fast_plan_$hours")
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onStart(plan) }, modifier = Modifier.fillMaxWidth().testTag("fast_start")) {
                    Text("Start $plan:${24 - plan} fast")
                }
            } else {
                val target = active.targetHours * 3_600_000L
                val elapsed = now - active.startMillis
                val progress = (elapsed.toFloat() / target).coerceIn(0f, 1f)
                val done = elapsed >= target
                val ringColor = if (done) SuccessGreen else MaterialTheme.colorScheme.primary
                val track = MaterialTheme.colorScheme.surfaceVariant
                Text("${active.targetHours}:${24 - active.targetHours} fast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            val stroke = 10.dp.toPx()
                            val arc = Size(size.width - stroke, size.height - stroke)
                            val topLeft = Offset(stroke / 2, stroke / 2)
                            drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke))
                            drawArc(ringColor, -90f, 360f * progress, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(duration(elapsed), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Started ${millisToClock(active.startMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (done) {
                            Text("Goal reached!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Text("Your eating window is open.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("${duration(target - elapsed)} to go", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Ends at ${millisToClock(active.startMillis + target)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (done) {
                    Button(onClick = onEnd, modifier = Modifier.fillMaxWidth().testTag("fast_end")) { Text("End fast") }
                } else {
                    OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth().testTag("fast_end")) { Text("End fast early") }
                }
            }
        }
    }
}

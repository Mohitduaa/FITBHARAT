package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SuccessEmerald

/** One of the four things that make up the daily score. [progress] is 0..1, [achieved] means goal met. */
data class ScoreMetric(
    val label: String,
    val valueText: String,
    val progress: Float,
    val achieved: Boolean
)

/** Score out of 100: each metric is worth 25 points, earned in proportion to its progress. */
fun dailyScore(metrics: List<ScoreMetric>): Int =
    (metrics.sumOf { it.progress.coerceIn(0f, 1f).toDouble() } * 25).toInt().coerceIn(0, 100)

@Composable
fun DailyScoreCard(
    metrics: List<ScoreMetric>,
    modifier: Modifier = Modifier
) {
    val score = dailyScore(metrics)
    val message = when {
        metrics.all { it.progress == 0f } -> "Your day has just begun. Log meals, steps and water to build your score."
        score >= 85 -> "Excellent day. You are close to hitting all your targets."
        score >= 60 -> "Good progress. Focus on the targets still open."
        else -> "There is still time today. Small steps will raise your score."
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_score_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (score >= 60) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$score/100",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            metrics.chunked(2).forEachIndexed { index, row ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ScoreMetricTile(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun ScoreMetricTile(metric: ScoreMetric, modifier: Modifier = Modifier) {
    val color = if (metric.achieved) SuccessEmerald else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
            .testTag("score_${metric.label.lowercase()}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metric.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (metric.achieved) {
                Icon(Icons.Default.Check, contentDescription = "Goal met", tint = SuccessEmerald, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = metric.valueText,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { metric.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

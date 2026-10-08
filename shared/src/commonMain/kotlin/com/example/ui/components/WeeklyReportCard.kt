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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalType
import com.example.data.model.WeeklyReport
import com.example.ui.theme.SuccessGreen
import com.example.util.toFixed
import com.example.util.withCommas
import kotlin.math.abs

/** "This week" summary: weight change, calories, steps, water and fasts, with a short verdict. */
@Composable
fun WeeklyReportCard(report: WeeklyReport?, goal: GoalType, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("weekly_report_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("WEEKLY REPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            Row {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                report?.weightChangeKg?.let { change ->
                    val good = when (goal) {
                        GoalType.LOSE -> change <= 0
                        GoalType.GAIN -> change >= 0
                        GoalType.MAINTAIN -> abs(change) <= 0.3
                    }
                    val color = if (good) SuccessGreen else MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${if (change > 0) "+" else if (change < 0) "-" else ""}${abs(change).toFixed(1)} kg",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (report == null) return@Column
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReportStat("Avg calories", report.avgCalories?.let { "${it.withCommas()}" } ?: "—", "${report.calorieDaysOnTarget}/7 days on target", Modifier.weight(1f))
                ReportStat(
                    "Steps / day",
                    report.avgSteps.withCommas(),
                    report.prevAvgSteps?.let { prev ->
                        val diff = report.avgSteps - prev
                        if (diff >= 0) "▲ ${diff.withCommas()} vs last week" else "▼ ${(-diff).withCommas()} vs last week"
                    } ?: "${report.stepGoalDays}/7 days at goal",
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReportStat("Water goal", "${report.waterGoalDays}/7", "days reached", Modifier.weight(1f))
                ReportStat("Fasts", "${report.fastsCompleted}", "completed", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                report.verdict,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

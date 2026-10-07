package com.example.ui.screens

import com.example.util.toDisplayString
import androidx.compose.material3.TextButton
import com.example.data.model.ChallengeRequirement
import com.example.util.todayDate
import com.example.util.toKey
import com.example.util.minusDays
import com.example.util.toFixed
import com.example.util.withCommas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyLogEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightLogEntity
import com.example.data.model.DesiChallenge
import com.example.data.model.StreakInfo
import com.example.ui.components.StreaksCard
import com.example.data.model.goal
import com.example.ui.components.WeightTrackerOverviewCard
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WaterBlue

@Composable
fun ProgressScreen(
    todaySteps: Int,
    todayWaterMl: Int,
    badges: List<com.example.data.model.Badge>,
    mealTotals: List<com.example.data.local.DailyMealTotals>,
    calorieTarget: Int,
    streaks: List<StreakInfo>,
    profile: UserProfileEntity,
    weightLogs: List<WeightLogEntity>,
    recentDailyLogs: List<DailyLogEntity>,
    challenges: List<DesiChallenge>,
    onOpenLogWeightDialog: () -> Unit,
    onCheckInChallenge: (String) -> Unit,
    onToggleJoinChallenge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Weight & Progress Report, 1: Desi Challenges

    val avgSteps = remember(recentDailyLogs) {
        if (recentDailyLogs.isNotEmpty()) recentDailyLogs.map { it.steps }.average().toInt() else 0
    }
    val stepGoalDays = remember(recentDailyLogs, profile.stepGoal) {
        recentDailyLogs.count { it.steps >= profile.stepGoal }
    }
    val weeklyChange = remember(weightLogs) { weeklyWeightChange(weightLogs) }
    val avgWater = remember(recentDailyLogs) {
        if (recentDailyLogs.isNotEmpty()) recentDailyLogs.map { it.waterMl }.average().toInt() else 0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("progress_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${profile.goal().displayName} • weekly consistency & streaks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sub Tabs
        item {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Overview") },
                    modifier = Modifier.testTag("tab_weight_report")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Challenges (${challenges.count { it.isJoined }})") },
                    modifier = Modifier.testTag("tab_challenges")
                )
            }
        }

        if (selectedSubTab == 0) {
            item { StreaksCard(streaks = streaks) }

            item { com.example.ui.components.WeeklyCaloriesChart(totals = mealTotals, calorieTarget = calorieTarget) }

            item { com.example.ui.components.BadgesCard(badges = badges) }

            // Weight Tracker Card
            item {
                WeightTrackerOverviewCard(
                    currentWeight = profile.currentWeightKg,
                    startWeight = profile.startWeightKg,
                    targetWeight = profile.targetWeightKg,
                    heightCm = profile.heightCm,
                    weightLogs = weightLogs,
                    onLogWeightClick = onOpenLogWeightDialog,
                    goal = profile.goal(),
                    weeklyRateKg = profile.weeklyRateKg
                )
            }

            // Progress Report (Weekly Summary)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_report_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Weekly Progress Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Last 7 days consistency analysis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (weeklyChange != null) Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SuccessGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = weeklyChange,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeeklyStatBox("Avg Steps", "$avgSteps / day", MaterialTheme.colorScheme.primary)
                            WeeklyStatBox("Avg Water", "${(avgWater / 1000.0).toFixed(1)} L / day", WaterBlue)
                            WeeklyStatBox("Step Goal", "$stepGoalDays / ${recentDailyLogs.size.coerceAtLeast(1)} days", MaterialTheme.colorScheme.secondary)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Coach Weekly Verdict
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Weekly Review",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = weeklyReview(profile, recentDailyLogs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weight Log History
            item {
                Text(
                    text = "Weight Log History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(weightLogs.reversed()) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${log.weightKg} kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (log.note.isNotBlank()) {
                                Text(
                                    text = log.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = runCatching { kotlinx.datetime.LocalDate.parse(log.date).toDisplayString() }.getOrDefault(log.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Challenges Tab
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Challenges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Build consistency with small daily commitments.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(challenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    todaySteps = todaySteps,
                    todayWaterMl = todayWaterMl,
                    onCheckIn = { onCheckInChallenge(challenge.id) },
                    onToggleJoin = { onToggleJoinChallenge(challenge.id) }
                )
            }
        }
    }
}

@Composable
fun WeeklyStatBox(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: DesiChallenge,
    todaySteps: Int,
    todayWaterMl: Int,
    onCheckIn: () -> Unit,
    onToggleJoin: () -> Unit
) {
    val progress = (challenge.daysCompleted.toFloat() / challenge.totalDays.toFloat()).coerceIn(0f, 1f)
    // Requirement that must be met today before the check-in unlocks (verified from logged data).
    val pending: String? = when (val r = challenge.requirement) {
        is ChallengeRequirement.Steps -> if (todaySteps >= r.minimum) null else "${todaySteps.withCommas()} / ${r.minimum.withCommas()} steps today"
        is ChallengeRequirement.WaterMl -> if (todayWaterMl >= r.minimum) null else "${todayWaterMl.withCommas()} / ${r.minimum.withCommas()} ml today"
        null -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("challenge_${challenge.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (challenge.isCompleted) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (challenge.isJoined || challenge.isCompleted) "${challenge.daysCompleted}/${challenge.totalDays} days" else "${challenge.totalDays} days",
                        fontWeight = FontWeight.Bold,
                        color = if (challenge.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(challenge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (challenge.isJoined || challenge.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = if (challenge.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Daily rule: ${challenge.dailyAction}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                challenge.isCompleted -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Challenge completed", color = SuccessGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onCheckIn) { Text("Start again") }
                }

                !challenge.isJoined -> Button(
                    onClick = onToggleJoin,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("join_${challenge.id}")
                ) { Text("Join challenge") }

                else -> Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onCheckIn,
                            enabled = !challenge.checkedInToday && pending == null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("check_in_${challenge.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (challenge.checkedInToday) "Done for today" else "Mark today done")
                        }
                        TextButton(onClick = onToggleJoin, modifier = Modifier.testTag("leave_${challenge.id}")) { Text("Leave") }
                    }
                    if (pending != null && !challenge.checkedInToday) {
                        Text(
                            text = "Unlocks when you reach the target: $pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Plain-language summary of the last week's logs; no invented numbers. */
private fun weeklyReview(profile: UserProfileEntity, logs: List<DailyLogEntity>): String {
    val tracked = logs.filter { it.steps > 0 || it.waterMl > 0 || it.score > 0 }
    if (tracked.isEmpty()) {
        return "Not enough data yet. Log meals, steps and weight for a few days to see your weekly review."
    }
    val days = tracked.size
    val avgSteps = tracked.sumOf { it.steps } / days
    val stepDays = tracked.count { it.steps >= profile.stepGoal }
    val waterDays = tracked.count { it.waterMl >= profile.waterGoalMl }
    val name = profile.name.ifBlank { "You" }
    return "$name, over the last $days days you averaged ${avgSteps.withCommas()} steps a day, " +
        "met your step goal on $stepDays of $days days and your water goal on $waterDays of $days days. " +
        when {
            stepDays * 2 >= days -> "Great consistency. Keep it up."
            else -> "Try to walk a little more each day; small increases add up."
        }
}

/** Weight change over the last 7 days from real logs, e.g. "-0.8 kg this week"; null with fewer than two logs. */
private fun weeklyWeightChange(logs: List<WeightLogEntity>): String? {
    if (logs.size < 2) return null
    val sorted = logs.sortedBy { it.date }
    val latest = sorted.last()
    val weekAgo = todayDate().minusDays(7).toKey()
    val baseline = sorted.lastOrNull { it.date <= weekAgo } ?: sorted.first()
    if (baseline === latest) return null
    val change = latest.weightKg - baseline.weightKg
    val sign = if (change > 0.05) "+" else if (change < -0.05) "-" else ""
    return "$sign${kotlin.math.abs(change).toFixed(1)} kg this week"
}

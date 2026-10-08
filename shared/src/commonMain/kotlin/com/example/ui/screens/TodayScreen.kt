package com.example.ui.screens

import androidx.compose.material.icons.filled.LocalFireDepartment
import com.example.ui.components.nextOpenMealSlot
import com.example.ui.components.WeeklyCheckInCard
import com.example.ui.components.WhatsLeftCard
import com.example.ui.components.mealTypeForNow
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.components.ProfileAvatar
import com.example.util.toFixed
import com.example.ui.components.dailyScore
import com.example.data.sensor.StepTrackingMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyLogEntity
import com.example.data.local.MealEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.DailyDesiPlan
import com.example.data.model.GoalType
import com.example.data.model.MealType
import com.example.data.model.NutritionPlan
import com.example.data.model.goal
import com.example.data.model.PlannedMeal
import com.example.ui.components.CalorieGaugeCard
import com.example.ui.components.DailyScoreCard
import com.example.ui.components.ScoreMetric
import com.example.ui.components.StreaksCard
import com.example.data.model.StreakInfo
import com.example.util.withCommas
import com.example.ui.components.DesiMealCard
import com.example.ui.components.StepsCard
import com.example.ui.components.WaterTrackerCard
import com.example.util.currentHour

@Composable
fun TodayScreen(
    profile: UserProfileEntity,
    plan: NutritionPlan,
    streaks: List<StreakInfo>,
    daysSinceWeighIn: Int?,
    onLogWeight: () -> Unit,
    todayLog: DailyLogEntity,
    todayMeals: List<MealEntity>,
    dailyPlan: DailyDesiPlan,
    onAddSteps: (Int) -> Unit,
    onSetSteps: (Int) -> Unit = {},
    activeFast: com.example.data.local.FastingSessionEntity? = null,
    lastFast: com.example.data.local.FastingSessionEntity? = null,
    onStartFast: (Int) -> Unit = {},
    onEndFast: () -> Unit = {},
    stepTrackingMode: StepTrackingMode,
    onEnableStepTracking: () -> Unit,
    onAddWater: (Int) -> Unit,
    onLogPlannedMeal: (PlannedMeal) -> Unit,
    onOpenAddMeal: (MealType) -> Unit,
    onOpenAiScan: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCalories = remember(todayMeals) { todayMeals.sumOf { it.calories } }
    val totalProtein = remember(todayMeals) { todayMeals.sumOf { it.proteinG } }
    val totalCarbs = remember(todayMeals) { todayMeals.sumOf { it.carbsG } }
    val totalFat = remember(todayMeals) { todayMeals.sumOf { it.fatG } }

    // Re-checked every minute so the greeting changes while the app stays open.
    var hour by remember { mutableStateOf(currentHour()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = currentHour()
        }
    }
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    val goal = profile.goal()
    val calorieWindow = when (goal) {
        GoalType.LOSE -> (profile.calorieTarget - 200)..(profile.calorieTarget + 100)
        GoalType.MAINTAIN -> (profile.calorieTarget - 150)..(profile.calorieTarget + 150)
        GoalType.GAIN -> (profile.calorieTarget - 100)..(profile.calorieTarget + 250)
    }
    val scoreMetrics = listOf(
        ScoreMetric(
            label = "Calories",
            valueText = "${totalCalories.withCommas()} / ${profile.calorieTarget.withCommas()} kcal",
            // Closeness to target: eating far above or far below both lose points.
            progress = if (totalCalories == 0) 0f
            else (1f - kotlin.math.abs(totalCalories - profile.calorieTarget).toFloat() / profile.calorieTarget).coerceIn(0f, 1f),
            achieved = totalCalories in calorieWindow
        ),
        ScoreMetric(
            label = "Steps",
            valueText = "${todayLog.steps.withCommas()} / ${profile.stepGoal.withCommas()}",
            progress = todayLog.steps.toFloat() / profile.stepGoal,
            achieved = todayLog.steps >= profile.stepGoal
        ),
        ScoreMetric(
            label = "Water",
            valueText = "${todayLog.waterMl} / ${profile.waterGoalMl} ml",
            progress = todayLog.waterMl.toFloat() / profile.waterGoalMl,
            achieved = todayLog.waterMl >= profile.waterGoalMl
        ),
        ScoreMetric(
            label = "Protein",
            valueText = "${totalProtein.toInt()} / ${plan.proteinG} g",
            progress = (totalProtein / plan.proteinG).toFloat(),
            achieved = totalProtein >= plan.proteinG * 0.9
        )
    )

    val loggedMealTypes = remember(todayMeals) {
        todayMeals.map { it.mealType }.toSet()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & User Greeting Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ProfileAvatar(
                        avatar = profile.avatar,
                        avatarPhoto = profile.avatarPhoto,
                        gender = profile.gender,
                        size = 46.dp,
                        modifier = Modifier.clickable(onClick = onOpenProfile)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        // Shrinks a little on narrow screens so the name is never cut off.
                        androidx.compose.foundation.text.BasicText(
                            text = "$greeting, ${profile.name.substringBefore(' ')}",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                                minFontSize = 12.sp,
                                maxFontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        )
                        Text(
                            text = "${goal.displayName} • ${profile.calorieTarget} kcal/day",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile.isPro) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "VIP PRO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onOpenPro,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("today_go_pro_button")
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Longest streak that is still running; tap to see all streaks.
                    val activeStreak = streaks.maxOfOrNull { it.current } ?: 0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onOpenProgress)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("header_streak"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$activeStreak",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Food Scanner Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAiScan)
                    .testTag("banner_ai_food_scan"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Food Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Photograph a meal to estimate calories and protein",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Button(
                        onClick = onOpenAiScan,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Weekly weigh-in prompt
        if (daysSinceWeighIn == null || daysSinceWeighIn >= 7) {
            item { WeeklyCheckInCard(daysSinceWeighIn = daysSinceWeighIn, onLogWeight = onLogWeight) }
        }

        // Calorie Budget Card
        item {
            CalorieGaugeCard(
                consumedCalories = totalCalories,
                targetCalories = profile.calorieTarget,
                burnedCalories = todayLog.caloriesBurned,
                // Losing needs the most activity; gaining keeps it light so the surplus holds.
                burnTarget = when (goal) {
                    GoalType.LOSE -> 400
                    GoalType.MAINTAIN -> 300
                    GoalType.GAIN -> 200
                },
                proteinG = totalProtein,
                carbsG = totalCarbs,
                fatG = totalFat,
                proteinTargetG = plan.proteinG,
                carbsTargetG = plan.carbsG,
                fatTargetG = plan.fatG
            )
        }

        // What is left for today, with a suggestion for the next open meal
        item {
            val slot = nextOpenMealSlot(loggedMealTypes)
            WhatsLeftCard(
                remainingKcal = profile.calorieTarget - totalCalories,
                remainingProtein = (plan.proteinG - totalProtein).toInt(),
                goal = goal,
                suggestion = dailyPlan.meals.firstOrNull { it.mealType == slot },
                onAddSuggestion = onLogPlannedMeal
            )
        }

        item {
            com.example.ui.components.FastingCard(
                active = activeFast,
                lastCompleted = lastFast,
                onStart = onStartFast,
                onEnd = onEndFast
            )
        }

        // Raat Ka Discipline Score Card
        item {
            DailyScoreCard(metrics = scoreMetrics)
        }

        item { StreaksCard(streaks = streaks, compact = true) }

        // Steps & Water Cards
        item {
            StepsCard(
                currentSteps = todayLog.steps,
                targetSteps = profile.stepGoal,
                onAddSteps = onAddSteps,
                onSetSteps = onSetSteps,
                trackingMode = stepTrackingMode,
                onEnableTracking = onEnableStepTracking
            )
        }

        item {
            WaterTrackerCard(
                waterMl = todayLog.waterMl,
                targetMl = profile.waterGoalMl,
                onAddWater = onAddWater
            )
        }

        // Section: Today's Recommended Desi Diet Plan
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Meal Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Customized for ${profile.dietPreference.replace("_", " ").lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { onOpenAddMeal(mealTypeForNow()) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Custom", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Morning coach mindset tip
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Morning Tip: ${dailyPlan.morningTip}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily Meals
        items(dailyPlan.meals) { meal ->
            val isLogged = loggedMealTypes.contains(meal.mealType.name)
            DesiMealCard(
                meal = meal,
                isAlreadyLogged = isLogged,
                onLogMeal = { onLogPlannedMeal(meal) }
            )
        }
    }
}

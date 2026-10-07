package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import com.example.data.model.GoalType
import com.example.data.model.MealType
import com.example.data.model.PlannedMeal
import com.example.util.withCommas
import kotlin.math.roundToInt

/** The next meal slot that has nothing logged yet, starting from the current time of day. */
fun nextOpenMealSlot(logged: Set<String>): MealType? {
    val order = MealType.entries
    val start = order.indexOf(mealTypeForNow())
    return order.drop(start).firstOrNull { it.name !in logged }
}

/**
 * "What's left today": remaining calories and protein, plus a concrete suggestion for the next meal
 * taken from today's meal plan.
 */
@Composable
fun WhatsLeftCard(
    remainingKcal: Int,
    remainingProtein: Int,
    goal: GoalType,
    suggestion: PlannedMeal?,
    onAddSuggestion: (PlannedMeal) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("whats_left_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("LEFT FOR TODAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            val headline = when {
                remainingKcal > 0 && remainingProtein > 0 ->
                    "${remainingKcal.withCommas()} kcal and $remainingProtein g protein left"
                remainingKcal > 0 -> "${remainingKcal.withCommas()} kcal left · protein target met"
                goal == GoalType.GAIN -> "Calorie target reached. Great job fuelling up today."
                else -> "You have reached today's calorie target."
            }
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (suggestion != null && remainingKcal > 0) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(14.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(suggestion.mealType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Suggested for ${suggestion.mealType.displayName.lowercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(suggestion.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${suggestion.calories} kcal · ${suggestion.proteinG.roundToInt()} g protein",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = { onAddSuggestion(suggestion) },
                    modifier = Modifier.fillMaxWidth().testTag("add_suggestion")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add to ${suggestion.mealType.displayName}")
                }
                }
            }
        }
    }
}

/** Weekly weigh-in prompt; logging a new weight recalculates the calorie plan. */
@Composable
fun WeeklyCheckInCard(daysSinceWeighIn: Int?, onLogWeight: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("weekly_checkin_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Weekly check-in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    if (daysSinceWeighIn == null) "Log your weight to start tracking progress."
                    else "$daysSinceWeighIn days since your last weigh-in. Your plan updates with your new weight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onLogWeight, modifier = Modifier.testTag("checkin_log_weight")) { Text("Log", fontSize = 13.sp) }
        }
    }
}

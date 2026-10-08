package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfileEntity
import com.example.data.model.AiDietPlan
import com.example.data.model.AiPlanMeal
import com.example.data.model.GoalType
import com.example.data.model.NutritionPlan
import com.example.data.model.goal
import com.example.util.toFixed
import com.example.util.withCommas

/** Gemini makes a one-day Indian meal plan for the user's body and goal; each meal can be logged with one tap. */
@Composable
fun AiDietPlanCard(
    profile: UserProfileEntity,
    nutrition: NutritionPlan,
    plan: AiDietPlan?,
    busy: Boolean,
    error: String?,
    aiAvailable: Boolean,
    loggedMealTypes: Set<String>,
    onGenerate: (extra: String) -> Unit,
    onClear: () -> Unit,
    onLogMeal: (AiPlanMeal) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxWidth().testTag("ai_diet_plan_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("AI DIET PLAN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = primary)
            }
            val goalWord = when (profile.goal()) {
                GoalType.LOSE -> "lose weight"
                GoalType.GAIN -> "gain weight"
                GoalType.MAINTAIN -> "stay fit"
            }
            Text(
                if (plan == null) "Your plan to $goalWord" else "Today's plan · ${plan.totalCalories.withCommas()} kcal · ${plan.totalProtein.toFixed(0)} g protein",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (plan == null) "Made for your ${profile.heightCm.toInt()} cm, ${profile.currentWeightKg.toFixed(1)} kg body: " +
                    "${nutrition.dailyCalories.withCommas()} kcal and ${nutrition.proteinG} g protein a day."
                else plan.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (plan != null && (plan.calorieTarget != nutrition.dailyCalories || plan.dietPreference != profile.dietPreference)) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your profile changed since this plan was made. Make a new one to match.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (plan != null && expanded) {
                plan.meals.sortedBy { it.type.ordinal }.forEach { meal ->
                    Spacer(Modifier.height(12.dp))
                    PlanMealBlock(meal, logged = meal.type.name in loggedMealTypes, onLog = { onLogMeal(meal) })
                }
                if (plan.tips.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        plan.tips.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            if (asking || (plan == null && aiAvailable)) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it.take(200) },
                    label = { Text("Any wish? (optional)") },
                    placeholder = { Text("e.g. no rice, more paneer, 3 roti max") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_plan_extra")
                )
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))

            when {
                !aiAvailable -> Text("AI is not set up in this build.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Making your plan…", style = MaterialTheme.typography.bodyMedium)
                }
                plan == null || asking -> Button(
                    onClick = { focus.clearFocus(); onGenerate(extra); asking = false; expanded = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_plan_generate")
                ) { Text(if (plan == null) "Make my plan" else "Make new plan") }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f).testTag("ai_plan_toggle")) {
                        Text(if (expanded) "Hide plan" else "View plan")
                    }
                    OutlinedButton(onClick = { asking = true }, modifier = Modifier.weight(1f)) { Text("New plan") }
                }
            }
            if (plan != null && !busy && expanded) {
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("Remove plan") }
            }
        }
    }
}

@Composable
private fun PlanMealBlock(meal: AiPlanMeal, logged: Boolean, onLog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meal.type.displayName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp)
                Text(meal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${meal.calories} kcal · ${meal.proteinG.toFixed(0)} g protein", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (logged) {
                Icon(Icons.Default.Check, contentDescription = "Logged", tint = com.example.ui.theme.SuccessGreen)
            } else {
                FilledTonalButton(onClick = onLog, modifier = Modifier.testTag("ai_plan_log_${meal.mealType.lowercase()}")) { Text("Add") }
            }
        }
        Spacer(Modifier.height(6.dp))
        meal.foods.forEach { food ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("${food.name} · ${food.portion}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("${food.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (meal.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(meal.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

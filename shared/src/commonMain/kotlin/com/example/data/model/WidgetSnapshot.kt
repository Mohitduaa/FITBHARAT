package com.example.data.model

import com.example.data.local.DailyLogEntity
import com.example.data.local.MealEntity
import com.example.data.local.UserProfileEntity
import com.example.util.currentTimeMillis
import com.example.util.toFixed
import com.example.util.withCommas
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WidgetSnapshotRow(val label: String, val value: String, val progress: Double)

/**
 * Everything the iOS widget draws, already formatted. The app writes it as JSON into the shared App Group
 * and the Swift widget only reads and renders it.
 */
@Serializable
data class WidgetSnapshot(
    val light: Boolean,
    val ringValue: String,
    val ringLabel: String,
    val ringProgress: Double,
    val over: Boolean,
    val rows: List<WidgetSnapshotRow>,
    val updatedAtMillis: Long
) {
    fun toJson(): String = Json.encodeToString(this)
}

private fun fraction(value: Double, goal: Double) = (value / goal.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)

/** Same numbers and choices (theme, ring, bars) as the Android widget. */
fun buildWidgetSnapshot(profile: UserProfileEntity, meals: List<MealEntity>, log: DailyLogEntity): WidgetSnapshot {
    val config = WidgetConfig.fromJson(profile.widgetConfig)
    val plan = profile.nutritionPlan()
    val eaten = meals.sumOf { it.calories }
    val left = profile.calorieTarget - eaten
    val burnTarget = when (profile.goalType) {
        "GAIN" -> 200
        "MAINTAIN" -> 300
        else -> 400
    }
    val rows = config.metrics.map { metric ->
        when (metric) {
            WidgetMetric.STEPS -> WidgetSnapshotRow(
                "Steps", "${log.steps.withCommas()} / ${profile.stepGoal.withCommas()}",
                fraction(log.steps.toDouble(), profile.stepGoal.toDouble())
            )
            WidgetMetric.WATER -> WidgetSnapshotRow(
                "Water", "${(log.waterMl / 1000.0).toFixed(1)} / ${(profile.waterGoalMl / 1000.0).toFixed(1)} L",
                fraction(log.waterMl.toDouble(), profile.waterGoalMl.toDouble())
            )
            WidgetMetric.PROTEIN -> meals.sumOf { it.proteinG }.let {
                WidgetSnapshotRow("Protein", "${it.toInt()} / ${plan.proteinG} g", fraction(it, plan.proteinG.toDouble()))
            }
            WidgetMetric.BURNED -> WidgetSnapshotRow(
                "Burned", "${log.caloriesBurned} / $burnTarget kcal",
                fraction(log.caloriesBurned.toDouble(), burnTarget.toDouble())
            )
            WidgetMetric.CARBS -> meals.sumOf { it.carbsG }.let {
                WidgetSnapshotRow("Carbs", "${it.toInt()} / ${plan.carbsG} g", fraction(it, plan.carbsG.toDouble()))
            }
            WidgetMetric.FAT -> meals.sumOf { it.fatG }.let {
                WidgetSnapshotRow("Fat", "${it.toInt()} / ${plan.fatG} g", fraction(it, plan.fatG.toDouble()))
            }
        }
    }
    return WidgetSnapshot(
        light = config.isLight,
        ringValue = (if (config.showsEaten) eaten else abs(left)).withCommas(),
        ringLabel = when {
            config.showsEaten -> "kcal eaten"
            left >= 0 -> "kcal left"
            else -> "kcal over"
        },
        ringProgress = eaten.toDouble() / profile.calorieTarget.coerceAtLeast(1),
        over = eaten > profile.calorieTarget * 1.05,
        rows = rows,
        updatedAtMillis = currentTimeMillis()
    )
}

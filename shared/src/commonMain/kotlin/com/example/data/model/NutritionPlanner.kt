package com.example.data.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class GoalType(val displayName: String) {
    LOSE("Lose weight"),
    MAINTAIN("Maintain"),
    GAIN("Gain weight");

    companion object {
        fun from(value: String): GoalType = entries.firstOrNull { it.name == value } ?: LOSE

        /** Goal implied by the weights; within 0.5 kg counts as maintain. */
        fun infer(currentKg: Double, targetKg: Double): GoalType = when {
            targetKg < currentKg - 0.5 -> LOSE
            targetKg > currentKg + 0.5 -> GAIN
            else -> MAINTAIN
        }
    }
}

enum class ActivityLevel(val displayName: String, val description: String, val factor: Double) {
    SEDENTARY("Sedentary", "Desk job, little exercise", 1.2),
    LIGHT("Light", "Walks / exercise 1-3 days a week", 1.375),
    MODERATE("Moderate", "Exercise 3-5 days a week", 1.55),
    ACTIVE("Very active", "Hard exercise 6-7 days a week", 1.725);

    companion object {
        fun from(value: String): ActivityLevel = entries.firstOrNull { it.name == value } ?: LIGHT
    }
}

data class PlannerInput(
    val gender: String,
    val age: Int,
    val heightCm: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val goal: GoalType,
    val activity: ActivityLevel,
    val weeklyRateKg: Double
)

data class NutritionPlan(
    val bmr: Int,
    val maintenanceCalories: Int,
    val dailyCalories: Int,
    /** Negative for a deficit, positive for a surplus. */
    val dailyAdjustment: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val waterMl: Int,
    val weeksToGoal: Int?,
    /** Set when the requested pace was capped by the safety floor. */
    val wasCappedBySafetyFloor: Boolean
)

/**
 * Calorie and macro planner.
 *
 * - BMR: Mifflin-St Jeor.
 * - Maintenance (TDEE): BMR x activity factor.
 * - Goal adjustment: ~7700 kcal per kg of body weight, spread across the week.
 * - Weight-loss calories never drop below max(BMR, 1200 women / 1500 men).
 * - Protein scales with body weight and goal; fat is 25% of calories; carbs fill the rest.
 */
object NutritionPlanner {
    private const val KCAL_PER_KG = 7700.0

    fun plan(input: PlannerInput): NutritionPlan {
        val isFemale = input.gender.equals("Female", ignoreCase = true)
        val bmr = 10 * input.currentWeightKg + 6.25 * input.heightCm - 5 * input.age + if (isFemale) -161 else 5
        val maintenance = bmr * input.activity.factor

        val requestedAdjustment = when (input.goal) {
            GoalType.LOSE -> -input.weeklyRateKg * KCAL_PER_KG / 7
            GoalType.GAIN -> input.weeklyRateKg * KCAL_PER_KG / 7
            GoalType.MAINTAIN -> 0.0
        }
        val floor = maxOf(bmr, if (isFemale) 1200.0 else 1500.0)
        val rawCalories = maintenance + requestedAdjustment
        val capped = input.goal == GoalType.LOSE && rawCalories < floor
        val dailyCalories = roundTo(if (capped) floor else rawCalories, 10)
        val adjustment = dailyCalories - roundTo(maintenance, 10)

        val proteinPerKg = when (input.goal) {
            GoalType.LOSE -> 1.6
            GoalType.MAINTAIN -> 1.4
            GoalType.GAIN -> 1.8
        }
        val proteinG = (input.currentWeightKg * proteinPerKg).roundToInt()
        val fatG = (dailyCalories * 0.25 / 9).roundToInt()
        val carbsG = ((dailyCalories - proteinG * 4 - fatG * 9) / 4.0).roundToInt().coerceAtLeast(0)
        val waterMl = roundTo(input.currentWeightKg * 35, 250).coerceIn(2000, 4000)

        val kgToGo = abs(input.currentWeightKg - input.targetWeightKg)
        val effectiveWeeklyRate = abs(adjustment) * 7 / KCAL_PER_KG
        val weeksToGoal = if (input.goal == GoalType.MAINTAIN || effectiveWeeklyRate <= 0.0) {
            null
        } else {
            ceil(kgToGo / effectiveWeeklyRate).toInt()
        }

        return NutritionPlan(
            bmr = bmr.roundToInt(),
            maintenanceCalories = roundTo(maintenance, 10),
            dailyCalories = dailyCalories,
            dailyAdjustment = adjustment,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            waterMl = waterMl,
            weeksToGoal = weeksToGoal,
            wasCappedBySafetyFloor = capped
        )
    }

    private fun roundTo(value: Double, step: Int): Int = ((value / step).roundToInt() * step)
}

fun com.example.data.local.UserProfileEntity.goal(): GoalType = GoalType.from(goalType)

fun com.example.data.local.UserProfileEntity.nutritionPlan(): NutritionPlan = NutritionPlanner.plan(
    PlannerInput(
        gender = gender,
        age = age,
        heightCm = heightCm,
        currentWeightKg = currentWeightKg,
        targetWeightKg = targetWeightKg,
        goal = goal(),
        activity = ActivityLevel.from(activityLevel),
        weeklyRateKg = weeklyRateKg
    )
)

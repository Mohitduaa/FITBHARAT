package com.example.data.model

import com.example.data.local.DailyLogEntity
import com.example.data.local.DailyMealTotals
import com.example.data.local.UserProfileEntity
import com.example.util.minusDays
import kotlinx.datetime.LocalDate

enum class StreakType(val title: String, val description: String) {
    STEPS("Step goal", "Reach your daily step goal"),
    WATER("Hydration", "Reach your daily water goal"),
    CALORIES("Calorie target", "Stay within your calorie range"),
    PROTEIN("Protein", "Reach your daily protein target"),
    LOGGING("Meal logging", "Log at least one meal"),
    PERFECT_DAY("Perfect day", "Meet all four daily goals")
}

data class StreakInfo(
    val type: StreakType,
    /** Consecutive days up to today (or yesterday, while today is still in progress). */
    val current: Int,
    val best: Int,
    val achievedToday: Boolean
)

object StreakCalculator {
    /**
     * [achieved] holds the days on which the goal was met. A streak stays alive through today until
     * the day ends, so a day that is still in progress never breaks it.
     */
    fun compute(type: StreakType, achieved: Set<LocalDate>, today: LocalDate): StreakInfo {
        val achievedToday = today in achieved
        var cursor = if (achievedToday) today else today.minusDays(1)
        var current = 0
        while (cursor in achieved) {
            current++
            cursor = cursor.minusDays(1)
        }

        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in achieved.sorted()) {
            run = if (previous != null && previous.minusDaysFrom(day) == 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return StreakInfo(type, current, maxOf(best, current), achievedToday)
    }

    /** Days between two dates, [this] being the earlier one. */
    private fun LocalDate.minusDaysFrom(later: LocalDate): Int = later.toEpochDays().toInt() - this.toEpochDays().toInt()

    fun computeAll(
        profile: UserProfileEntity,
        plan: NutritionPlan,
        logs: List<DailyLogEntity>,
        mealTotals: List<DailyMealTotals>,
        today: LocalDate
    ): List<StreakInfo> {
        val goal = profile.goal()
        val window = when (goal) {
            GoalType.LOSE -> (profile.calorieTarget - 200)..(profile.calorieTarget + 100)
            GoalType.MAINTAIN -> (profile.calorieTarget - 150)..(profile.calorieTarget + 150)
            GoalType.GAIN -> (profile.calorieTarget - 100)..(profile.calorieTarget + 250)
        }

        fun dates(source: Iterable<String>): Set<LocalDate> = source.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

        val steps = dates(logs.filter { it.steps >= profile.stepGoal }.map { it.date })
        val water = dates(logs.filter { it.waterMl >= profile.waterGoalMl }.map { it.date })
        val calories = dates(mealTotals.filter { it.calories in window }.map { it.date })
        val protein = dates(mealTotals.filter { it.protein >= plan.proteinG * 0.9 }.map { it.date })
        val logging = dates(mealTotals.map { it.date })
        val perfect = steps intersect water intersect calories intersect protein

        return listOf(
            compute(StreakType.STEPS, steps, today),
            compute(StreakType.WATER, water, today),
            compute(StreakType.CALORIES, calories, today),
            compute(StreakType.PROTEIN, protein, today),
            compute(StreakType.LOGGING, logging, today),
            compute(StreakType.PERFECT_DAY, perfect, today)
        )
    }
}

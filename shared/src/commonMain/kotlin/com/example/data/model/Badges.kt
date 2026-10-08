package com.example.data.model

import com.example.data.local.DailyMealTotals
import com.example.data.local.UserProfileEntity
import com.example.util.toFixed
import com.example.util.withCommas
import kotlin.math.abs

enum class BadgeIcon { MEAL, WORKOUT, WEIGHT, STREAK, STEPS, WATER, STAR, PROGRAM, TROPHY, FAST }

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: BadgeIcon,
    val earned: Boolean,
    /** e.g. "3 / 7 days" while not earned yet. */
    val progress: String
)

/** Achievements worked out from the user's own data; nothing extra is stored. */
object BadgeCalculator {
    fun compute(
        profile: UserProfileEntity,
        streaks: List<StreakInfo>,
        completedWorkoutDays: Set<String>,
        mealTotals: List<DailyMealTotals>,
        dailyLogs: List<com.example.data.local.DailyLogEntity> = emptyList(),
        fasts: List<com.example.data.local.FastingSessionEntity> = emptyList()
    ): List<Badge> {
        val fastHours = fasts.mapNotNull { f -> f.endMillis?.let { (it - f.startMillis) / 3_600_000.0 } }
        val completedFasts = fasts.count { f -> f.endMillis?.let { it - f.startMillis >= f.targetHours * 3_600_000L } == true }
        val longestFast = fastHours.maxOrNull() ?: 0.0
        val bestSteps = dailyLogs.maxOfOrNull { it.steps } ?: 0
        val totalSteps = dailyLogs.sumOf { it.steps.toLong() }
        fun best(type: StreakType) = streaks.firstOrNull { it.type == type }?.best ?: 0
        val goal = profile.goal()
        val moved = when (goal) {
            GoalType.LOSE -> profile.startWeightKg - profile.currentWeightKg
            GoalType.GAIN -> profile.currentWeightKg - profile.startWeightKg
            GoalType.MAINTAIN -> 0.0
        }
        val programDays = completedWorkoutDays.count { WorkoutProgram.isProgramDay(it) }
        val week1 = (1..7).all { WorkoutProgram.dayId(1, it) in completedWorkoutDays }
        val goalReached = when (goal) {
            GoalType.LOSE -> profile.currentWeightKg <= profile.targetWeightKg
            GoalType.GAIN -> profile.currentWeightKg >= profile.targetWeightKg
            GoalType.MAINTAIN -> abs(profile.currentWeightKg - profile.targetWeightKg) <= 1.0
        }
        val verb = if (goal == GoalType.GAIN) "Gain" else "Lose"

        fun days(current: Int, needed: Int) = "${current.coerceAtMost(needed)} / $needed days"

        return listOf(
            Badge("first_meal", "First Meal", "Log your first meal.", BadgeIcon.MEAL, mealTotals.isNotEmpty(), if (mealTotals.isEmpty()) "Not yet" else "Done"),
            Badge("first_workout", "First Workout", "Finish any day of the 4-week program.", BadgeIcon.WORKOUT, programDays > 0, "$programDays / 1"),
            Badge(
                "kg_1", "First Kilo", "$verb your first 1 kg.", BadgeIcon.WEIGHT,
                goal != GoalType.MAINTAIN && moved >= 1.0, "${moved.coerceAtLeast(0.0).toFixed(1)} / 1 kg"
            ),
            Badge(
                "kg_5", "Five Kilos", "$verb 5 kg in total.", BadgeIcon.WEIGHT,
                goal != GoalType.MAINTAIN && moved >= 5.0, "${moved.coerceAtLeast(0.0).toFixed(1)} / 5 kg"
            ),
            Badge("first_fast", "First Fast", "Complete your first fast to its goal.", BadgeIcon.FAST, completedFasts > 0, "$completedFasts / 1"),
            Badge("fast_16", "16-Hour Fast", "Finish a fast of 16 hours or more.", BadgeIcon.FAST, longestFast >= 16.0, "${longestFast.toFixed(1)} / 16 h"),
            Badge("fasts_10", "Fasting Pro", "Complete 10 fasts.", BadgeIcon.FAST, completedFasts >= 10, "$completedFasts / 10"),
            Badge("steps_10k", "10,000 Steps", "Walk 10,000 steps in one day.", BadgeIcon.STEPS, bestSteps >= 10_000, "Best ${bestSteps.withCommas()} / 10,000"),
            Badge("steps_100k", "100K Club", "Walk 100,000 steps in total.", BadgeIcon.STEPS, totalSteps >= 100_000, "${totalSteps / 1000}k / 100k"),
            Badge("log_7", "Week of Logging", "Log meals 7 days in a row.", BadgeIcon.STREAK, best(StreakType.LOGGING) >= 7, days(best(StreakType.LOGGING), 7)),
            Badge("steps_7", "Step Streak", "Hit your step goal 7 days in a row.", BadgeIcon.STEPS, best(StreakType.STEPS) >= 7, days(best(StreakType.STEPS), 7)),
            Badge("log_30", "30-Day Streak", "Log meals 30 days in a row.", BadgeIcon.STREAK, best(StreakType.LOGGING) >= 30, days(best(StreakType.LOGGING), 30)),
            Badge("water_7", "Hydration Hero", "Hit your water goal 7 days in a row.", BadgeIcon.WATER, best(StreakType.WATER) >= 7, days(best(StreakType.WATER), 7)),
            Badge("perfect_day", "Perfect Day", "Meet calories, steps, water and protein on the same day.", BadgeIcon.STAR, best(StreakType.PERFECT_DAY) >= 1, days(best(StreakType.PERFECT_DAY), 1)),
            Badge("week_1", "Week One Done", "Complete all 7 days of week 1.", BadgeIcon.PROGRAM, week1, "${(1..7).count { WorkoutProgram.dayId(1, it) in completedWorkoutDays }} / 7 days"),
            Badge("program", "Program Finisher", "Complete the full 4-week program.", BadgeIcon.PROGRAM, programDays >= WorkoutProgram.TOTAL_DAYS, "$programDays / ${WorkoutProgram.TOTAL_DAYS} days"),
            Badge("goal", "Goal Reached", "Reach your target weight of ${profile.targetWeightKg.toFixed(1)} kg.", BadgeIcon.TROPHY, goalReached, "${profile.currentWeightKg.toFixed(1)} kg now")
        )
    }
}

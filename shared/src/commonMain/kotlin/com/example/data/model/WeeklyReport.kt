package com.example.data.model

import com.example.data.local.DailyLogEntity
import com.example.data.local.DailyMealTotals
import com.example.data.local.FastingSessionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightLogEntity
import com.example.util.millisToDateKey
import com.example.util.minusDays
import com.example.util.toFixed
import com.example.util.toKey
import com.example.util.withCommas
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

/** The last 7 days (today included) compared with the 7 before, built only from what the user logged. */
data class WeeklyReport(
    val weightChangeKg: Double?,
    val loggedDays: Int,
    val avgCalories: Int?,
    val calorieDaysOnTarget: Int,
    val avgSteps: Int,
    val prevAvgSteps: Int?,
    val stepGoalDays: Int,
    val waterGoalDays: Int,
    val fastsCompleted: Int,
    val verdict: String
) {
    val hasData: Boolean get() = loggedDays > 0 || avgSteps > 0 || weightChangeKg != null

    /** One line for the Sunday notification. */
    fun notificationText(): String = buildList {
        weightChangeKg?.let { add("${if (it > 0) "+" else if (it < 0) "-" else ""}${abs(it).toFixed(1)} kg") }
        avgCalories?.let { add("avg ${it.withCommas()} kcal") }
        add("$calorieDaysOnTarget/7 days on target")
        add("${avgSteps.withCommas()} steps/day")
        if (fastsCompleted > 0) add("$fastsCompleted fasts")
    }.joinToString(" · ")

    companion object {
        fun compute(
            profile: UserProfileEntity,
            weightLogs: List<WeightLogEntity>,
            dailyLogs: List<DailyLogEntity>,
            mealTotals: List<DailyMealTotals>,
            fasts: List<FastingSessionEntity>,
            today: LocalDate
        ): WeeklyReport {
            val weekStart = today.minusDays(6).toKey()
            val prevStart = today.minusDays(13).toKey()
            val todayKey = today.toKey()
            fun inWeek(date: String) = date >= weekStart && date <= todayKey
            fun inPrevWeek(date: String) = date >= prevStart && date < weekStart

            // Weight: latest this week against the latest before it (or the first one this week).
            val sorted = weightLogs.sortedWith(compareBy({ it.date }, { it.loggedAt }))
            val latest = sorted.lastOrNull { inWeek(it.date) }
            val baseline = sorted.lastOrNull { it.date < weekStart } ?: sorted.firstOrNull { inWeek(it.date) }
            val weightChange = if (latest != null && baseline != null && latest !== baseline) {
                ((latest.weightKg - baseline.weightKg) * 10).roundToInt() / 10.0
            } else null

            val meals = mealTotals.filter { inWeek(it.date) && it.calories > 0 }
            val target = profile.calorieTarget
            val onTarget = meals.count { it.calories >= target * 0.8 && it.calories <= target * 1.1 }
            val avgCalories = if (meals.isEmpty()) null else meals.sumOf { it.calories } / meals.size

            val week = dailyLogs.filter { inWeek(it.date) }
            val prev = dailyLogs.filter { inPrevWeek(it.date) }
            val avgSteps = week.sumOf { it.steps } / 7
            val prevAvgSteps = if (prev.isEmpty()) null else prev.sumOf { it.steps } / 7
            val stepDays = week.count { it.steps >= profile.stepGoal }
            val waterDays = week.count { it.waterMl >= profile.waterGoalMl }

            val fastsDone = fasts.count { fast ->
                val end = fast.endMillis ?: return@count false
                end - fast.startMillis >= fast.targetHours * 3_600_000L && inWeek(millisToDateKey(fast.startMillis))
            }

            return WeeklyReport(
                weightChangeKg = weightChange,
                loggedDays = meals.size,
                avgCalories = avgCalories,
                calorieDaysOnTarget = onTarget,
                avgSteps = avgSteps,
                prevAvgSteps = prevAvgSteps,
                stepGoalDays = stepDays,
                waterGoalDays = waterDays,
                fastsCompleted = fastsDone,
                verdict = verdict(profile, weightChange, meals.size, onTarget, stepDays, avgSteps, prevAvgSteps)
            )
        }

        private fun verdict(
            profile: UserProfileEntity,
            weightChange: Double?,
            loggedDays: Int,
            onTarget: Int,
            stepDays: Int,
            avgSteps: Int,
            prevAvgSteps: Int?
        ): String {
            if (loggedDays == 0 && avgSteps == 0 && weightChange == null) {
                return "Not enough data yet. Log meals, steps and your weight this week to get a full report."
            }
            val name = profile.name.substringBefore(' ').ifBlank { "You" }
            val parts = mutableListOf<String>()
            when {
                weightChange == null -> parts += "Log your weight once a week so the report can track your progress."
                profile.goal() == GoalType.LOSE && weightChange < 0 -> parts += "$name, you are down ${abs(weightChange).toFixed(1)} kg this week. Great work."
                profile.goal() == GoalType.GAIN && weightChange > 0 -> parts += "$name, you gained ${weightChange.toFixed(1)} kg this week, right on track."
                profile.goal() == GoalType.MAINTAIN && abs(weightChange) <= 0.3 -> parts += "$name, your weight held steady this week."
                else -> parts += "$name, your weight moved ${if (weightChange > 0) "up" else "down"} ${abs(weightChange).toFixed(1)} kg. One week can be water weight; keep going."
            }
            when {
                loggedDays == 0 -> parts += "No meals were logged, so calories can't be checked."
                onTarget >= 5 -> parts += "Calories were on target on $onTarget days."
                else -> parts += "Calories were on target on only $onTarget of $loggedDays logged days; aim for steadier days."
            }
            if (prevAvgSteps != null && prevAvgSteps > 0) {
                val diff = avgSteps - prevAvgSteps
                parts += if (diff >= 0) "Steps are up ${diff.withCommas()} a day from last week." else "Steps dropped ${(-diff).withCommas()} a day from last week."
            } else if (stepDays > 0) {
                parts += "You hit your step goal on $stepDays days."
            }
            return parts.joinToString(" ")
        }
    }
}

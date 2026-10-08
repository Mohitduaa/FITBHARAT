package com.example.data.model

import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightLogEntity
import com.example.util.minusDays
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.math.ceil

/** One weight reading on the timeline: [day] counts from today (negative = past). */
data class TimelinePoint(val day: Int, val weightKg: Double)

/**
 * "At this pace you reach 70 kg by 12 Mar": the user's real trend from recent weigh-ins,
 * falling back to the plan's weekly rate when there is not enough history yet.
 */
data class GoalTimeline(
    val goal: GoalType,
    val currentKg: Double,
    val targetKg: Double,
    /** Signed kg per week: negative when weight is going down. */
    val ratePerWeek: Double,
    /** True when [ratePerWeek] comes from the user's own weigh-ins, false when it is the plan pace. */
    val fromTrend: Boolean,
    /** null when the goal is reached or the pace is moving away from it. */
    val etaDate: LocalDate?,
    val etaDays: Int?,
    val reached: Boolean,
    /** The plan's own pace (signed kg/week), used for the date when the trend is going the wrong way. */
    val plannedRate: Double = ratePerWeek,
    val wrongWay: Boolean,
    val history: List<TimelinePoint>
) {
    val remainingKg: Double get() = abs(targetKg - currentKg)

    companion object {
        private const val TREND_WINDOW_DAYS = 42

        fun compute(profile: UserProfileEntity, logs: List<WeightLogEntity>, today: LocalDate): GoalTimeline {
            val goal = profile.goal()
            val current = profile.currentWeightKg
            val target = profile.targetWeightKg

            // Latest reading per day, last 90 days, oldest first.
            val history = logs
                .mapNotNull { log -> runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log } }
                .filter { (date, _) -> date >= today.minusDays(90) && date <= today }
                .groupBy({ it.first }, { it.second })
                .map { (date, sameDay) -> TimelinePoint(today.daysUntil(date), sameDay.maxBy { it.loggedAt }.weightKg) }
                .sortedBy { it.day }

            // Least-squares slope over the recent window, if it spans at least a week.
            val recent = history.filter { it.day >= -TREND_WINDOW_DAYS }
            val trend = if (recent.size >= 2 && recent.last().day - recent.first().day >= 7) {
                val meanX = recent.map { it.day.toDouble() }.average()
                val meanY = recent.map { it.weightKg }.average()
                val num = recent.sumOf { (it.day - meanX) * (it.weightKg - meanY) }
                val den = recent.sumOf { (it.day - meanX) * (it.day - meanX) }
                if (den > 0) num / den * 7 else null
            } else null

            val planned = when (goal) {
                GoalType.LOSE -> -profile.weeklyRateKg
                GoalType.GAIN -> profile.weeklyRateKg
                GoalType.MAINTAIN -> 0.0
            }
            val rate = trend ?: planned

            val reached = when (goal) {
                GoalType.LOSE -> current <= target
                GoalType.GAIN -> current >= target
                GoalType.MAINTAIN -> abs(current - target) <= 1.0
            }
            val needed = target - current
            val wrongWay = !reached && goal != GoalType.MAINTAIN && (rate == 0.0 || needed * rate < 0 || abs(rate) < 0.02)
            // Off track: show when the plan pace would get there, so there is still a date to aim for.
            val projectionRate = if (wrongWay) planned else rate
            val etaDays = if (reached || goal == GoalType.MAINTAIN || projectionRate == 0.0) null
            else ceil(abs(needed) / abs(projectionRate) * 7).toInt().coerceAtMost(365 * 5)

            return GoalTimeline(
                goal = goal,
                currentKg = current,
                targetKg = target,
                ratePerWeek = rate,
                fromTrend = trend != null,
                etaDate = etaDays?.let { today.plus(it, DateTimeUnit.DAY) },
                etaDays = etaDays,
                reached = reached,
                plannedRate = planned,
                wrongWay = wrongWay,
                history = history
            )
        }
    }
}

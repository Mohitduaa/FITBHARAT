package com.example.data.model

import com.example.util.withCommas

/** What the user has done so far today, for deciding whether a reminder is still useful. */
data class TodayStatus(
    val lunchLogged: Boolean,
    val dinnerLogged: Boolean,
    val steps: Int,
    val waterMl: Int,
    val waterGoalMl: Int
)

enum class SmartReminderKind { LUNCH, DINNER, WALK, WATER }

/** Smart reminders: skip the ones already taken care of, and say something specific about the rest. */
object SmartReminders {
    /** The notification text, or null when the reminder should not be shown. */
    fun message(kind: SmartReminderKind, status: TodayStatus, times: ReminderTimes): String? = when (kind) {
        SmartReminderKind.LUNCH -> if (status.lunchLogged) null
        else "Lunch isn't logged yet. Add it now so today's calories stay accurate."
        SmartReminderKind.DINNER -> if (status.dinnerLogged) null
        else "Dinner isn't logged yet. Finish today's food log before bed."
        SmartReminderKind.WALK -> if (status.steps >= times.walkMinSteps) null
        else "Only ${status.steps.withCommas()} steps so far today. A 20-minute walk adds about 2,000."
        SmartReminderKind.WATER -> if (status.waterMl >= status.waterGoalMl) null
        else "${status.waterMl} of ${status.waterGoalMl} ml so far. Have a glass of water now."
    }

    /** Which kinds are already done today, so their pending reminder can be dropped (used on iOS). */
    fun doneToday(status: TodayStatus, times: ReminderTimes): Set<SmartReminderKind> =
        SmartReminderKind.entries.filter { message(it, status, times) == null }.toSet()
}

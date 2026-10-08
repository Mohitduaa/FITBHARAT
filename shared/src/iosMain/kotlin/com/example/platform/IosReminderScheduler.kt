package com.example.platform

import com.example.data.model.ReminderTimes
import kotlinx.datetime.plus
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/** Daily local notifications. iOS keeps repeating calendar triggers across restarts by itself. */
class IosReminderScheduler : ReminderScheduler {
    /** Today's smart reminder ids that are already done, so re-scheduling doesn't bring them back. */
    private var doneIds: Set<String> = emptySet()

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun requestPermission() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    override fun apply(water: Boolean, meals: Boolean, walk: Boolean, times: ReminderTimes) {
        val notifications = center
        notifications.getPendingNotificationRequestsWithCompletionHandler { pending ->
            val ours = pending.orEmpty().mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(PREFIX) }
            notifications.removePendingNotificationRequestsWithIdentifiers(ours)

            if (water) {
                val step = times.waterEveryHours.coerceIn(1, 6) * 60
                var at = times.waterStart
                while (at <= times.waterEnd) {
                    add("${PREFIX}water_$at", "Time for water", "Have a glass of water and log it in FitBharat.", at)
                    at += step
                }
            }
            if (meals) {
                if (times.smart) {
                    addDaily("lunch", "Log your lunch", "Lunch isn't logged yet. Add it now so today's calories stay accurate.", times.lunch)
                    addDaily("dinner", "Log your dinner", "Dinner isn't logged yet. Finish today's food log before bed.", times.dinner)
                } else {
                    add("${PREFIX}lunch", "Log your lunch", "Add what you ate to keep your calorie count accurate.", times.lunch)
                    add("${PREFIX}dinner", "Log your dinner", "Finish today's food log before bed.", times.dinner)
                }
            }
            if (times.weeklyReport) {
                val sunday = NSDateComponents().apply {
                    weekday = 1
                    hour = 19
                    minute = 0
                }
                val content = UNMutableNotificationContent().apply {
                    setTitle("Your weekly report")
                    setBody("See how your week went: weight, calories, steps and fasting.")
                    setSound(UNNotificationSound.defaultSound)
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(sunday, repeats = true)
                notifications.addNotificationRequest(
                    UNNotificationRequest.requestWithIdentifier("${PREFIX}weekly", content = content, trigger = trigger),
                    withCompletionHandler = null
                )
            }
            if (walk) {
                if (times.smart) {
                    addDaily("walk", "Evening walk", "Under ${times.walkMinSteps} steps today. A 20-minute walk adds about 2,000.", times.walk)
                } else {
                    add("${PREFIX}walk", "Evening walk", "A 15-minute walk now helps you reach your step goal.", times.walk)
                }
            }
        }
    }

    override fun scheduleFastEnd(atMillis: Long?) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(FAST_ID))
        if (atMillis == null) return
        val seconds = (atMillis - com.example.util.currentTimeMillis()) / 1000.0
        if (seconds <= 1.0) return
        val content = UNMutableNotificationContent().apply {
            setTitle("Fast complete")
            setBody("Well done! Break your fast with a balanced, protein-rich meal.")
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        center.addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(FAST_ID, content = content, trigger = trigger),
            withCompletionHandler = null
        )
    }

    /**
     * iOS can't look at today's log when a notification fires, so smart reminders are scheduled one
     * day at a time for the next week, and today's one is removed as soon as the task is done.
     */
    private fun addDaily(key: String, title: String, body: String, minutesAfterMidnight: Int) {
        val today = com.example.util.todayDate()
        for (offset in 0 until SMART_DAYS) {
            val date = today.plus(offset, kotlinx.datetime.DateTimeUnit.DAY)
            if (offset == 0 && "${PREFIX}${key}_$date" in doneIds) continue
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
            }
            val time = NSDateComponents().apply {
                year = date.year.toLong()
                month = date.month.ordinal.toLong() + 1
                day = date.day.toLong()
                hour = (minutesAfterMidnight / 60).toLong()
                minute = (minutesAfterMidnight % 60).toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(time, repeats = false)
            center.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier("${PREFIX}${key}_$date", content = content, trigger = trigger),
                withCompletionHandler = null
            )
        }
    }

    override fun onTodayStatus(status: com.example.data.model.TodayStatus, times: ReminderTimes) {
        if (!times.smart) return
        val today = com.example.util.todayDate()
        val done = com.example.data.model.SmartReminders.doneToday(status, times)
        val ids = buildList {
            if (com.example.data.model.SmartReminderKind.LUNCH in done) add("${PREFIX}lunch_$today")
            if (com.example.data.model.SmartReminderKind.DINNER in done) add("${PREFIX}dinner_$today")
            if (com.example.data.model.SmartReminderKind.WALK in done) add("${PREFIX}walk_$today")
        }
        doneIds = ids.toSet()
        if (ids.isNotEmpty()) center.removePendingNotificationRequestsWithIdentifiers(ids)
    }

    private fun add(id: String, title: String, body: String, minutesAfterMidnight: Int) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val time = NSDateComponents().apply {
            hour = (minutesAfterMidnight / 60).toLong()
            minute = (minutesAfterMidnight % 60).toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(time, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier(id, content = content, trigger = trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    private companion object {
        const val PREFIX = "fb_"
        const val SMART_DAYS = 7
        // Not under PREFIX, so re-applying the daily reminders never removes a running fast's alert.
        const val FAST_ID = "fasting_end"
    }
}

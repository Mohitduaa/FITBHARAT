package com.example.platform

import com.example.data.model.ReminderTimes
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Daily local notifications. iOS keeps repeating calendar triggers across restarts by itself. */
class IosReminderScheduler : ReminderScheduler {
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
                add("${PREFIX}lunch", "Log your lunch", "Add what you ate to keep your calorie count accurate.", times.lunch)
                add("${PREFIX}dinner", "Log your dinner", "Finish today's food log before bed.", times.dinner)
            }
            if (walk) {
                add("${PREFIX}walk", "Evening walk", "A 15-minute walk now helps you reach your step goal.", times.walk)
            }
        }
    }

    private fun add(id: String, title: String, body: String, minutesAfterMidnight: Int) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val time = NSDateComponents().apply {
            setHour((minutesAfterMidnight / 60).toLong())
            setMinute((minutesAfterMidnight % 60).toLong())
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(time, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier(id, content = content, trigger = trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    private companion object {
        const val PREFIX = "fb_"
    }
}

package com.example.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.model.ReminderTimes
import java.util.Calendar

/** The reminder kinds, each with its own notification and alarm. */
internal enum class ReminderKind(val id: Int, val title: String, val text: String) {
    WATER(101, "Time for water", "Have a glass of water and log it in FitBharat."),
    MEAL_LUNCH(102, "Log your lunch", "Add what you ate to keep your calorie count accurate."),
    MEAL_DINNER(103, "Log your dinner", "Finish today's food log before bed."),
    WALK(104, "Evening walk", "A 15-minute walk now helps you reach your step goal.")
}

/**
 * Schedules daily reminders with inexact (battery friendly) alarms. The switches are mirrored in
 * SharedPreferences so [ReminderReceiver] can re-create the alarms after a reboot.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    /** Set by the Activity, which owns the permission-result launcher. */
    var permissionRequester: () -> Unit = {}

    override fun apply(water: Boolean, meals: Boolean, walk: Boolean, times: ReminderTimes) {
        prefs(context).edit()
            .putBoolean(KEY_WATER, water)
            .putBoolean(KEY_MEALS, meals)
            .putBoolean(KEY_WALK, walk)
            .putString(KEY_TIMES, times.toJson())
            .apply()
        schedule(context)
    }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequester()
        }
    }

    internal companion object {
        private const val PREFS = "reminders"
        private const val KEY_WATER = "water"
        private const val KEY_MEALS = "meals"
        private const val KEY_WALK = "walk"
        private const val KEY_TIMES = "times"

        fun times(context: Context) = ReminderTimes.fromJson(prefs(context).getString(KEY_TIMES, "") ?: "")
        const val CHANNEL_ID = "reminders"
        const val EXTRA_KIND = "kind"

        fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** (Re)creates every alarm from the saved switches. */
        fun schedule(context: Context) {
            val p = prefs(context)
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val t = times(context)
            fun set(kind: ReminderKind, enabled: Boolean, atMinutes: Int, interval: Long) {
                val hour = atMinutes / 60
                val minute = atMinutes % 60
                val intent = PendingIntent.getBroadcast(
                    context,
                    kind.id,
                    Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind.name),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarms.cancel(intent)
                if (!enabled) return
                val first = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    while (timeInMillis <= System.currentTimeMillis()) add(Calendar.MILLISECOND, interval.toInt())
                }
                alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, first.timeInMillis, interval, intent)
            }
            set(ReminderKind.WATER, p.getBoolean(KEY_WATER, false), t.waterStart, t.waterEveryHours.coerceIn(1, 6) * AlarmManager.INTERVAL_HOUR)
            set(ReminderKind.MEAL_LUNCH, p.getBoolean(KEY_MEALS, false), t.lunch, AlarmManager.INTERVAL_DAY)
            set(ReminderKind.MEAL_DINNER, p.getBoolean(KEY_MEALS, false), t.dinner, AlarmManager.INTERVAL_DAY)
            set(ReminderKind.WALK, p.getBoolean(KEY_WALK, false), t.walk, AlarmManager.INTERVAL_DAY)
        }
    }
}

/** Shows the reminder notification; also restores alarms after the phone restarts. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AndroidReminderScheduler.schedule(context)
            return
        }
        val kind = intent.getStringExtra(AndroidReminderScheduler.EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() } ?: return

        // Water reminders only inside the user's chosen window.
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val t = AndroidReminderScheduler.times(context)
        if (kind == ReminderKind.WATER && now !in (t.waterStart - 10)..(t.waterEnd + 10)) return

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(AndroidReminderScheduler.CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(context, kind.id, launch, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, AndroidReminderScheduler.CHANNEL_ID)
            .setSmallIcon(
                context.resources.getIdentifier("ic_stat_notify", "drawable", context.packageName)
                    .takeIf { it != 0 } ?: context.applicationInfo.icon
            )
            .setContentTitle(kind.title)
            .setContentText(kind.text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(kind.id, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the call; nothing to show.
        }
    }
}

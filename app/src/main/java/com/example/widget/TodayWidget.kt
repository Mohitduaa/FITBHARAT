package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.FitBharatApplication
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Home screen widget: calories left and steps for today. Tapping it opens the app. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        TodayWidget.keepUpdated(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TodayWidget.update(context, manager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }
}

object TodayWidget {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val watching = AtomicBoolean(false)

    private fun widgetIds(context: Context, manager: AppWidgetManager): IntArray =
        manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))

    /** Redraws every placed widget from the latest data. */
    suspend fun update(
        context: Context,
        manager: AppWidgetManager = AppWidgetManager.getInstance(context),
        ids: IntArray = widgetIds(context, manager)
    ) {
        if (ids.isEmpty()) return
        val app = context.applicationContext as FitBharatApplication
        val repository = app.container.repository
        val profile = repository.userProfile.first()
        val eaten = repository.getMealsForToday().first().sumOf { it.calories }
        val steps = repository.getTodayLog().first().steps

        val left = profile.calorieTarget - eaten
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
            setTextViewText(R.id.widget_left_value, "%,d".format(abs(left)))
            setTextViewText(R.id.widget_left_label, if (left >= 0) "kcal left today" else "kcal over target")
            setProgressBar(
                R.id.widget_cal_progress, 100,
                (eaten * 100 / profile.calorieTarget.coerceAtLeast(1)).coerceIn(0, 100), false
            )
            setTextViewText(R.id.widget_steps_value, "%,d".format(steps))
            setTextViewText(R.id.widget_steps_label, "of %,d steps".format(profile.stepGoal))
            setProgressBar(
                R.id.widget_steps_progress, 100,
                (steps * 100 / profile.stepGoal.coerceAtLeast(1)).coerceIn(0, 100), false
            )
            setOnClickPendingIntent(R.id.widget_root, open)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    fun refreshAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { update(app) }
    }

    /** While the app process is alive, redraw whenever meals, steps or the profile change. */
    @OptIn(FlowPreview::class)
    fun keepUpdated(context: Context) {
        val app = context.applicationContext as FitBharatApplication
        val manager = AppWidgetManager.getInstance(app)
        if (widgetIds(app, manager).isEmpty() || !watching.compareAndSet(false, true)) return
        scope.launch {
            val repository = app.container.repository
            combine(repository.userProfile, repository.getMealsForToday(), repository.getTodayLog()) { _, _, _ -> }
                .debounce(1_000)
                .collect { update(app) }
        }
    }
}

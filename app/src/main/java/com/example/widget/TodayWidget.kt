package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.FitBharatApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.nutritionPlan
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
        val log = repository.getTodayLog().first()
        val steps = log.steps
        val meals = repository.getMealsForToday().first()
        val eaten = meals.sumOf { it.calories }

        val left = profile.calorieTarget - eaten
        val protein = meals.sumOf { it.proteinG }
        val plan = profile.nutritionPlan()
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
            setImageViewBitmap(
                R.id.widget_ring,
                calorieRing(
                    progress = eaten.toFloat() / profile.calorieTarget.coerceAtLeast(1),
                    value = "%,d".format(abs(left)),
                    label = if (left >= 0) "kcal left" else "kcal over"
                )
            )
            setTextViewText(R.id.widget_steps_value, "%,d / %,d".format(steps, profile.stepGoal))
            setProgressBar(R.id.widget_steps_progress, 100, percent(steps.toDouble(), profile.stepGoal.toDouble()), false)
            setTextViewText(R.id.widget_water_value, "%.1f / %.1f L".format(log.waterMl / 1000.0, profile.waterGoalMl / 1000.0))
            setProgressBar(R.id.widget_water_progress, 100, percent(log.waterMl.toDouble(), profile.waterGoalMl.toDouble()), false)
            setTextViewText(R.id.widget_protein_value, "%d / %d g".format(protein.toInt(), plan.proteinG))
            setProgressBar(R.id.widget_protein_progress, 100, percent(protein, plan.proteinG.toDouble()), false)
            setOnClickPendingIntent(R.id.widget_root, open)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun percent(value: Double, goal: Double): Int = (value * 100 / goal.coerceAtLeast(1.0)).toInt().coerceIn(0, 100)

    /** A donut for today's calories with the number left in the middle (RemoteViews cannot draw custom shapes). */
    private fun calorieRing(progress: Float, value: String, label: String): Bitmap {
        val size = 360
        val stroke = 36f
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = stroke / 2 + 6f
        val rect = RectF(inset, inset, size - inset, size - inset)
        val over = progress > 1.05f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = 0xFF45413A.toInt()
        }
        canvas.drawArc(rect, -90f, 360f, false, ring)
        ring.color = if (over) 0xFFE5655B.toInt() else 0xFFEE8A47.toInt()
        if (progress > 0f) canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0.02f, 1f), false, ring)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFFF5F1EA.toInt()
            textSize = if (value.length >= 5) 76f else 92f
        }
        canvas.drawText(value, size / 2f, size / 2f + 14f, text)
        text.typeface = Typeface.DEFAULT
        text.color = 0xFFB1ACA0.toInt()
        text.textSize = 34f
        canvas.drawText(label, size / 2f, size / 2f + 62f, text)
        return bitmap
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

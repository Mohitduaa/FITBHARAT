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
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetMetric
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
        val plan = profile.nutritionPlan()
        val config = WidgetConfig.fromJson(profile.widgetConfig)
        val burnTarget = when (profile.goalType) {
            "GAIN" -> 200
            "MAINTAIN" -> 300
            else -> 400
        }

        /** Label, value text and progress for one bar. */
        fun row(metric: WidgetMetric): Triple<String, String, Int> = when (metric) {
            WidgetMetric.STEPS -> Triple("Steps", "%,d / %,d".format(steps, profile.stepGoal), percent(steps.toDouble(), profile.stepGoal.toDouble()))
            WidgetMetric.WATER -> Triple(
                "Water", "%.1f / %.1f L".format(log.waterMl / 1000.0, profile.waterGoalMl / 1000.0),
                percent(log.waterMl.toDouble(), profile.waterGoalMl.toDouble())
            )
            WidgetMetric.PROTEIN -> meals.sumOf { it.proteinG }.let {
                Triple("Protein", "%d / %d g".format(it.toInt(), plan.proteinG), percent(it, plan.proteinG.toDouble()))
            }
            WidgetMetric.BURNED -> Triple(
                "Burned", "%d / %d kcal".format(log.caloriesBurned, burnTarget),
                percent(log.caloriesBurned.toDouble(), burnTarget.toDouble())
            )
            WidgetMetric.CARBS -> meals.sumOf { it.carbsG }.let {
                Triple("Carbs", "%d / %d g".format(it.toInt(), plan.carbsG), percent(it, plan.carbsG.toDouble()))
            }
            WidgetMetric.FAT -> meals.sumOf { it.fatG }.let {
                Triple("Fat", "%d / %d g".format(it.toInt(), plan.fatG), percent(it, plan.fatG.toDouble()))
            }
        }

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val layout = if (config.isLight) R.layout.widget_today_light else R.layout.widget_today
        val views = RemoteViews(context.packageName, layout).apply {
            setImageViewBitmap(
                R.id.widget_ring,
                calorieRing(
                    progress = eaten.toFloat() / profile.calorieTarget.coerceAtLeast(1),
                    value = "%,d".format(if (config.showsEaten) eaten else abs(left)),
                    label = when {
                        config.showsEaten -> "kcal eaten"
                        left >= 0 -> "kcal left"
                        else -> "kcal over"
                    },
                    light = config.isLight
                )
            )
            val ids = listOf(
                Triple(R.id.widget_row1_label, R.id.widget_row1_value, R.id.widget_row1_progress),
                Triple(R.id.widget_row2_label, R.id.widget_row2_value, R.id.widget_row2_progress),
                Triple(R.id.widget_row3_label, R.id.widget_row3_value, R.id.widget_row3_progress)
            )
            config.metrics.zip(ids).forEach { (metric, viewIds) ->
                val (label, value, progress) = row(metric)
                setTextViewText(viewIds.first, label)
                setTextViewText(viewIds.second, value)
                setProgressBar(viewIds.third, 100, progress, false)
            }
            setOnClickPendingIntent(R.id.widget_root, open)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun percent(value: Double, goal: Double): Int = (value * 100 / goal.coerceAtLeast(1.0)).toInt().coerceIn(0, 100)

    /** A donut for today's calories with the number left in the middle (RemoteViews cannot draw custom shapes). */
    private fun calorieRing(progress: Float, value: String, label: String, light: Boolean): Bitmap {
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
            color = if (light) 0xFFECE6DC.toInt() else 0xFF45413A.toInt()
        }
        canvas.drawArc(rect, -90f, 360f, false, ring)
        ring.color = when {
            over -> 0xFFE5655B.toInt()
            light -> 0xFFD9622B.toInt()
            else -> 0xFFEE8A47.toInt()
        }
        if (progress > 0f) canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0.02f, 1f), false, ring)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (light) 0xFF1F1E1B.toInt() else 0xFFF5F1EA.toInt()
            textSize = if (value.length >= 5) 76f else 92f
        }
        canvas.drawText(value, size / 2f, size / 2f + 14f, text)
        text.typeface = Typeface.DEFAULT
        text.color = if (light) 0xFF6B6860.toInt() else 0xFFB1ACA0.toInt()
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

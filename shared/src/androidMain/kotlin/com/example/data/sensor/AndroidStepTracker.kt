package com.example.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.util.toKey
import com.example.util.todayDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the hardware step counter (cumulative since boot) and reports the steps taken since the previous
 * reading. The last reading is persisted, so steps walked while the app was closed are counted the next
 * time it opens (same day only).
 */
class AndroidStepTracker(
    private val context: Context,
    private val requestActivityPermission: () -> Unit
) : StepTracker, SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("step_counter", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(StepTrackingMode.MANUAL)
    override val mode: StateFlow<StepTrackingMode> = _mode.asStateFlow()

    override var onSteps: (Int) -> Unit = {}

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    override fun refresh() {
        _mode.value = when {
            sensor == null -> StepTrackingMode.MANUAL
            !hasPermission() -> StepTrackingMode.NEEDS_PERMISSION
            else -> StepTrackingMode.AUTO
        }
        if (_mode.value == StepTrackingMode.AUTO) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, 5_000_000)
        }
    }

    override fun requestPermission() = requestActivityPermission()

    override fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values[0].toLong()
        val today = todayDate().toKey()
        val now = System.currentTimeMillis()
        // Boot time rounded to 10 s so clock drift doesn't look like a reboot.
        val bootTime = (now - SystemClock.elapsedRealtime()) / 10_000
        val last = prefs.getLong(KEY_LAST_TOTAL, -1L)
        val lastDate = prefs.getString(KEY_LAST_DATE, null)
        val lastBoot = prefs.getLong(KEY_LAST_BOOT, -1L)
        val lastTime = prefs.getLong(KEY_LAST_TIME, now)
        val rebooted = lastBoot >= 0 && kotlin.math.abs(bootTime - lastBoot) > 1

        val raw = when {
            last < 0 || lastDate != today -> 0L    // first reading or new day: set baseline
            rebooted -> total                      // counter restarted at boot
            total >= last -> total - last
            else -> 0L                             // sensor glitch (value went down): re-baseline only
        }
        // Nobody walks more than ~3 steps a second; anything above that is a sensor jump.
        val maxPlausible = ((now - lastTime).coerceAtLeast(0) / 1000) * 3 + 50
        val delta = if (raw > maxPlausible) 0L else raw

        prefs.edit()
            .putLong(KEY_LAST_TOTAL, total)
            .putString(KEY_LAST_DATE, today)
            .putLong(KEY_LAST_BOOT, bootTime)
            .putLong(KEY_LAST_TIME, now)
            .apply()
        if (delta > 0) onSteps(delta.toInt())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val KEY_LAST_TOTAL = "last_total"
        const val KEY_LAST_DATE = "last_date"
        const val KEY_LAST_BOOT = "last_boot"
        const val KEY_LAST_TIME = "last_time"
    }
}

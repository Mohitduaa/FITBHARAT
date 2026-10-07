package com.example.platform

import com.example.data.sensor.StepTracker
import com.example.data.sensor.StepTrackingMode
import com.example.util.toKey
import com.example.util.todayDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusNotDetermined
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults

/**
 * Counts steps with the iPhone's motion coprocessor (CMPedometer). The pedometer reports the running total
 * since midnight, so the tracker keeps the last total it has seen and hands only the difference to the app.
 * Steps walked while the app was closed are picked up the next time it opens, because CoreMotion keeps the
 * day's history.
 */
class IosStepTracker : StepTracker {
    private val pedometer = CMPedometer()
    private val defaults = NSUserDefaults.standardUserDefaults
    private var running = false

    private val _mode = MutableStateFlow(StepTrackingMode.MANUAL)
    override val mode: StateFlow<StepTrackingMode> = _mode.asStateFlow()

    override var onSteps: (Int) -> Unit = {}

    private fun startOfToday(): NSDate = NSCalendar.currentCalendar.startOfDayForDate(NSDate())

    private fun currentMode(): StepTrackingMode {
        if (!CMPedometer.isStepCountingAvailable()) return StepTrackingMode.MANUAL
        return when (CMPedometer.authorizationStatus()) {
            CMAuthorizationStatusAuthorized -> StepTrackingMode.AUTO
            CMAuthorizationStatusNotDetermined -> StepTrackingMode.NEEDS_PERMISSION
            else -> StepTrackingMode.MANUAL // denied or restricted
        }
    }

    override fun refresh() {
        _mode.value = currentMode()
        if (_mode.value == StepTrackingMode.AUTO) start()
    }

    /** A first query makes iOS show its motion-permission prompt; refresh again once it is answered. */
    override fun requestPermission() {
        pedometer.queryPedometerDataFromDate(startOfToday(), toDate = NSDate()) { _, _ -> refresh() }
    }

    override fun stop() {
        if (running) pedometer.stopPedometerUpdates()
        running = false
    }

    private fun start() {
        if (running) return
        running = true
        pedometer.startPedometerUpdatesFromDate(startOfToday()) { data, _ ->
            if (data != null) report(data.numberOfSteps.longValue)
        }
    }

    private fun report(total: Long) {
        val today = todayDate().toKey()
        val lastDate = defaults.stringForKey(KEY_DATE)
        val lastTotal = defaults.integerForKey(KEY_TOTAL)
        val delta = when {
            lastDate != today -> total          // first reading of the day: everything walked so far
            total >= lastTotal -> total - lastTotal
            else -> 0L
        }
        defaults.setObject(today, forKey = KEY_DATE)
        defaults.setInteger(total, forKey = KEY_TOTAL)
        if (delta > 0) onSteps(delta.toInt())
    }

    private companion object {
        const val KEY_DATE = "fb_steps_date"
        const val KEY_TOTAL = "fb_steps_total"
    }
}

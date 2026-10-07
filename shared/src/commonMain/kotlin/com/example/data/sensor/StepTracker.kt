package com.example.data.sensor

import kotlinx.coroutines.flow.StateFlow

enum class StepTrackingMode {
    /** The phone's step sensor is active. */
    AUTO,

    /** Sensor exists but the activity / motion permission is missing. */
    NEEDS_PERMISSION,

    /** No step sensor on this device; steps are added manually. */
    MANUAL
}

/**
 * Counts steps using the device's motion hardware. Implementations report only the steps taken since the
 * previous report (never a running total), so the repository can simply add them to today's log.
 */
interface StepTracker {
    val mode: StateFlow<StepTrackingMode>

    /** Called with newly counted steps; set once by the ViewModel. */
    var onSteps: (Int) -> Unit

    /** Re-checks availability / permission and (re)starts counting. Call when the app becomes visible. */
    fun refresh()

    /** Asks the OS for motion permission (shows the system prompt when needed). */
    fun requestPermission()

    /** Stops counting; call when the app goes to the background. */
    fun stop()
}

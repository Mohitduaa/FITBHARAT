package com.example.platform

import androidx.compose.runtime.Composable
import androidx.room.RoomDatabase
import com.example.data.local.AppDatabase
import com.example.data.sensor.StepTracker

/**
 * Everything the shared code needs from the host OS. Android and iOS each provide an implementation,
 * so the app logic and UI stay identical on both.
 */
interface PlatformServices {
    /** Gemini API key, or null when none is configured (the app then falls back to offline coach tips). */
    val geminiApiKey: String?

    val stepTracker: StepTracker

    val reminders: ReminderScheduler

    fun databaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun logError(tag: String, message: String, throwable: Throwable? = null)
}

/** Opens the camera / photo library and hands back a downscaled JPEG. */
interface PhotoPicker {
    fun takePhoto()
    fun pickFromGallery()
}

/** Each platform wires its own camera / gallery APIs (permission prompts, file providers, ...). */
@Composable
expect fun rememberPhotoPicker(
    onPhoto: (jpeg: ByteArray) -> Unit,
    onError: (message: String) -> Unit,
    maxDimension: Int = 1280
): PhotoPicker

/** Keeps the status / navigation bar icons readable when the app theme differs from the system one. */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)

/** Platform back-button / swipe-back handling. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Saves / loads the backup file through the system file UI (Files, Google Drive, share sheet, ...). */
interface BackupFiles {
    /** Lets the user choose where to save [content] (Drive, Files, WhatsApp, ...). */
    fun save(fileName: String, content: String)

    /** Lets the user pick a backup file; the text content is delivered to the callback given at creation. */
    fun pick()
}

@Composable
expect fun rememberBackupFiles(
    onPicked: (content: String) -> Unit,
    onSaved: () -> Unit,
    onError: (message: String) -> Unit
): BackupFiles

/** Scans a product barcode (EAN / UPC) with the camera. */
interface BarcodeScanner {
    /** False on platforms where scanning is not available yet. */
    val isAvailable: Boolean
    fun scan()
}

@Composable
expect fun rememberBarcodeScanner(
    onScanned: (barcode: String) -> Unit,
    onError: (message: String) -> Unit
): BarcodeScanner

/** Daily local notifications. Implementations keep the schedule across reboots. */
interface ReminderScheduler {
    /** Turns each reminder on or off and schedules it at the user's chosen [times]. */
    fun apply(water: Boolean, meals: Boolean, walk: Boolean, times: com.example.data.model.ReminderTimes)

    /** Asks for notification permission where the OS requires it (Android 13+, iOS). */
    fun requestPermission()
}

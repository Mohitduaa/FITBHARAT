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

    val autoBackup: AutoBackup

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

    /** A one-off "fast complete" notification at [atMillis]; null cancels it. */
    fun scheduleFastEnd(atMillis: Long?)
}

/** Where the daily backup stands, for the settings screen. */
data class AutoBackupState(
    val enabled: Boolean = false,
    /** Human-readable place the files go, or null when no folder has been chosen yet. */
    val location: String? = null,
    val lastBackupMillis: Long? = null,
    /** Android lets the user pick the folder; iOS always uses the app's Files folder. */
    val canChangeFolder: Boolean = false,
    val error: String? = null
)

/**
 * Writes a backup file once a day without the user doing anything, keeping the last [KEEP] days.
 * The platform decides where (a user-picked folder on Android, the Files app on iOS).
 */
interface AutoBackup {
    val state: kotlinx.coroutines.flow.StateFlow<AutoBackupState>

    /** Called once by the app container with the function that produces the backup JSON. */
    fun attach(exporter: suspend () -> String)

    /** Turning it on may first ask for a folder (Android). */
    fun setEnabled(enabled: Boolean)

    fun changeFolder()

    fun backupNow()

    /** Runs a backup when the last one is more than a day old; call when the app comes to the foreground. */
    fun backupIfDue()

    companion object {
        const val KEEP = 7
        const val FILE_PREFIX = "fitbharat-auto-"
    }
}

@file:OptIn(ExperimentalForeignApi::class)

package com.example.platform

import com.example.util.currentTimeMillis
import com.example.util.toKey
import com.example.util.todayDate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * Daily backup into the app's Documents/Backups folder, which the Files app shows under
 * "On My iPhone › FitBharat" and which iCloud device backup includes. Runs when the app comes to the
 * foreground and the last backup is more than a day old.
 */
class IosAutoBackup : AutoBackup {
    private var exporter: (suspend () -> String)? = null
    private val defaults = NSUserDefaults.standardUserDefaults
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(readState())
    override val state: StateFlow<AutoBackupState> = _state.asStateFlow()

    private fun readState(error: String? = null) = AutoBackupState(
        enabled = defaults.boolForKey(KEY_ENABLED),
        location = "Files › On My iPhone › FitBharat › Backups",
        lastBackupMillis = defaults.doubleForKey(KEY_LAST).toLong().takeIf { it > 0 },
        canChangeFolder = false,
        error = error
    )

    private fun backupDirectory(): String {
        val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        return "$documents/Backups"
    }

    override fun attach(exporter: suspend () -> String) {
        this.exporter = exporter
    }

    override fun setEnabled(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_ENABLED)
        _state.value = readState()
        if (enabled) backupIfDue()
    }

    override fun changeFolder() = Unit

    override fun backupNow() {
        scope.launch { runBackup() }
    }

    override fun backupIfDue() {
        val last = defaults.doubleForKey(KEY_LAST).toLong()
        if (defaults.boolForKey(KEY_ENABLED) && currentTimeMillis() - last > DUE_AFTER_MILLIS) backupNow()
    }

    private suspend fun runBackup() {
        val export = exporter ?: return
        try {
            val content = export()
            val directory = backupDirectory()
            val files = NSFileManager.defaultManager
            files.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
            val path = "$directory/${AutoBackup.FILE_PREFIX}${todayDate().toKey()}.json"
            val written = NSString.create(string = content)
                .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            if (!written) error("write failed")

            files.contentsOfDirectoryAtPath(directory, error = null).orEmpty()
                .mapNotNull { it as? String }
                .filter { it.startsWith(AutoBackup.FILE_PREFIX) }
                .sortedDescending()
                .drop(AutoBackup.KEEP)
                .forEach { files.removeItemAtPath("$directory/$it", error = null) }

            defaults.setDouble(currentTimeMillis().toDouble(), forKey = KEY_LAST)
            _state.value = readState()
        } catch (e: Exception) {
            _state.value = readState(error = "Backup failed. Please try \"Back up now\" again.")
        }
    }

    private companion object {
        const val KEY_ENABLED = "fb_auto_backup_enabled"
        const val KEY_LAST = "fb_auto_backup_last"
        const val DUE_AFTER_MILLIS = 20 * 60 * 60 * 1000L
    }
}

@file:OptIn(ExperimentalForeignApi::class)

package com.example.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_NAME
import com.example.data.sensor.StepTracker
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

class IosPlatformServices(override val geminiApiKey: String?) : PlatformServices {
    override val stepTracker: StepTracker = IosStepTracker()

    override val reminders: ReminderScheduler = IosReminderScheduler()

    override val autoBackup: AutoBackup = IosAutoBackup()

    private fun directory(kind: ULong): String =
        NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true).firstOrNull() as? String
            ?: NSTemporaryDirectory()

    /**
     * The database lives in Application Support, which the Files app does not show (Documents is shared
     * there for the daily backups). Early builds kept it in Documents, so move it over once.
     */
    override fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val files = NSFileManager.defaultManager
        val support = directory(NSApplicationSupportDirectory)
        files.createDirectoryAtPath(support, withIntermediateDirectories = true, attributes = null, error = null)
        val documents = directory(NSDocumentDirectory)
        val newPath = "$support/$DATABASE_NAME"
        if (!files.fileExistsAtPath(newPath) && files.fileExistsAtPath("$documents/$DATABASE_NAME")) {
            listOf("", "-wal", "-shm").forEach { suffix ->
                val old = "$documents/$DATABASE_NAME$suffix"
                if (files.fileExistsAtPath(old)) files.moveItemAtPath(old, toPath = "$newPath$suffix", error = null)
            }
        }
        return Room.databaseBuilder<AppDatabase>(name = newPath)
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] $message ${throwable?.message.orEmpty()}")
    }
}

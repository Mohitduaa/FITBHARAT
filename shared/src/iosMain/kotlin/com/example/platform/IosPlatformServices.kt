package com.example.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_NAME
import com.example.data.sensor.StepTracker
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

class IosPlatformServices(override val geminiApiKey: String?) : PlatformServices {
    override val stepTracker: StepTracker = IosStepTracker()

    override val reminders: ReminderScheduler = IosReminderScheduler()

    override fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        return Room.databaseBuilder<AppDatabase>(name = "$directory/$DATABASE_NAME")
    }

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] $message ${throwable?.message.orEmpty()}")
    }
}

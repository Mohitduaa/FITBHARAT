package com.example.platform

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_NAME
import com.example.data.sensor.AndroidStepTracker
import com.example.data.sensor.StepTracker

class AndroidPlatformServices(
    context: Context,
    override val geminiApiKey: String?
) : PlatformServices {
    private val appContext = context.applicationContext

    /** Set by the visible Activity, which owns the permission-result launcher. */
    var activityPermissionRequester: () -> Unit = {}

    override val stepTracker: StepTracker = AndroidStepTracker(appContext) { activityPermissionRequester() }

    override val reminders = AndroidReminderScheduler(appContext)

    override val autoBackup = AndroidAutoBackup(appContext)

    override fun databaseBuilder(): RoomDatabase.Builder<AppDatabase> =
        Room.databaseBuilder<AppDatabase>(context = appContext, name = DATABASE_NAME)

    override fun logError(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

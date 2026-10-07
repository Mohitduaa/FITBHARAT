package com.example

import android.app.Application
import com.example.platform.AndroidPlatformServices

class FitBharatApplication : Application(), com.example.platform.AppContainerHolder {
    val platform: AndroidPlatformServices by lazy {
        AndroidPlatformServices(this, geminiApiKey = BuildConfig.GEMINI_API_KEY)
    }

    override fun onCreate() {
        super.onCreate()
        // Keeps a placed home screen widget in step with the data while the app process is running.
        com.example.widget.TodayWidget.keepUpdated(this)
    }

    /** One container (and therefore one database) per process, shared by every Activity instance. */
    override val container: AppContainer by lazy { AppContainer(platform) }
}

package com.example

import android.app.Application
import com.example.platform.AndroidPlatformServices

class FitBharatApplication : Application() {
    val platform: AndroidPlatformServices by lazy {
        AndroidPlatformServices(this, geminiApiKey = BuildConfig.GEMINI_API_KEY)
    }

    /** One container (and therefore one database) per process, shared by every Activity instance. */
    val container: AppContainer by lazy { AppContainer(platform) }
}

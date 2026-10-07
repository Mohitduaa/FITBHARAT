package com.example

import com.example.data.local.AppDatabase
import com.example.data.local.configure
import com.example.platform.PlatformServices

/** Holds the long-lived objects of the app; created once per process by the platform entry point. */
class AppContainer(val platform: PlatformServices) {
    val database: AppDatabase by lazy { platform.databaseBuilder().configure() }
}

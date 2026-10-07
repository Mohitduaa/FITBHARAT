package com.example

import com.example.data.local.AppDatabase
import com.example.data.local.configure
import com.example.data.backup.BackupManager
import com.example.data.repository.FitnessRepository
import com.example.platform.PlatformServices

/** Holds the long-lived objects of the app; created once per process by the platform entry point. */
class AppContainer(val platform: PlatformServices) {
    val database: AppDatabase by lazy { platform.databaseBuilder().configure() }

    /** The single repository over [database]; host apps (and widgets) use this instead of touching Room. */
    val repository: FitnessRepository by lazy { FitnessRepository(database.appDao()) }

    init {
        platform.autoBackup.attach { BackupManager(database.appDao()).export() }
    }
}

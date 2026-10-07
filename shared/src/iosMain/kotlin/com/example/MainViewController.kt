package com.example

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.platform.IosPlatformServices
import com.example.ui.FitBharatRoot
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/** One container (and so one database) per process. */
private val container: AppContainer by lazy {
    // CI writes the key into Info.plist; blank or placeholder values make the app fall back to offline tips.
    val key = NSBundle.mainBundle.objectForInfoDictionaryKey("GEMINI_API_KEY") as? String
    AppContainer(IosPlatformServices(geminiApiKey = key))
}

/** Entry point called from the SwiftUI shell in iosApp. */
fun MainViewController(): UIViewController {
    installCrashLogger()
    val previousCrash = readCrashReport()
    return ComposeUIViewController {
        var crash by remember { mutableStateOf(previousCrash) }
        val report = crash
        if (report != null) {
            CrashReportScreen(report) {
                clearCrashReport()
                crash = null
            }
        } else {
            val steps = container.platform.stepTracker
            LaunchedEffect(Unit) { steps.refresh() }
            LifecycleEventEffect(Lifecycle.Event.ON_START) {
                steps.refresh()
                container.platform.autoBackup.backupIfDue()
            }
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { steps.stop() }
            FitBharatRoot(container)
        }
    }
}

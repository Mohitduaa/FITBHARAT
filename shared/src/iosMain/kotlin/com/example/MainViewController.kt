package com.example

import androidx.compose.runtime.LaunchedEffect
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
fun MainViewController(): UIViewController = ComposeUIViewController {
    val steps = container.platform.stepTracker
    LaunchedEffect(Unit) { steps.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { steps.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { steps.stop() }
    FitBharatRoot(container)
}

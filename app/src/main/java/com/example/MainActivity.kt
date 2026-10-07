package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.FitBharatRoot

class MainActivity : ComponentActivity() {
    private val app get() = application as FitBharatApplication

    private val activityPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            app.platform.stepTracker.refresh()
        }

    private val backupFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            app.platform.autoBackup.onFolderPicked(uri)
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app.platform.reminders.permissionRequester = {
            if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        app.platform.activityPermissionRequester = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        app.platform.autoBackup.folderPicker = { backupFolderLauncher.launch(null) }
        setContent { FitBharatRoot(app.container) }
    }

    override fun onStart() {
        super.onStart()
        app.platform.stepTracker.refresh()
        app.platform.autoBackup.backupIfDue()
    }

    override fun onStop() {
        super.onStop()
        app.platform.stepTracker.stop()
        com.example.widget.TodayWidget.refreshAsync(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) app.platform.activityPermissionRequester = {}
    }
}

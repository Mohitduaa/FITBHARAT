package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.topViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController

/*
 * Without a Mac there is no crash log on hand, so the app records its own: an uncaught Kotlin exception is
 * written to a file, and the next launch shows it on screen (with a Share button) before starting the app.
 */

private fun crashFile(): String {
    val directory = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: NSTemporaryDirectory()
    return "$directory/last_crash.txt"
}

@OptIn(ExperimentalNativeApi::class)
internal fun installCrashLogger() {
    setUnhandledExceptionHook { error ->
        runCatching {
            NSString.create(string = error.stackTraceToString())
                .writeToFile(crashFile(), atomically = true, encoding = NSUTF8StringEncoding, error = null)
        }
    }
}

internal fun readCrashReport(): String? =
    NSString.stringWithContentsOfFile(crashFile(), encoding = NSUTF8StringEncoding, error = null)
        ?.takeIf { it.isNotBlank() }

internal fun clearCrashReport() {
    NSFileManager.defaultManager.removeItemAtPath(crashFile(), error = null)
}

@Composable
internal fun CrashReportScreen(report: String, onContinue: () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.safeDrawingPadding().padding(16.dp)) {
                Text("FitBharat closed unexpectedly last time", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Please send a screenshot of this screen (or tap Share) so it can be fixed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.take(4000),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { shareText(report) }, modifier = Modifier.weight(1f)) { Text("Share") }
                    Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("Try again") }
                }
            }
        }
    }
}

private fun shareText(text: String) {
    val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    topViewController()?.presentViewController(sheet, animated = true, completion = null)
}

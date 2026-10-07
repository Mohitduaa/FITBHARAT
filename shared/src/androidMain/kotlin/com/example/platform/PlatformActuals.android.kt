package com.example.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun rememberPhotoPicker(
    onPhoto: (jpeg: ByteArray) -> Unit,
    onError: (message: String) -> Unit,
    maxDimension: Int
): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPhoto by rememberUpdatedState(onPhoto)
    val currentOnError by rememberUpdatedState(onError)

    // The camera writes the full-resolution photo here; the preview thumbnail is too small to recognise food.
    val cameraUri = remember { ScanImageLoader.createCameraUri(context) }

    fun deliver(uri: Uri) {
        scope.launch {
            try {
                currentOnPhoto(withContext(Dispatchers.IO) { ScanImageLoader.loadJpeg(context, uri, maxDimension) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                currentOnError("Photo load nahi ho payi. Dusri photo ke saath try karein.")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::deliver)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) deliver(cameraUri)
    }

    fun launchCamera() {
        try {
            cameraLauncher.launch(cameraUri)
        } catch (e: Exception) {
            currentOnError("Camera is not available on this device. Please pick from Gallery.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else currentOnError("Camera permission not granted. Please pick from Gallery or allow permission in device settings.")
    }

    return remember {
        object : PhotoPicker {
            override fun takePhoto() {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            override fun pickFromGallery() {
                try {
                    galleryLauncher.launch("image/*")
                } catch (e: Exception) {
                    currentOnError("Gallery not accessible. Please try the camera.")
                }
            }
        }
    }
}

@Composable
actual fun rememberBackupFiles(
    onPicked: (content: String) -> Unit,
    onSaved: () -> Unit,
    onError: (message: String) -> Unit
): BackupFiles {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnSaved by rememberUpdatedState(onSaved)
    val currentOnError by rememberUpdatedState(onError)
    // The content to write is held here until the user has chosen a destination.
    val pending = remember { arrayOf<String?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pending[0]
        pending[0] = null
        if (uri != null && content != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) }
                            ?: error("Could not open destination")
                    }
                    currentOnSaved()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    currentOnError("The backup could not be saved. Please try another location.")
                }
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                            ?: error("Could not open file")
                    }
                    currentOnPicked(text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    currentOnError("The selected file could not be read.")
                }
            }
        }
    }

    return remember {
        object : BackupFiles {
            override fun save(fileName: String, content: String) {
                pending[0] = content
                createLauncher.launch(fileName)
            }

            override fun pick() {
                openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
            }
        }
    }
}

@Composable
actual fun rememberBarcodeScanner(
    onScanned: (barcode: String) -> Unit,
    onError: (message: String) -> Unit
): BarcodeScanner {
    val context = LocalContext.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnError by rememberUpdatedState(onError)
    return remember(context) {
        object : BarcodeScanner {
            override val isAvailable = true

            override fun scan() {
                val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13,
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8,
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A,
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
                    )
                    .build()
                com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
                    .startScan()
                    .addOnSuccessListener { barcode ->
                        val value = barcode.rawValue
                        if (value.isNullOrBlank()) currentOnError("Could not read the barcode. Please try again.")
                        else currentOnScanned(value)
                    }
                    .addOnFailureListener { error ->
                        android.util.Log.e("BarcodeScanner", "Scan failed", error)
                        val code = (error as? com.google.mlkit.common.MlKitException)?.errorCode
                        currentOnError(
                            when (code) {
                                com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED -> return@addOnFailureListener
                                com.google.mlkit.common.MlKitException.UNAVAILABLE ->
                                    "The scanner is being downloaded by Google Play services. Please try again in a minute."
                                else -> "The barcode scanner could not start. Please try again."
                            }
                        )
                    }
                    .addOnCanceledListener { }
            }
        }
    }
}

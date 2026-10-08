@file:OptIn(ExperimentalForeignApi::class)

package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIPresentationController
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.presentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Camera barcode scanner for packaged foods (EAN / UPC). Shown as a sheet with a live camera preview; the
 * first code it reads closes the sheet. Swiping the sheet down cancels.
 */
private class ScannerSession(
    private val onScanned: (String) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol, UIAdaptivePresentationControllerDelegateProtocol {
    private val session = AVCaptureSession()
    private val controller = UIViewController()
    private var finished = false

    fun present() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as? AVCaptureDeviceInput }
        if (input == null || !session.canAddInput(input)) {
            onError("Camera access is needed to scan barcodes. Allow it in Settings › FitBharat › Camera.")
            return
        }
        session.addInput(input)
        val output = AVCaptureMetadataOutput()
        if (!session.canAddOutput(output)) {
            onError("The camera could not start. Please try again.")
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
        output.metadataObjectTypes = listOf(
            AVMetadataObjectTypeEAN13Code,
            AVMetadataObjectTypeEAN8Code,
            AVMetadataObjectTypeUPCECode,
            AVMetadataObjectTypeCode128Code
        )

        val (width, height) = UIScreen.mainScreen.bounds.useContents { size.width to size.height }
        controller.view.backgroundColor = UIColor.blackColor
        val preview = AVCaptureVideoPreviewLayer(session = session)
        preview.videoGravity = AVLayerVideoGravityResizeAspectFill
        preview.frame = CGRectMake(0.0, 0.0, width, height)
        controller.view.layer.addSublayer(preview)
        val hint = UILabel(frame = CGRectMake(24.0, 40.0, width - 48.0, 44.0))
        hint.text = "Point the camera at the barcode · swipe down to close"
        hint.textColor = UIColor.whiteColor
        hint.font = UIFont.boldSystemFontOfSize(15.0)
        hint.numberOfLines = 2
        controller.view.addSubview(hint)

        topViewController()?.presentViewController(controller, animated = true) {
            controller.presentationController?.delegate = this
            session.startRunning()
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (finished) return
        val code = didOutputMetadataObjects
            .firstNotNullOfOrNull { (it as? AVMetadataMachineReadableCodeObject)?.stringValue }
            ?: return
        finished = true
        session.stopRunning()
        controller.dismissViewControllerAnimated(true) { onScanned(code) }
    }

    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
        finished = true
        session.stopRunning()
    }
}

private class IosBarcodeScanner(
    private val onScanned: (String) -> Unit,
    private val onError: (String) -> Unit
) : BarcodeScanner {
    // The sheet only holds its delegate weakly, so keep the running session alive here.
    private var current: ScannerSession? = null

    override val isAvailable: Boolean = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null

    override fun scan() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                if (!granted) {
                    onError("Camera access is needed to scan barcodes. Allow it in Settings › FitBharat › Camera.")
                } else {
                    current = ScannerSession(onScanned, onError).also { it.present() }
                }
            }
        }
    }
}

@Composable
actual fun rememberBarcodeScanner(
    onScanned: (barcode: String) -> Unit,
    onError: (message: String) -> Unit
): BarcodeScanner {
    val latestScanned = rememberUpdatedState(onScanned)
    val latestError = rememberUpdatedState(onError)
    return remember { IosBarcodeScanner({ latestScanned.value(it) }, { latestError.value(it) }) }
}

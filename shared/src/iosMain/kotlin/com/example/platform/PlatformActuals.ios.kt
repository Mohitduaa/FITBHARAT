@file:OptIn(ExperimentalForeignApi::class)

package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import platform.posix.memcpy

/** The view controller currently on screen, used to present system pickers and the share sheet. */
internal fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (true) {
        val next = controller?.presentedViewController ?: break
        controller = next
    }
    return controller
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

/** Scales the picture so its longest side is [maxDimension] and encodes it as JPEG. */
private fun UIImage.toJpeg(maxDimension: Int): ByteArray? {
    val (width, height) = size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = minOf(1.0, maxDimension / maxOf(width, height))
    val target = CGSizeMake(width * scale, height * scale)
    UIGraphicsBeginImageContextWithOptions(target, false, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    val data = UIImageJPEGRepresentation(resized ?: this, 0.85) ?: return null
    return data.toByteArray()
}

// ---------------------------------------------------------------- photo picker

private class PhotoDelegate(
    private val maxDimension: Int,
    private val onPhoto: (ByteArray) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        val jpeg = image?.toJpeg(maxDimension)
        if (jpeg != null) onPhoto(jpeg) else onError("Could not read that picture. Please try another one.")
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private class IosPhotoPicker(
    private val maxDimension: Int,
    private val onPhoto: (ByteArray) -> Unit,
    private val onError: (String) -> Unit
) : PhotoPicker {
    // The picker only holds its delegate weakly, so keep it alive here.
    private var delegate: PhotoDelegate? = null

    override fun takePhoto() = present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera, "The camera is not available on this device.")

    override fun pickFromGallery() = present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary, "The photo library is not available.")

    private fun present(source: UIImagePickerControllerSourceType, unavailableMessage: String) {
        if (!UIImagePickerController.isSourceTypeAvailable(source)) {
            onError(unavailableMessage)
            return
        }
        val photoDelegate = PhotoDelegate(maxDimension, onPhoto, onError)
        delegate = photoDelegate
        val picker = UIImagePickerController()
        picker.sourceType = source
        picker.delegate = photoDelegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberPhotoPicker(
    onPhoto: (jpeg: ByteArray) -> Unit,
    onError: (message: String) -> Unit,
    maxDimension: Int
): PhotoPicker {
    val latestOnPhoto = rememberUpdatedState(onPhoto)
    val latestOnError = rememberUpdatedState(onError)
    return remember(maxDimension) {
        IosPhotoPicker(maxDimension, { latestOnPhoto.value(it) }, { latestOnError.value(it) })
    }
}

// ---------------------------------------------------------------- system bars

/** iOS picks the status bar style from the content; nothing to wire for now. */
@Composable
actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit

// ---------------------------------------------------------------- back handling

/** iOS already has the edge-swipe back gesture handled by the system; nothing extra to wire. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

// ---------------------------------------------------------------- backup files

private class FilePickerDelegate(
    private val onPicked: (String) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val text = url?.let { NSString.stringWithContentsOfURL(it, encoding = NSUTF8StringEncoding, error = null) }
        if (text != null) onPicked(text) else onError("Could not read that file.")
    }
}

private class IosBackupFiles(
    private val onPicked: (String) -> Unit,
    private val onSaved: () -> Unit,
    private val onError: (String) -> Unit
) : BackupFiles {
    private var pickerDelegate: FilePickerDelegate? = null

    /** Writes the backup to a temporary file and opens the share sheet (Save to Files, WhatsApp, Drive, ...). */
    override fun save(fileName: String, content: String) {
        val path = NSTemporaryDirectory() + fileName
        val written = NSString.create(string = content)
            .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) {
            onError("Could not create the backup file.")
            return
        }
        val sheet = UIActivityViewController(activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null)
        sheet.completionWithItemsHandler = { _, completed, _, _ -> if (completed) onSaved() }
        topViewController()?.presentViewController(sheet, animated = true, completion = null)
    }

    override fun pick() {
        val delegate = FilePickerDelegate(onPicked, onError)
        pickerDelegate = delegate
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText, UTTypeData),
            asCopy = true
        )
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberBackupFiles(
    onPicked: (content: String) -> Unit,
    onSaved: () -> Unit,
    onError: (message: String) -> Unit
): BackupFiles {
    val latestPicked = rememberUpdatedState(onPicked)
    val latestSaved = rememberUpdatedState(onSaved)
    val latestError = rememberUpdatedState(onError)
    return remember {
        IosBackupFiles({ latestPicked.value(it) }, { latestSaved.value() }, { latestError.value(it) })
    }
}

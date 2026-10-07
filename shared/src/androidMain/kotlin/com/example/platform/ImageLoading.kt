package com.example.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Loads food photos at a size that is sharp enough for recognition but cheap to upload. */
internal object ScanImageLoader {

    /** A cache-file URI for the camera app to write the full-resolution photo to. */
    fun createCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "scan").apply { mkdirs() }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "meal.jpg"))
    }

    /** Decodes, downscales and JPEG-compresses [uri]; must be called off the main thread. */
    fun loadJpeg(context: Context, uri: Uri, maxDimension: Int = 1280): ByteArray {
        val bitmap = load(context, uri, maxDimension)
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private fun load(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies EXIF rotation.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = maxDimension.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Could not open image")
        }
    }
}

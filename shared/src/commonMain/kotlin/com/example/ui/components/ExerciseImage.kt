package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import fitbharat.shared.generated.resources.Res
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.decodeToImageBitmap

private fun fileKey(imageId: String) = imageId.lowercase().replace(Regex("[^a-z0-9]"), "_")

private suspend fun loadFrame(imageId: String, frame: Int): ImageBitmap? = try {
    Res.readBytes("files/exercises/${fileKey(imageId)}_$frame.jpg").decodeToImageBitmap()
} catch (e: Exception) {
    null
}

/**
 * Photo of an exercise. With [animate] the two photos (start / end position) alternate, which shows
 * how the movement is performed. Falls back to a plain icon when there is no photo.
 */
@Composable
fun ExerciseImage(imageId: String, modifier: Modifier = Modifier, animate: Boolean = false) {
    var frames by remember(imageId) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var current by remember(imageId) { mutableStateOf(0) }

    LaunchedEffect(imageId) {
        frames = if (imageId.isEmpty()) emptyList() else listOfNotNull(loadFrame(imageId, 0), loadFrame(imageId, 1))
    }
    LaunchedEffect(frames, animate) {
        if (animate && frames.size > 1) {
            while (true) {
                delay(900)
                current = (current + 1) % frames.size
            }
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val frame = frames.getOrNull(current)
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }
}

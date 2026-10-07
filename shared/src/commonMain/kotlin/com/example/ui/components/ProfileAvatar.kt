package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.platform.rememberPhotoPicker
import fitbharat.shared.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

const val AVATAR_PRESET_COUNT = 12
const val AVATAR_PHOTO = "photo"

fun presetAvatarKey(number: Int) = "preset:${number.toString().padStart(2, '0')}"

/** The avatar to show when the user has not picked one. */
fun defaultAvatarKey(gender: String) = presetAvatarKey(if (gender.equals("Female", ignoreCase = true)) 2 else 1)

@OptIn(ExperimentalEncodingApi::class)
private suspend fun loadAvatar(key: String, photo: String): ImageBitmap? = try {
    when {
        key == AVATAR_PHOTO && photo.isNotEmpty() -> Base64.decode(photo).decodeToImageBitmap()
        key.startsWith("preset:") -> Res.readBytes("files/avatars/avatar_${key.removePrefix("preset:")}.png").decodeToImageBitmap()
        else -> null
    }
} catch (e: Exception) {
    null
}

/** Circular avatar: a bundled illustration or the user's own photo. */
@Composable
fun ProfileAvatar(
    avatar: String,
    avatarPhoto: String,
    gender: String,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val key = avatar.ifEmpty { defaultAvatarKey(gender) }
    var image by remember(key, avatarPhoto) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(key, avatarPhoto) { image = loadAvatar(key, avatarPhoto) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .testTag("profile_avatar"),
        contentAlignment = Alignment.Center
    ) {
        image?.let {
            Image(bitmap = it, contentDescription = "Profile avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Avatar chooser for the profile screen: the twelve illustrations plus "use my photo".
 * Reports the new choice through [onChange] as (avatarKey, photoBase64).
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun AvatarPicker(
    avatar: String,
    avatarPhoto: String,
    gender: String,
    onChange: (avatar: String, avatarPhoto: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = avatar.ifEmpty { defaultAvatarKey(gender) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberPhotoPicker(
        onPhoto = { jpeg ->
            error = null
            onChange(AVATAR_PHOTO, Base64.encode(jpeg))
        },
        onError = { error = it },
        maxDimension = 320
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(avatar = current, avatarPhoto = avatarPhoto, gender = gender, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = { photoPicker.pickFromGallery() }, modifier = Modifier.testTag("avatar_use_photo")) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Use my photo")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        (1..AVATAR_PRESET_COUNT).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { number ->
                    val key = presetAvatarKey(number)
                    val selected = key == current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            .clickable { onChange(key, avatarPhoto) }
                            .testTag("avatar_option_$number"),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(avatar = key, avatarPhoto = "", gender = gender, size = 48.dp)
                        if (selected) {
                            Box(
                                modifier = Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

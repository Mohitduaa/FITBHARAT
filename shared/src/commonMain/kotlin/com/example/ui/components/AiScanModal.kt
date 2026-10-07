package com.example.ui.components

import kotlin.math.roundToInt
import com.example.util.toFixed
import com.example.util.toCompactString
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AiFoodScanResult
import com.example.data.model.MealType
import com.example.platform.rememberPhotoPicker
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatTeal
import com.example.ui.theme.ProteinViolet
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.AiScanUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiFoodScanDialog(
    uiState: AiScanUiState,
    onDismiss: () -> Unit,
    onScanPhoto: (ByteArray) -> Unit,
    onAddToDiary: (MealType, AiFoodScanResult) -> Unit,
    onAddManually: () -> Unit
) {
    var selectedMealType by remember { mutableStateOf(mealTypeForNow()) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberPhotoPicker(
        onPhoto = onScanPhoto,
        onError = { message -> scanErrorMessage = message }
    )

    fun launchCameraSafely() {
        scanErrorMessage = null
        photoPicker.takePhoto()
    }

    fun launchGallerySafely() {
        scanErrorMessage = null
        photoPicker.pickFromGallery()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("ai_food_scan_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Food Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Meal recognition",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_ai_scan_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState) {
                    is AiScanUiState.Idle -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                // Camera Viewfinder Mock Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(20.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Photograph Your Meal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "We identify the dishes in your photo and estimate calories and macros for the portion shown.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(18.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { launchCameraSafely() },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f).testTag("camera_photo_button")
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Camera", fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = { launchGallerySafely() },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1f).testTag("gallery_photo_button")
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Gallery", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (scanErrorMessage != null) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                                    .padding(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = scanErrorMessage!!,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                            }

                    }

                    is AiScanUiState.Scanning -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(68.dp),
                                    strokeWidth = 7.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Analyzing your meal...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Identifying dishes, portions and nutrition",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is AiScanUiState.Success -> {
                        val baseResult = uiState.result
                        // Portion editor: a multiplier per detected item (or for the whole meal when none were found).
                        var portions by remember(baseResult) { mutableStateOf(List(baseResult.detectedItems.size) { 1.0 }) }
                        var wholePortion by remember(baseResult) { mutableStateOf(1.0) }
                        val result = remember(baseResult, portions, wholePortion) { baseResult.withPortions(portions, wholePortion) }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Dish Title Card
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(18.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = result.dishName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Health Score ${result.desiHealthScore}/10",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = result.portionDescription,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (baseResult.detectedItems.isEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Portion", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                                PortionStepper(value = wholePortion, onChange = { wholePortion = it }, allowZero = false)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Big Calories & Macro Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            MacroBox("CALORIES", "${result.estimatedCalories} kcal", MaterialTheme.colorScheme.primary)
                                            MacroBox("PROTEIN", "${result.proteinG.toFixed(1)}g", ProteinViolet)
                                            MacroBox("CARBS", "${result.carbsG.toFixed(1)}g", CarbsAmber)
                                            MacroBox("FATS", "${result.fatG.toFixed(1)}g", FatTeal)
                                        }
                                    }
                                }
                            }

                            // What is in the meal
                            if (result.detectedItems.isNotEmpty() || result.ingredients.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(16.dp)
                                            .testTag("scan_ingredients")
                                    ) {
                                        Text(
                                            text = "What is in this meal",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "Adjust portions to match what you ate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        result.detectedItems.forEachIndexed { index, item ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        item.portion,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    PortionStepper(
                                                        value = portions.getOrElse(index) { 1.0 },
                                                        onChange = { newValue -> portions = portions.toMutableList().also { it[index] = newValue } },
                                                        allowZero = true
                                                    )
                                                    Text(
                                                        "${item.calories} kcal",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        if (result.ingredients.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Ingredients",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                result.ingredients.forEach { name ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            name,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Coach Feedback
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Nutrition Feedback",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = result.desiCoachFeedback,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Smart Desi Swaps
                            if (result.smartDesiSwaps.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Smart Swaps",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(result.smartDesiSwaps) { swap ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SuccessEmerald.copy(alpha = 0.1f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = SuccessEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = swap,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Choose Meal Slot
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Add this meal to", fontWeight = FontWeight.Bold)
                                Text(
                                    "Picked from the time of day. Tap to change.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                MealType.entries.chunked(2).forEachIndexed { rowIndex, rowTypes ->
                                    if (rowIndex > 0) Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowTypes.forEach { type ->
                                            MealSlotTile(
                                                type = type,
                                                selected = selectedMealType == type,
                                                onClick = { selectedMealType = type },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onAddToDiary(selectedMealType, result)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_scanned_meal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add ${result.estimatedCalories} kcal to ${selectedMealType.displayName}", fontWeight = FontWeight.Bold)
                        }
                    }

                    is AiScanUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Scan unsuccessful",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { launchCameraSafely() },
                                    modifier = Modifier.testTag("scan_retry_button")
                                ) {
                                    Text("Retake photo")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onAddManually,
                                    modifier = Modifier.testTag("scan_add_manually_button")
                                ) {
                                    Text("Add manually")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBox(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
    }
}


@Composable
private fun MealSlotTile(type: MealType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("meal_slot_${type.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** − ×1.5 + control in half-portion steps; 0 removes the item. */
@Composable
private fun PortionStepper(value: Double, onChange: (Double) -> Unit, allowZero: Boolean) {
    val min = if (allowZero) 0.0 else 0.5
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("portion_stepper")
    ) {
        IconButton(onClick = { onChange((value - 0.5).coerceAtLeast(min)) }, enabled = value > min, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(16.dp))
        }
        Text(
            text = if (value == 0.0) "None" else "×${value.toCompactString(1)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onChange((value + 0.5).coerceAtMost(5.0)) }, enabled = value < 5.0, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "More", modifier = Modifier.size(16.dp))
        }
    }
}

/** The scan result with each detected item (or the whole meal) scaled by its portion multiplier. */
private fun AiFoodScanResult.withPortions(portions: List<Double>, whole: Double): AiFoodScanResult {
    val baseItemKcal = detectedItems.sumOf { it.calories }
    val ratio = if (detectedItems.isNotEmpty() && baseItemKcal > 0) {
        detectedItems.indices.sumOf { detectedItems[it].calories * portions.getOrElse(it) { 1.0 } } / baseItemKcal
    } else {
        whole
    }
    if (ratio == 1.0 && portions.all { it == 1.0 }) return this
    val items = detectedItems.mapIndexed { index, item ->
        val m = portions.getOrElse(index) { 1.0 }
        item.copy(
            calories = (item.calories * m).roundToInt(),
            proteinG = item.proteinG * m,
            carbsG = item.carbsG * m,
            fatG = item.fatG * m
        )
    }
    val description = if (detectedItems.isEmpty()) {
        "$portionDescription ×${whole.toCompactString(1)}"
    } else {
        detectedItems.mapIndexedNotNull { index, item ->
            val m = portions.getOrElse(index) { 1.0 }
            when (m) {
                0.0 -> null
                1.0 -> "${item.portion} ${item.name}"
                else -> "${item.portion} ${item.name} ×${m.toCompactString(1)}"
            }
        }.joinToString(", ")
    }
    return copy(
        estimatedCalories = (estimatedCalories * ratio).roundToInt(),
        proteinG = proteinG * ratio,
        carbsG = carbsG * ratio,
        fatG = fatG * ratio,
        fiberG = fiberG * ratio,
        detectedItems = items,
        portionDescription = description
    )
}

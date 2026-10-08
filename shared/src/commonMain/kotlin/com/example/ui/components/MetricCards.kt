package com.example.ui.components

import com.example.util.toFixed
import com.example.data.sensor.StepTrackingMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatTeal
import com.example.ui.theme.ProteinViolet
import com.example.ui.theme.WaterBlue

@Composable
fun CalorieGaugeCard(
    consumedCalories: Int,
    targetCalories: Int,
    burnedCalories: Int,
    /** Daily active-calorie goal from steps and workouts. */
    burnTarget: Int,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    proteinTargetG: Int = 75,
    carbsTargetG: Int = 200,
    fatTargetG: Int = 50,
    modifier: Modifier = Modifier
) {
    val progress = (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1.2f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "calorie_progress")
    val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calorie_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CALORIE BUDGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Daily Energy Balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Goal: $targetCalories kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gauge + Stat Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Progress Dial
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(118.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(118.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 11.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress.coerceAtMost(1f) },
                        modifier = Modifier.size(118.dp),
                        color = if (consumedCalories > targetCalories) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        strokeWidth = 11.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$consumedCalories",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kcal eaten",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Stat Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$remainingCalories kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Burned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$burnedCalories / $burnTarget kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (burnedCalories.toFloat() / burnTarget.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    val netCalories = consumedCalories - burnedCalories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Net Intake",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$netCalories kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macro Bars Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroPill(
                    label = "PROTEIN",
                    currentG = proteinG,
                    targetG = proteinTargetG.toDouble(),
                    barColor = ProteinViolet,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "CARBS",
                    currentG = carbsG,
                    targetG = carbsTargetG.toDouble(),
                    barColor = CarbsAmber,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    label = "FATS",
                    currentG = fatG,
                    targetG = fatTargetG.toDouble(),
                    barColor = FatTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MacroPill(
    label: String,
    currentG: Double,
    targetG: Double,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (currentG / targetG).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${currentG.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / ${targetG.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun StepsCard(
    currentSteps: Int,
    targetSteps: Int,
    onAddSteps: (Int) -> Unit,
    trackingMode: StepTrackingMode = StepTrackingMode.MANUAL,
    onEnableTracking: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSetSteps: (Int) -> Unit = {}
) {
    var editing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (editing) {
        var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentSteps.toString()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Correct today's steps") },
            text = {
                Column {
                    Text(
                        "If the phone counted steps you didn't walk (for example in a vehicle), enter the right number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() }.take(6) },
                        label = { Text("Steps today") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.testTag("steps_edit_field")
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { text.toIntOrNull()?.let(onSetSteps); editing = false },
                    enabled = text.toIntOrNull() != null
                ) { Text("Save") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
    val progress = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
    val distanceKm = (currentSteps * 0.00076).toFixed(2)
    val activeBurn = (currentSteps * 0.04).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("steps_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = "Steps",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Walk & Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$distanceKm km • $activeBurn kcal burned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                when (trackingMode) {
                    StepTrackingMode.AUTO -> Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("steps_auto_badge")
                    ) {
                        Text("● Auto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    StepTrackingMode.NEEDS_PERMISSION -> FilledTonalButton(
                        onClick = onEnableTracking,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("enable_step_tracking_button")
                    ) {
                        Text("Auto count", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    StepTrackingMode.MANUAL -> FilledTonalButton(
                        onClick = { onAddSteps(500) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_steps_button")
                    ) {
                        Text("+500 steps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { editing = true }
                        .testTag("steps_edit")
                ) {
                    Text(
                        text = "$currentSteps steps taken",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Correct steps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Goal: $targetSteps steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WaterTrackerCard(
    waterMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (waterMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)
    val glassesDrank = waterMl / 250
    val totalGlasses = targetMl / 250

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_tracker_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WaterBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Water",
                            tint = WaterBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hydration Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$glassesDrank of $totalGlasses glasses ($waterMl / ${targetMl}ml)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Undo a mistaken tap.
                    if (waterMl > 0) {
                        androidx.compose.material3.IconButton(
                            onClick = { onAddWater(-250) },
                            modifier = Modifier.testTag("remove_water_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove 250ml water",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { onAddWater(250) },
                        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = WaterBlue.copy(alpha = 0.15f),
                            contentColor = WaterBlue
                        ),
                        modifier = Modifier.testTag("add_water_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add 250ml water"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = WaterBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (waterMl >= targetMl) "Daily water goal reached" else "${targetMl - waterMl}ml left for optimal metabolism",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (waterMl >= targetMl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+250ml tap",
                    style = MaterialTheme.typography.labelSmall,
                    color = WaterBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.example.ui.components

import com.example.util.toDisplayString
import com.example.util.toFixed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import com.example.data.model.GoalType
import com.example.ui.theme.WarningAmber
import kotlin.math.abs
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.WeightLogEntity
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WaterBlue

@Composable
fun WeightTrackerOverviewCard(
    currentWeight: Double,
    startWeight: Double,
    targetWeight: Double,
    heightCm: Double,
    weightLogs: List<WeightLogEntity>,
    onLogWeightClick: () -> Unit,
    goal: GoalType = GoalType.LOSE,
    weeklyRateKg: Double = 0.5,
    modifier: Modifier = Modifier
) {
    val change = currentWeight - startWeight
    val progressPercent = when (goal) {
        GoalType.LOSE -> (-change / (startWeight - targetWeight).coerceAtLeast(0.1) * 100)
        GoalType.GAIN -> (change / (targetWeight - startWeight).coerceAtLeast(0.1) * 100)
        GoalType.MAINTAIN -> if (abs(currentWeight - targetWeight) <= 1.0) 100.0 else 0.0
    }.toInt().coerceIn(0, 100)
    val movingRightWay = when (goal) {
        GoalType.LOSE -> change < 0
        GoalType.GAIN -> change > 0
        GoalType.MAINTAIN -> abs(currentWeight - targetWeight) <= 1.0
    }
    // Label follows what actually happened, not the goal (weight can go the other way).
    val changeLabel = when {
        change < -0.05 -> "LOST"
        change > 0.05 -> "GAINED"
        else -> "CHANGE"
    }
    val changeValue = (if (change > 0) "+" else if (change < 0) "-" else "") + (abs(change)).toFixed(1)

    val heightM = heightCm / 100.0
    val bmi = if (heightM > 0) currentWeight / (heightM * heightM) else 0.0

    // Indian BMI Classification (WHO Asian-Indian standards)
    val (bmiCategory, bmiColor) = when {
        bmi < 18.5 -> "Underweight" to WaterBlue
        bmi < 23.0 -> "Healthy" to SuccessEmerald
        bmi < 25.0 -> "Overweight" to CarbsAmber
        else -> "Obese (High Risk)" to ErrorCoral
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_tracker_overview_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WEIGHT & BMI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Body Transformation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onLogWeightClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("log_today_weight_button")
                ) {
                    Text("Log Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Modern Stat Cards (Current, Lost, Target)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeightMetricTile(
                    label = "CURRENT",
                    value = (currentWeight).toFixed(1),
                    unit = "kg",
                    icon = Icons.Default.Scale,
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                WeightMetricTile(
                    label = changeLabel,
                    value = changeValue,
                    unit = "kg",
                    icon = if (change > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    accentColor = if (movingRightWay || change == 0.0) SuccessEmerald else WarningAmber,
                    modifier = Modifier.weight(1f)
                )
                WeightMetricTile(
                    label = "TARGET",
                    value = (targetWeight).toFixed(1),
                    unit = "kg",
                    icon = Icons.Default.Flag,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Goal Milestone Progress",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val awayFromGoal = when (goal) {
                            GoalType.LOSE -> change > 0.05
                            GoalType.GAIN -> change < -0.05
                            GoalType.MAINTAIN -> false
                        }
                        Text(
                            text = if (awayFromGoal) "${abs(change).toFixed(1)} kg ${if (change > 0) "above" else "below"} start"
                            else "$progressPercent% Completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (awayFromGoal) CarbsAmber else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${(startWeight).toFixed(1)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Target: ${(targetWeight).toFixed(1)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Multi-Segment BMI Gauge
            ProfessionalBmiSpectrum(
                bmi = bmi,
                categoryName = bmiCategory,
                categoryColor = bmiColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weight Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (goal == GoalType.MAINTAIN) "Goal: stay within ±1 kg" else "Plan pace: ${if (goal == GoalType.LOSE) "-" else "+"}$weeklyRateKg kg/week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessEmerald.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (movingRightWay) "On Track" else "Keep going",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SuccessEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Professional Gradient Area Canvas Chart
            ProfessionalWeightChart(
                weightLogs = weightLogs,
                targetWeight = targetWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

@Composable
fun WeightMetricTile(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(vertical = 12.dp, horizontal = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProfessionalBmiSpectrum(
    bmi: Double,
    categoryName: String,
    categoryColor: Color
) {
    // Asian-Indian BMI spectrum: Underweight (<18.5), Normal (18.5-22.9), Overweight (23-24.9), Obese (25+)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BMI: ${(bmi).toFixed(1)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Color Segment Spectrum Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            ) {
                // Underweight (<18.5)
                Box(
                    modifier = Modifier
                        .weight(18.5f)
                        .height(8.dp)
                        .background(WaterBlue)
                )
                Spacer(modifier = Modifier.width(2.dp))
                // Normal Asian (18.5 - 22.9)
                Box(
                    modifier = Modifier
                        .weight(4.5f)
                        .height(8.dp)
                        .background(SuccessEmerald)
                )
                Spacer(modifier = Modifier.width(2.dp))
                // Overweight (23 - 24.9)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(8.dp)
                        .background(CarbsAmber)
                )
                Spacer(modifier = Modifier.width(2.dp))
                // Obese (25 - 35)
                Box(
                    modifier = Modifier
                        .weight(10f)
                        .height(8.dp)
                        .background(ErrorCoral)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Spectrum markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("< 18.5", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("18.5–22.9 (Healthy)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessEmerald)
                Text("23–24.9", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("25+ (Obese)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfessionalWeightChart(
    weightLogs: List<WeightLogEntity>,
    targetWeight: Double,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetLineColor = CarbsAmber
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            if (weightLogs.isEmpty()) return@Canvas

            val minWeight = (weightLogs.minOf { it.weightKg }.coerceAtMost(targetWeight) - 2.0).toFloat()
            val maxWeight = (weightLogs.maxOf { it.weightKg }.coerceAtLeast(targetWeight) + 2.0).toFloat()
            val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

            // 1. Draw subtle horizontal grid lines
            val gridSteps = 3
            for (i in 0..gridSteps) {
                val y = height * (i.toFloat() / gridSteps)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 2. Target Weight Reference Line (Dashed)
            val targetY = height - ((targetWeight.toFloat() - minWeight) / weightRange * height)
            drawLine(
                color = targetLineColor.copy(alpha = 0.6f),
                start = Offset(0f, targetY),
                end = Offset(width, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            // 3. Compute Coordinates with edge padding
            val paddingX = 24.dp.toPx()
            val usableWidth = width - (paddingX * 2)

            val points = weightLogs.mapIndexed { index, log ->
                val x = if (weightLogs.size > 1) {
                    paddingX + (usableWidth * (index.toFloat() / (weightLogs.size - 1).toFloat()))
                } else {
                    width / 2f
                }
                val y = height - ((log.weightKg.toFloat() - minWeight) / weightRange * height)
                Offset(x, y)
            }

            // 4. Draw Rich Gradient Area Fill Under the Curve
            val fillPath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )

            // 5. Draw Smooth Bezier Curve Stroke
            val curvePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
            }

            drawPath(
                path = curvePath,
                color = primaryColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 6. Draw Glowing Circular Nodes on each data point
            points.forEachIndexed { idx, pt ->
                val isLatest = idx == points.size - 1

                // Outer Halo
                drawCircle(
                    color = if (isLatest) primaryColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.15f),
                    radius = if (isLatest) 10.dp.toPx() else 7.dp.toPx(),
                    center = pt
                )
                // White Ring
                drawCircle(
                    color = Color.White,
                    radius = if (isLatest) 6.dp.toPx() else 4.5.dp.toPx(),
                    center = pt
                )
                // Solid Inner Core
                drawCircle(
                    color = primaryColor,
                    radius = if (isLatest) 4.dp.toPx() else 3.dp.toPx(),
                    center = pt
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clean X-Axis Date / Week Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val count = weightLogs.size
            weightLogs.forEachIndexed { index, log ->
                // Real dates of the logs, e.g. "24 Sep"; the latest is today when logged today.
                val label = if (log.date == com.example.util.todayDate().toString()) "Today"
                else runCatching { kotlinx.datetime.LocalDate.parse(log.date).toDisplayString().substringBeforeLast(' ') }.getOrDefault(log.date)
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (index == count - 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (index == count - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogWeightDialog(
    initialWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var weightText by remember { mutableStateOf((initialWeight).toFixed(1)) }
    var noteText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("log_weight_dialog")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Log Today's Weight",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "For the most accurate trend, weigh yourself in the morning before eating.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        errorText = ""
                    },
                    label = { Text("Weight (in kg)") },
                    singleLine = true,
                    isError = errorText.isNotEmpty(),
                    supportingText = if (errorText.isNotEmpty()) {
                        { Text(errorText, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("weight_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Optional, e.g. Post morning walk)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("weight_note_field")
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull()
                            if (w == null || w < 30.0 || w > 250.0) {
                                errorText = "Please enter valid weight between 30 and 250 kg"
                            } else {
                                onConfirm(w, noteText)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_weight_button")
                    ) {
                        Text("Save Weight", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WidgetConfig
import com.example.data.model.WidgetMetric

// Same palette as the Android widget, so the preview matches what lands on the home screen.
private data class WidgetPalette(
    val background: Brush,
    val text: Color,
    val secondary: Color,
    val track: Color,
    val bars: List<Color>
)

private val DarkPalette = WidgetPalette(
    background = Brush.linearGradient(listOf(Color(0xFF38342E), Color(0xFF1B1A17))),
    text = Color(0xFFF5F1EA),
    secondary = Color(0xFFB1ACA0),
    track = Color(0xFF45413A),
    bars = listOf(Color(0xFFEE8A47), Color(0xFF5A9FD4), Color(0xFF9C8BD6))
)

private val LightPalette = WidgetPalette(
    background = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF6F0E6))),
    text = Color(0xFF1F1E1B),
    secondary = Color(0xFF6B6860),
    track = Color(0xFFECE6DC),
    bars = listOf(Color(0xFFD9622B), Color(0xFF3F7CA8), Color(0xFF7E6BC4))
)

private fun WidgetMetric.icon(): ImageVector = when (this) {
    WidgetMetric.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
    WidgetMetric.WATER -> Icons.Default.WaterDrop
    WidgetMetric.PROTEIN -> Icons.Default.EggAlt
    WidgetMetric.BURNED -> Icons.Default.LocalFireDepartment
    WidgetMetric.CARBS -> Icons.Default.Grain
    WidgetMetric.FAT -> Icons.Default.Opacity
}

/** Sample numbers for the preview: value text and how full the bar is. */
private fun WidgetMetric.sample(): Pair<String, Float> = when (this) {
    WidgetMetric.STEPS -> "5,240 / 7,500" to 0.70f
    WidgetMetric.WATER -> "1.8 / 3.0 L" to 0.60f
    WidgetMetric.PROTEIN -> "68 / 120 g" to 0.57f
    WidgetMetric.BURNED -> "210 / 400 kcal" to 0.52f
    WidgetMetric.CARBS -> "140 / 230 g" to 0.61f
    WidgetMetric.FAT -> "32 / 56 g" to 0.57f
}

/** Live preview, theme tiles, ring choice and the three bars of the home screen widget. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettings(config: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    WidgetPreview(config)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "Add it from your home screen: long-press, then Widgets → FitBharat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SettingLabel("Style")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ThemeTile("Dark", DarkPalette, selected = !config.isLight, modifier = Modifier.weight(1f).testTag("widget_theme_dark")) {
            onChange(config.copy(theme = "DARK"))
        }
        ThemeTile("Light", LightPalette, selected = config.isLight, modifier = Modifier.weight(1f).testTag("widget_theme_light")) {
            onChange(config.copy(theme = "LIGHT"))
        }
    }

    SettingLabel("Ring shows")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("LEFT" to "Calories left", "EATEN" to "Calories eaten").forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = config.ring == key,
                onClick = { onChange(config.copy(ring = key)) },
                shape = SegmentedButtonDefaults.itemShape(index, 2),
                modifier = Modifier.testTag("widget_ring_${key.lowercase()}")
            ) { Text(label, fontSize = 13.sp) }
        }
    }

    SettingLabel("Bars · tap to swap in, the oldest one makes room")
    val chosen = config.metrics
    WidgetMetric.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { metric ->
                val position = chosen.indexOf(metric)
                MetricTile(
                    metric = metric,
                    order = position + 1,
                    color = if (position >= 0) (if (config.isLight) LightPalette else DarkPalette).bars[position] else null,
                    modifier = Modifier.weight(1f).testTag("widget_metric_${metric.name.lowercase()}")
                ) {
                    if (position < 0) onChange(config.copy(rows = (chosen.drop(1) + metric).map { it.name }))
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WidgetPreview(config: WidgetConfig) {
    val palette = if (config.isLight) LightPalette else DarkPalette
    val ringColor = palette.bars.first()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(palette.background)
            .border(1.dp, palette.text.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("widget_preview")
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
            Canvas(modifier = Modifier.size(88.dp)) {
                val stroke = 9.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(palette.track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * 0.58f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (config.showsEaten) "1,165" else "845", color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(if (config.showsEaten) "kcal eaten" else "kcal left", color = palette.secondary, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            config.metrics.forEachIndexed { index, metric ->
                val (value, fill) = metric.sample()
                Column {
                    Row {
                        Text(metric.label, color = palette.secondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(value, color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.track)) {
                        Box(Modifier.fillMaxWidth(fill).height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.bars[index]))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(label: String, palette: WidgetPalette, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.background)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else palette.text.copy(alpha = 0.15f)),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(palette.bars.first()))
        Spacer(Modifier.width(8.dp))
        Text(label, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (selected) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(metric: WidgetMetric, order: Int, color: Color?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val selected = color != null
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(if (selected) color!!.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, if (selected) color!!.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Icon(
            metric.icon(),
            contentDescription = null,
            tint = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            metric.label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(color!!), contentAlignment = Alignment.Center) {
                Text("$order", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

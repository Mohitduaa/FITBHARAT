package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Badge
import com.example.data.model.BadgeIcon

private fun BadgeIcon.vector(): ImageVector = when (this) {
    BadgeIcon.MEAL -> Icons.Default.Restaurant
    BadgeIcon.WORKOUT -> Icons.Default.FitnessCenter
    BadgeIcon.WEIGHT -> Icons.Default.MonitorWeight
    BadgeIcon.STREAK -> Icons.Default.LocalFireDepartment
    BadgeIcon.STEPS -> Icons.Default.DirectionsWalk
    BadgeIcon.WATER -> Icons.Default.WaterDrop
    BadgeIcon.STAR -> Icons.Default.Star
    BadgeIcon.PROGRAM -> Icons.Default.MilitaryTech
    BadgeIcon.TROPHY -> Icons.Default.EmojiEvents
}

/** Grid of achievements; earned ones are filled in the accent colour. Tap one for details. */
@Composable
fun BadgesCard(badges: List<Badge>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf<Badge?>(null) }
    open?.let { badge ->
        AlertDialog(
            onDismissRequest = { open = null },
            icon = { Icon(badge.icon.vector(), contentDescription = null) },
            title = { Text(badge.title) },
            text = { Text(if (badge.earned) "${badge.description}\n\nEarned!" else "${badge.description}\n\nProgress: ${badge.progress}") },
            confirmButton = { TextButton(onClick = { open = null }) { Text("OK") } }
        )
    }

    val earned = badges.count { it.earned }
    Card(
        modifier = modifier.fillMaxWidth().testTag("badges_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("BADGES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            Text("$earned of ${badges.size} earned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            badges.chunked(4).forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { badge ->
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { open = badge }.testTag("badge_${badge.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    badge.icon.vector(),
                                    contentDescription = badge.title,
                                    tint = if (badge.earned) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                badge.title,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                color = if (badge.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

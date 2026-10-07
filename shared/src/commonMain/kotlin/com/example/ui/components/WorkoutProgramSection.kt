package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MuscleGroup
import com.example.data.model.ProgramDay
import com.example.data.model.WorkoutProgram
import com.example.util.withCommas

private fun MuscleGroup.shortLabel() = when (this) {
    MuscleGroup.CHEST -> "Chest"
    MuscleGroup.ABS -> "Abs"
    MuscleGroup.ARMS -> "Arms"
    MuscleGroup.LEGS -> "Legs"
    MuscleGroup.BACK_SHOULDERS -> "Back"
    MuscleGroup.FULL_BODY -> "Full"
    MuscleGroup.REST -> "Rest"
}

/** The 4-week program: progress summary, then one row of day tiles per week. */
@Composable
fun WorkoutProgramSection(
    completed: Set<String>,
    weightKg: Double,
    onOpenDay: (ProgramDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val next = WorkoutProgram.nextDay(completed)
    val doneCount = completed.count { WorkoutProgram.isProgramDay(it) }

    Column(modifier = modifier.fillMaxWidth().testTag("workout_program"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "4-WEEK HOME PROGRAM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$doneCount of ${WorkoutProgram.TOTAL_DAYS} days completed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { doneCount.toFloat() / WorkoutProgram.TOTAL_DAYS },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(14.dp))
                if (next != null) {
                    Button(
                        onClick = { onOpenDay(next) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("program_continue")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (doneCount == 0) "Start Week 1 · Day 1" else "Continue: Week ${next.week} · Day ${next.day} (${next.focus.label})")
                    }
                } else {
                    Text("Program complete. Great work.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        WorkoutProgram.weeks.forEach { week ->
            val minutes = week.days.sumOf { it.durationMinutes }
            val kcal = week.days.sumOf { it.estimatedCalories(weightKg) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Week ${week.number}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Text(
                            "${kcal.withCommas()} kcal · $minutes min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        week.days.forEach { day ->
                            DayTile(
                                day = day,
                                done = day.id in completed,
                                isNext = day.id == next?.id,
                                onClick = { onOpenDay(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTile(day: ProgramDay, done: Boolean, isNext: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        done -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val content = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (isNext) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("program_day_${day.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = "Completed", tint = content, modifier = Modifier.size(16.dp))
        } else {
            Text("${day.day}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = content)
        }
        Spacer(Modifier.height(4.dp))
        Text(day.focus.shortLabel(), fontSize = 9.sp, color = content, maxLines = 1, textAlign = TextAlign.Center)
        Text("${day.exercises.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = content)
    }
}

/** Day detail: every exercise with its photo, duration and instructions, plus a start button. */
@Composable
fun ProgramDayDialog(
    day: ProgramDay,
    weightKg: Double,
    done: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    var detail by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.model.WorkoutExercise?>(null) }
    detail?.let { ExerciseDetailDialog(it, onDismiss = { detail = null }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp).testTag("program_day_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WEEK ${day.week} · DAY ${day.day}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        Text(day.focus.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${day.exercises.size} exercises · ${day.durationMinutes} min · ~${day.estimatedCalories(weightKg)} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(day.exercises) { index, exercise ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { detail = exercise }.testTag("exercise_row_$index"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseImage(exercise.imageId, Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${index + 1}. ${exercise.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${exercise.durationSeconds} sec · ${exercise.repsOrDetails}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Tap for instructions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("program_start")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (done) "Do it again" else "Start workout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** How-to screen for one exercise: animated photos (start / end position), muscles and every step. */
@Composable
fun ExerciseDetailDialog(exercise: com.example.data.model.WorkoutExercise, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp).testTag("exercise_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${exercise.durationSeconds} sec · ${exercise.repsOrDetails}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        ExerciseImage(
                            imageId = exercise.imageId,
                            modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)),
                            animate = true
                        )
                    }
                    if (exercise.muscles.isNotBlank()) {
                        item {
                            Text("MUSCLES WORKED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                            Text(exercise.muscles, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    item {
                        Text("HOW TO DO IT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    val steps = exercise.steps.ifEmpty { listOf(exercise.description) }
                    itemsIndexed(steps) { index, step ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                    if (exercise.tips.isNotBlank()) {
                        item {
                            Text("TIP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                            Text(exercise.tips, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    exerciseImageCredit(exercise.imageId)?.let { credit ->
                        item {
                            Text(
                                "Photo: $credit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import com.example.util.toCompactString
import com.example.util.withCommas
import com.example.util.toFixed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.datetime.toLocalDateTime
import com.example.util.toDisplayString
import com.example.data.model.formatClock
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserProfileEntity
import com.example.data.model.ActivityLevel
import com.example.data.model.GoalType
import com.example.data.model.NutritionPlan
import com.example.data.model.nutritionPlan
import com.example.util.plusWeeks
import com.example.util.toDisplayString
import com.example.util.todayDate

private val WeeklyPaceOptions = listOf(0.25, 0.5, 0.75)

/**
 * Profile & goal editor. Doubles as first-run setup when [isFirstSetup] is true (not dismissible).
 * The calorie plan is recalculated live from the inputs, so the user sees what they're signing up for.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditDialog(
    profile: UserProfileEntity,
    isFirstSetup: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit,
    backupMessage: String? = null,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onEnableReminders: () -> Unit = {},
    autoBackup: com.example.platform.AutoBackup? = null
) {
    var confirmRestore by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(profile.name) }
    var gender by remember { mutableStateOf(profile.gender) }
    var avatar by remember { mutableStateOf(profile.avatar) }
    var avatarPhoto by remember { mutableStateOf(profile.avatarPhoto) }
    var remindWater by remember { mutableStateOf(profile.remindWater) }
    var remindMeals by remember { mutableStateOf(profile.remindMeals) }
    var remindWalk by remember { mutableStateOf(profile.remindWalk) }
    var reminderTimes by remember { mutableStateOf(com.example.data.model.ReminderTimes.fromJson(profile.reminderTimes)) }
    var themeMode by remember { mutableStateOf(profile.themeMode) }
    var widgetConfig by remember { mutableStateOf(com.example.data.model.WidgetConfig.fromJson(profile.widgetConfig)) }
    var ageText by remember { mutableStateOf(if (isFirstSetup) "" else profile.age.toString()) }
    var heightText by remember { mutableStateOf(if (isFirstSetup) "" else profile.heightCm.toInt().toString()) }
    var currentText by remember { mutableStateOf(if (isFirstSetup) "" else formatKg(profile.currentWeightKg)) }
    var targetText by remember { mutableStateOf(if (isFirstSetup) "" else formatKg(profile.targetWeightKg)) }
    var goal by remember { mutableStateOf(GoalType.from(profile.goalType)) }
    var pace by remember { mutableStateOf(profile.weeklyRateKg) }
    var activity by remember { mutableStateOf(ActivityLevel.from(profile.activityLevel)) }
    var diet by remember { mutableStateOf(profile.dietPreference) }
    var stepGoalText by remember { mutableStateOf(profile.stepGoal.toString()) }

    val age = ageText.toIntOrNull()?.takeIf { it in 13..90 }
    val height = heightText.toDoubleOrNull()?.takeIf { it in 120.0..230.0 }
    val current = currentText.toDoubleOrNull()?.takeIf { it in 30.0..250.0 }
    val target = targetText.toDoubleOrNull()?.takeIf { it in 30.0..250.0 }
    val stepGoal = stepGoalText.toIntOrNull()?.takeIf { it in 1000..40000 }

    // Re-suggest the goal whenever the weights change; the user can still override it after.
    LaunchedEffect(current, target) {
        if (current != null && target != null) goal = GoalType.infer(current, target)
    }
    val maxPace = if (goal == GoalType.GAIN) 0.5 else 0.75
    val effectivePace = pace.coerceAtMost(maxPace)

    val goalMismatch = when {
        current == null || target == null -> null
        goal == GoalType.LOSE && target >= current -> "For weight loss, the target weight must be lower than your current weight."
        goal == GoalType.GAIN && target <= current -> "For weight gain, the target weight must be higher than your current weight."
        else -> null
    }

    val draft = if (age != null && height != null && current != null && target != null) {
        profile.copy(
            name = name.trim(),
            gender = gender,
            age = age,
            heightCm = height,
            currentWeightKg = current,
            targetWeightKg = if (goal == GoalType.MAINTAIN) current else target,
            goalType = goal.name,
            weeklyRateKg = effectivePace,
            activityLevel = activity.name,
            dietPreference = diet,
            stepGoal = stepGoal ?: profile.stepGoal,
            avatar = avatar,
            avatarPhoto = avatarPhoto,
            remindWater = remindWater,
            remindMeals = remindMeals,
            remindWalk = remindWalk,
            reminderTimes = reminderTimes.toJson(),
            widgetConfig = widgetConfig.toJson(),
            themeMode = themeMode
        )
    } else {
        null
    }
    val plan = draft?.takeIf { goalMismatch == null }?.nutritionPlan()
    val canSave = draft != null && name.isNotBlank() && stepGoal != null && goalMismatch == null

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace current data?") },
            text = { Text("Restoring a backup replaces everything currently in the app with the contents of the backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    onRestore()
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
        )
    }

    Dialog(
        onDismissRequest = { if (!isFirstSetup) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isFirstSetup,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_edit_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFirstSetup) "Welcome to FitBharat" else "Profile & Goal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFirstSetup) "Enter your details and we will build your personal calorie plan"
                            else "Update your details and the plan recalculates automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isFirstSetup) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Section("Avatar") {
                        AvatarPicker(
                            avatar = avatar,
                            avatarPhoto = avatarPhoto,
                            gender = gender,
                            onChange = { newAvatar, newPhoto ->
                                avatar = newAvatar
                                avatarPhoto = newPhoto
                            }
                        )
                    }

                    // About you
                    Section("About you") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(30) },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_field")
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("Male", "Female").forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = gender == option,
                                    onClick = { gender = option },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                                ) { Text(option) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(ageText, { ageText = it }, "Age", "years", ageText.isNotEmpty() && age == null, Modifier.weight(1f))
                            NumberField(heightText, { heightText = it }, "Height", "cm", heightText.isNotEmpty() && height == null, Modifier.weight(1f))
                        }
                    }

                    // Weight goal
                    Section("Weight goal") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(currentText, { currentText = it }, "Current weight", "kg", currentText.isNotEmpty() && current == null, Modifier.weight(1f), decimal = true)
                            NumberField(
                                if (goal == GoalType.MAINTAIN && current != null) formatKg(current) else targetText,
                                { targetText = it },
                                "Target weight",
                                "kg",
                                targetText.isNotEmpty() && target == null,
                                Modifier.weight(1f),
                                decimal = true,
                                enabled = goal != GoalType.MAINTAIN
                            )
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            GoalType.entries.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = goal == option,
                                    onClick = { goal = option },
                                    shape = SegmentedButtonDefaults.itemShape(index, GoalType.entries.size),
                                    modifier = Modifier.testTag("goal_${option.name.lowercase()}")
                                ) {
                                    Text(
                                        when (option) {
                                            GoalType.LOSE -> "Lose"
                                            GoalType.MAINTAIN -> "Maintain"
                                            GoalType.GAIN -> "Gain"
                                        },
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        if (goalMismatch != null) {
                            Text(goalMismatch, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (goal != GoalType.MAINTAIN) {
                            Text(
                                text = "Speed (per week)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val options = WeeklyPaceOptions.filter { it <= maxPace }
                                options.forEachIndexed { index, option ->
                                    SegmentedButton(
                                        selected = effectivePace == option,
                                        onClick = { pace = option },
                                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                                    ) {
                                        Text("${formatKg(option)} kg", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Activity
                    Section("Daily activity") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ActivityLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = activity == level,
                                    onClick = { activity = level },
                                    label = { Text(level.displayName) }
                                )
                            }
                        }
                        Text(
                            text = activity.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Diet & steps
                    Section("Food & steps") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "VEGETARIAN" to "Veg",
                                "NON_VEGETARIAN" to "Non-veg",
                                "EGGETARIAN" to "Egg",
                                "JAIN" to "Jain"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = diet == key,
                                    onClick = { diet = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                        NumberField(
                            stepGoalText,
                            { stepGoalText = it },
                            "Daily steps goal",
                            "steps",
                            stepGoalText.isNotEmpty() && stepGoal == null,
                            Modifier.fillMaxWidth()
                        )
                    }

                    Section("Reminders") {
                        val t = reminderTimes
                        ReminderRow(
                            "Drink water",
                            "Every ${t.waterEveryHours} h, ${formatClock(t.waterStart)} to ${formatClock(t.waterEnd)}",
                            remindWater
                        ) {
                            remindWater = it
                            if (it) onEnableReminders()
                        }
                        if (remindWater) {
                            ReminderOptions {
                                Text("Every", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                listOf(1, 2, 3).forEach { h ->
                                    androidx.compose.material3.FilterChip(
                                        selected = t.waterEveryHours == h,
                                        onClick = { reminderTimes = t.copy(waterEveryHours = h) },
                                        label = { Text("$h h") }
                                    )
                                }
                            }
                            ReminderOptions {
                                TimeChip("From", t.waterStart) { reminderTimes = reminderTimes.copy(waterStart = it) }
                                TimeChip("To", t.waterEnd) { reminderTimes = reminderTimes.copy(waterEnd = it) }
                            }
                        }
                        ReminderRow("Log meals", "${formatClock(t.lunch)} and ${formatClock(t.dinner)}", remindMeals) {
                            remindMeals = it
                            if (it) onEnableReminders()
                        }
                        if (remindMeals) {
                            ReminderOptions {
                                TimeChip("Lunch", t.lunch) { reminderTimes = reminderTimes.copy(lunch = it) }
                                TimeChip("Dinner", t.dinner) { reminderTimes = reminderTimes.copy(dinner = it) }
                            }
                        }
                        ReminderRow("Weekly report", "Sundays at 7:00 PM", t.weeklyReport) {
                            reminderTimes = reminderTimes.copy(weeklyReport = it)
                            if (it) onEnableReminders()
                        }
                        ReminderRow("Walk", formatClock(t.walk), remindWalk) {
                            remindWalk = it
                            if (it) onEnableReminders()
                        }
                        if (remindWalk) {
                            ReminderOptions {
                                TimeChip("At", t.walk) { reminderTimes = reminderTimes.copy(walk = it) }
                            }
                        }
                    }

                    Section("Appearance") {
                        ThemeModeToggle(themeMode) { themeMode = it }
                    }

                    Section("Home screen widget") {
                        WidgetSettings(widgetConfig) { widgetConfig = it }
                    }

                    // Backup & restore
                    Section(if (isFirstSetup) "Already use FitBharat?" else "Backup & restore") {
                        Text(
                            text = if (isFirstSetup) {
                                "Moving from another phone? Restore your data from a backup file."
                            } else {
                                "Save your data to Google Drive, Files or any app, and restore it on a new phone."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!isFirstSetup) {
                                OutlinedButton(
                                    onClick = onBackup,
                                    modifier = Modifier.weight(1f).testTag("backup_export_button")
                                ) { Text("Back up") }
                            }
                            OutlinedButton(
                                onClick = { if (isFirstSetup) onRestore() else confirmRestore = true },
                                modifier = Modifier.weight(1f).testTag("backup_restore_button")
                            ) { Text("Restore") }
                        }
                        if (!isFirstSetup && autoBackup != null) DailyBackupRow(autoBackup)
                        if (backupMessage != null) {
                            Text(
                                text = backupMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("backup_message")
                            )
                        }
                    }

                    // Live plan
                    PlanPreview(plan = plan, goal = goal, current = current, target = target)
                }

                // Footer
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { draft?.let(onSave) },
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Text(
                            text = if (isFirstSetup) "Create my plan" else "Save plan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val allowed = if (decimal) input.filter { it.isDigit() || it == '.' } else input.filter { it.isDigit() }
            onValueChange(allowed.take(6))
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = isError,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun PlanPreview(plan: NutritionPlan?, goal: GoalType, current: Double?, target: Double?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(18.dp)
            .testTag("plan_preview"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "YOUR DAILY PLAN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (plan == null) {
            Text(
                text = "Enter your age, height and weight to see your plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = plan.dailyCalories.withCommas(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "kcal / day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        val adjustmentText = when {
            plan.dailyAdjustment < 0 -> "${-plan.dailyAdjustment} kcal deficit"
            plan.dailyAdjustment > 0 -> "${plan.dailyAdjustment} kcal surplus"
            else -> "no deficit / surplus"
        }
        Text(
            text = "Maintenance ${plan.maintenanceCalories.withCommas()} kcal • $adjustmentText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MacroPill("Protein", "${plan.proteinG} g", Modifier.weight(1f))
            MacroPill("Carbs", "${plan.carbsG} g", Modifier.weight(1f))
            MacroPill("Fat", "${plan.fatG} g", Modifier.weight(1f))
        }
        Text(
            text = "Water: ${(plan.waterMl / 1000.0).toFixed(1)} L / day",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        val weeks = plan.weeksToGoal
        if (weeks != null && current != null && target != null) {
            val dateText = todayDate().plusWeeks(weeks).toDisplayString()
            Text(
                text = "${formatKg(target)} kg by ~$dateText ($weeks weeks)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (plan.wasCappedBySafetyFloor) {
            Text(
                text = "Note: this pace would fall below a safe calorie level, so your plan is set to the recommended minimum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatKg(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toCompactString(2)

@Composable
private fun ReminderRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag("reminder_${title.lowercase().replace(' ', '_')}")
        )
    }
}

@Composable
private fun ReminderOptions(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp),
        content = content
    )
}

/** A chip showing a time; tapping it opens a clock to change it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimeChip(label: String, minutes: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.material3.AssistChip(
        onClick = { open = true },
        label = { Text("$label  ${formatClock(minutes)}") },
        leadingIcon = {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = Modifier.testTag("time_chip_${label.lowercase()}")
    )
    if (open) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = minutes / 60,
            initialMinute = minutes % 60,
            is24Hour = false
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onChange(state.hour * 60 + state.minute)
                    open = false
                }) { Text("Set") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { open = false }) { Text("Cancel") } }
        )
    }
}


/** System / Light / Dark as one segmented toggle with icons. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeToggle(mode: String, onChange: (String) -> Unit) {
    val options = listOf(
        Triple("SYSTEM", "System", Icons.Default.BrightnessAuto),
        Triple("LIGHT", "Light", Icons.Default.LightMode),
        Triple("DARK", "Dark", Icons.Default.DarkMode)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (key, label, icon) ->
            SegmentedButton(
                selected = mode == key,
                onClick = { onChange(key) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("theme_${key.lowercase()}")
            ) { Text(label, fontSize = 13.sp) }
        }
    }
}

/** "Daily backup" switch with where the files go and when the last one was written. */
@Composable
private fun DailyBackupRow(autoBackup: com.example.platform.AutoBackup) {
    val state by autoBackup.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Saves a backup file every day automatically and keeps the last ${com.example.platform.AutoBackup.KEEP} days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Switch(
                checked = state.enabled,
                onCheckedChange = { autoBackup.setEnabled(it) },
                modifier = Modifier.testTag("daily_backup_switch")
            )
        }
        if (state.enabled) {
            state.location?.let {
                Text("Saved to: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                state.lastBackupMillis?.let { "Last backup: ${formatBackupTime(it)}" } ?: "No backup yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextButton(onClick = { autoBackup.backupNow() }) { Text("Back up now") }
                if (state.canChangeFolder) {
                    androidx.compose.material3.TextButton(onClick = { autoBackup.changeFolder() }) { Text("Change folder") }
                }
            }
        }
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatBackupTime(millis: Long): String {
    val dateTime = kotlin.time.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val hour12 = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "${dateTime.date.toDisplayString()}, $hour12:$minute ${if (dateTime.hour < 12) "AM" else "PM"}"
}

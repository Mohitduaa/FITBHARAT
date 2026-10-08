package com.example.ui.screens

import com.example.ui.components.icon
import com.example.util.toFixed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf
import com.example.data.model.MealItem
import com.example.data.model.parseMealItems
import com.example.data.model.toJson
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MealEntity
import com.example.data.model.MealType
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatTeal
import com.example.ui.theme.ProteinPurple

@Composable
fun DietScreen(
    meals: List<MealEntity>,
    targetCalories: Int,
    onOpenAddMeal: (MealType) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onOpenAiScan: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateMeal: (MealEntity) -> Unit = {},
    aiPlanSlot: @Composable () -> Unit = {}
) {
    var editing by remember { mutableStateOf<MealEntity?>(null) }
    editing?.let { meal ->
        EditMealDialog(
            meal = meal,
            onDismiss = { editing = null },
            onSave = { onUpdateMeal(it); editing = null },
            onDelete = { onDeleteMeal(meal.id); editing = null },
            onAddMore = { editing = null; onOpenAddMeal(it) }
        )
    }

    val totalCalories = remember(meals) { meals.sumOf { it.calories } }
    val totalProtein = remember(meals) { meals.sumOf { it.proteinG } }
    val totalCarbs = remember(meals) { meals.sumOf { it.carbsG } }
    val totalFat = remember(meals) { meals.sumOf { it.fatG } }

    val mealsByType = remember(meals) {
        meals.groupBy {
            try {
                MealType.valueOf(it.mealType)
            } catch (e: Exception) {
                MealType.LUNCH
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("diet_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Summary Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Indian Food Diary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Total Intake: $totalCalories / $targetCalories kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onOpenAiScan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("diet_scan_food_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Food", fontSize = 12.sp)
                }
            }
        }

        // Daily Macro Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DietMacroStat("Calories", "$totalCalories kcal", MaterialTheme.colorScheme.primary)
                    DietMacroStat("Protein", "${(totalProtein).toFixed(1)}g", ProteinPurple)
                    DietMacroStat("Carbs", "${(totalCarbs).toFixed(1)}g", CarbsAmber)
                    DietMacroStat("Fat", "${(totalFat).toFixed(1)}g", FatTeal)
                }
            }
        }

        item { aiPlanSlot() }

        // Meal Sections
        items(MealType.values()) { mealType ->
            val slotMeals = mealsByType[mealType] ?: emptyList()
            val slotCalories = slotMeals.sumOf { it.calories }

            MealSlotCard(
                mealType = mealType,
                meals = slotMeals,
                slotCalories = slotCalories,
                onAddClick = { onOpenAddMeal(mealType) },
                onDeleteMeal = onDeleteMeal,
                onEditMeal = { editing = it }
            )
        }
    }
}

@Composable
fun DietMacroStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MealSlotCard(
    mealType: MealType,
    meals: List<MealEntity>,
    slotCalories: Int,
    onAddClick: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onEditMeal: (MealEntity) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meal_slot_${mealType.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(mealType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mealType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$slotCalories kcal logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_to_${mealType.name.lowercase()}_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Food")
                }
            }

            if (meals.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing logged yet. Tap + to add a meal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meals.forEach { meal ->
                        LoggedFoodItemRow(meal = meal, onDelete = { onDeleteMeal(meal.id) }, onEdit = { onEditMeal(meal) })
                    }
                }
            }
        }
    }
}

@Composable
fun LoggedFoodItemRow(
    meal: MealEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
            .testTag("meal_row_${meal.id}")
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meal.foodName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${meal.unit} • ${meal.calories} kcal • ${meal.proteinG.toFixed(1)} g protein",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(28.dp).testTag("edit_meal_${meal.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp).testTag("delete_meal_${meal.id}")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Change a logged meal: adjust each food inside it (when the meal was scanned with its items),
 * or the whole portion, move it to another meal slot, or add another food to that slot.
 */
@Composable
fun EditMealDialog(
    meal: MealEntity,
    onDismiss: () -> Unit,
    onSave: (MealEntity) -> Unit,
    onDelete: () -> Unit,
    onAddMore: (MealType) -> Unit = {}
) {
    val baseItems = remember(meal.id) { parseMealItems(meal.itemsJson).ifEmpty { guessMealItems(meal) } }
    val counts = remember(meal.id) { mutableStateListOf(*baseItems.map { it.count }.toTypedArray()) }
    var portion by remember(meal.id) { mutableDoubleStateOf(1.0) }
    var slot by remember(meal.id) {
        mutableStateOf(runCatching { MealType.valueOf(meal.mealType) }.getOrDefault(MealType.LUNCH))
    }

    val items = baseItems.mapIndexed { i, item -> item.copy(count = counts[i]) }
    val kcal: Int
    val protein: Double
    val carbs: Double
    val fat: Double
    if (items.isNotEmpty()) {
        kcal = items.sumOf { (it.calories * it.count).roundToInt() }
        protein = items.sumOf { it.proteinG * it.count }
        carbs = items.sumOf { it.carbsG * it.count }
        fat = items.sumOf { it.fatG * it.count }
    } else {
        kcal = (meal.calories * portion).roundToInt()
        protein = meal.proteinG * portion
        carbs = meal.carbsG * portion
        fat = meal.fatG * portion
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit meal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(meal.foodName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                if (items.isNotEmpty()) {
                    Text("Items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    items.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.portion} · ${(item.calories * item.count).roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PortionStepper(value = item.count, min = 0.0, onChange = { counts[index] = it }, tag = "item_$index")
                        }
                    }
                } else {
                    Text("Portion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PortionStepper(value = portion, min = 0.25, onChange = { portion = it }, tag = "portion")
                }

                Text(
                    "$kcal kcal · ${protein.toFixed(1)} g protein",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("Meal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MealType.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { type ->
                            FilterChip(
                                selected = slot == type,
                                onClick = { slot = type },
                                label = { Text(type.displayName, maxLines = 1) },
                                leadingIcon = { Icon(type.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { onAddMore(slot) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_meal_add_more")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add another food to ${slot.displayName}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kept = items.filter { it.count > 0 }
                    onSave(
                        meal.copy(
                            mealType = slot.name,
                            quantity = if (items.isEmpty()) meal.quantity * portion else meal.quantity,
                            unit = if (items.isEmpty()) meal.unit else kept.joinToString(", ") { it.label() },
                            calories = kcal,
                            proteinG = protein,
                            carbsG = carbs,
                            fatG = fat,
                            fiberG = if (items.isEmpty()) meal.fiberG * portion else meal.fiberG,
                            itemsJson = kept.toJson()
                        )
                    )
                },
                enabled = kcal > 0,
                modifier = Modifier.testTag("edit_meal_save")
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * Older scanned meals were saved without their items, only a description like
 * "1 piece Roti, 1 katori Dal ×0.5". Split that back into items, sharing the meal's totals in
 * proportion to typical calories from the food database. Returns empty when it can't be matched.
 */
private fun guessMealItems(meal: MealEntity): List<MealItem> {
    val parts = meal.unit.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return emptyList()
    val countRegex = Regex("""\s*[×x]\s*([0-9.]+)\s*$""")
    data class Part(val portion: String, val name: String, val count: Double, val weight: Double)
    val parsed = parts.map { raw ->
        val count = countRegex.find(raw)?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
        val text = raw.replace(countRegex, "")
        val words = text.split(" ").filter { it.isNotBlank() }
        val name = words.lastOrNull() ?: text
        val portion = words.dropLast(1).joinToString(" ")
        val preset = com.example.data.model.IndianFoodDatabase.items.firstOrNull { it.name.contains(name, ignoreCase = true) }
            ?: return emptyList()
        Part(portion, name, count, preset.calories * count)
    }
    val total = parsed.sumOf { it.weight }.takeIf { it > 0 } ?: return emptyList()
    return parsed.map { p ->
        val share = p.weight / total / p.count
        MealItem(
            name = p.name,
            portion = p.portion,
            calories = (meal.calories * share).roundToInt(),
            proteinG = meal.proteinG * share,
            carbsG = meal.carbsG * share,
            fatG = meal.fatG * share,
            count = p.count
        )
    }
}

private fun Double.compact(): String = toFixed(2).trimEnd('0').trimEnd('.')

private fun MealItem.label(): String = if (count == 1.0) "$portion $name" else "$portion $name ×${count.compact()}"

@Composable
private fun PortionStepper(value: Double, min: Double, onChange: (Double) -> Unit, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(
            onClick = { onChange((value - 0.5).coerceAtLeast(min)) },
            modifier = Modifier.size(34.dp).testTag("${tag}_minus")
        ) { Icon(Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(18.dp)) }
        Text(
            "×${value.compact()}",
            modifier = Modifier.width(52.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedIconButton(
            onClick = { onChange((value + 0.5).coerceAtMost(10.0)) },
            modifier = Modifier.size(34.dp).testTag("${tag}_plus")
        ) { Icon(Icons.Default.Add, contentDescription = "More", modifier = Modifier.size(18.dp)) }
    }
}

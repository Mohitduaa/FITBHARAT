package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.QrCodeScanner
import com.example.util.toFixed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IndianFoodDatabase
import com.example.data.model.IndianFoodPreset
import com.example.data.model.MealType

@Composable
fun AddMealDialog(
    mealType: MealType,
    onDismiss: () -> Unit,
    onAddMeal: (String, Double, String, Int, Double, Double, Double, Double) -> Unit,
    recentFoods: List<com.example.data.local.MealEntity> = emptyList(),
    onAddRecent: (com.example.data.local.MealEntity) -> Unit = {},
    barcodeAvailable: Boolean = false,
    onScanBarcode: () -> Unit = {},
    /** Vegetarian users only see vegetarian foods. */
    vegOnly: Boolean = false
) {
    // 0: recent foods, 1: food database, 2: custom food
    var selectedTab by remember { mutableIntStateOf(if (recentFoods.isNotEmpty()) 0 else 1) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Selected Preset state
    var selectedPreset by remember { mutableStateOf<IndianFoodPreset?>(null) }
    var quantityMultiplier by remember { mutableDoubleStateOf(1.0) }

    // Custom Entry state
    var customName by remember { mutableStateOf("") }
    var customCalories by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var customPortion by remember { mutableStateOf("1 serving") }

    val categories = listOf("All", "Breads", "Dals & Lentils", "Rice & Khichdi", "Paneer & Dairy", "Breakfast", "Snacks & Salads", "Eggs & Non-Veg", "Beverages")
        .filter { !vegOnly || it != "Eggs & Non-Veg" }

    val filteredPresets = remember(searchQuery, selectedCategory, vegOnly) {
        IndianFoodDatabase.items.filter { item -> !vegOnly || item.isVeg }.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.hindiName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("add_meal_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add to ${mealType.displayName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Indian Food Tracker & Calorie Counter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_meal_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Recent") },
                        modifier = Modifier.testTag("tab_recent_foods")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Database") },
                        modifier = Modifier.testTag("tab_desi_database")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Custom") },
                        modifier = Modifier.testTag("tab_custom_food")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    RecentFoodsList(
                        foods = recentFoods,
                        mealType = mealType,
                        onAdd = onAddRecent,
                        modifier = Modifier.weight(1f)
                    )
                } else if (selectedTab == 1) {
                    // Search & Filters
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search roti, dal, paneer...", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (barcodeAvailable) {
                                IconButton(onClick = onScanBarcode, modifier = Modifier.testTag("scan_barcode_button")) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset list or selection preview
                    if (selectedPreset == null) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredPresets) { preset ->
                                FoodPresetRow(
                                    preset = preset,
                                    onSelect = {
                                        selectedPreset = preset
                                        quantityMultiplier = 1.0
                                    }
                                )
                            }
                        }
                    } else {
                        // Selected item portion customizer
                        val p = selectedPreset!!
                        val totalCal = (p.calories * quantityMultiplier).toInt()
                        val totalProt = p.proteinG * quantityMultiplier
                        val totalCarbs = p.carbsG * quantityMultiplier
                        val totalFat = p.fatG * quantityMultiplier

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = p.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = p.hindiName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { selectedPreset = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Base serving: ${p.servingSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (p.healthTip.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${p.healthTip}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Select Portion / Quantity:", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0.5 to "½x", 1.0 to "1x", 1.5 to "1.5x", 2.0 to "2x").forEach { (factor, label) ->
                                    FilterChip(
                                        selected = quantityMultiplier == factor,
                                        onClick = { quantityMultiplier = factor },
                                        label = {
                                            Text(label, fontSize = 13.sp, maxLines = 1, softWrap = false, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Total nutrients preview
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Calories", style = MaterialTheme.typography.labelSmall)
                                    Text("$totalCal kcal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Protein", style = MaterialTheme.typography.labelSmall)
                                    Text("${(totalProt).toFixed(1)}g", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Carbs", style = MaterialTheme.typography.labelSmall)
                                    Text("${(totalCarbs).toFixed(1)}g", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fat", style = MaterialTheme.typography.labelSmall)
                                    Text("${(totalFat).toFixed(1)}g", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    onAddMeal(
                                        p.name,
                                        quantityMultiplier,
                                        p.servingSize,
                                        totalCal,
                                        totalProt,
                                        totalCarbs,
                                        totalFat,
                                        p.fiberG * quantityMultiplier
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_add_preset_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add $totalCal kcal to ${mealType.displayName}")
                            }
                        }
                    }
                } else {
                    // Custom Food Input
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Food / Dish Name (e.g. Oats with Almond Milk)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_food_name_input")
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = customPortion,
                                onValueChange = { customPortion = it },
                                label = { Text("Portion Size (e.g. 1 bowl, 2 pieces)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = customCalories,
                                onValueChange = { customCalories = it },
                                label = { Text("Calories (kcal) *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("custom_calories_input")
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customProtein,
                                    onValueChange = { customProtein = it },
                                    label = { Text("Protein (g)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customCarbs,
                                    onValueChange = { customCarbs = it },
                                    label = { Text("Carbs (g)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customFat,
                                    onValueChange = { customFat = it },
                                    label = { Text("Fat (g)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val cal = customCalories.toIntOrNull() ?: 100
                                    val prot = customProtein.toDoubleOrNull() ?: 5.0
                                    val carbs = customCarbs.toDoubleOrNull() ?: 15.0
                                    val fat = customFat.toDoubleOrNull() ?: 3.0
                                    val name = customName.ifBlank { "Custom Meal" }
                                    onAddMeal(name, 1.0, customPortion, cal, prot, carbs, fat, 2.0)
                                },
                                shape = RoundedCornerShape(14.dp),
                                enabled = customName.isNotBlank() && customCalories.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_add_custom_button")
                            ) {
                                Text("Add Custom Food to ${mealType.displayName}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodPresetRow(
    preset: IndianFoodPreset,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("food_preset_${preset.name.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (preset.isVeg) com.example.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error)
                    )
                }
                Text(
                    text = "${preset.servingSize} • ${preset.proteinG}g protein",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${preset.calories} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** Foods the user logged before; one tap adds the same portion again. */
@Composable
private fun RecentFoodsList(
    foods: List<com.example.data.local.MealEntity>,
    mealType: MealType,
    onAdd: (com.example.data.local.MealEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (foods.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Foods you log will appear here for quick re-adding.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(foods) { food ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("recent_food_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.foodName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        "${food.calories} kcal · ${food.proteinG.toInt()} g protein · ${food.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onAdd(food) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add to ${mealType.displayName}", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
    }
}

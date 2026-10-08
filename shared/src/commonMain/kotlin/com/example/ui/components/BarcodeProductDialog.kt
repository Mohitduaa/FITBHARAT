package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MealType
import com.example.data.remote.FoodProduct
import com.example.ui.viewmodel.BarcodeUiState
import com.example.util.toCompactString
import kotlin.math.roundToInt

/** Result of a barcode scan: loading, the product with a portion picker, or an error. */
@Composable
fun BarcodeProductDialog(
    state: BarcodeUiState,
    initialMealType: MealType,
    onAdd: (product: FoodProduct, grams: Double, mealType: MealType) -> Unit,
    onScanAgain: () -> Unit,
    onDismiss: () -> Unit,
    aiAvailable: Boolean = false,
    onEstimateWithAi: () -> Unit = {}
) {
    if (state is BarcodeUiState.Idle) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).testTag("barcode_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Packaged food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                Spacer(Modifier.height(8.dp))
                when (state) {
                    is BarcodeUiState.Loading -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Looking up ${state.barcode}...", style = MaterialTheme.typography.bodyMedium)
                    }

                    is BarcodeUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                    }

                    is BarcodeUiState.Found -> ProductPortion(
                        state.product, initialMealType, onAdd,
                        aiAvailable = aiAvailable,
                        estimating = state.estimating,
                        estimateFailed = state.estimateFailed,
                        onEstimateWithAi = onEstimateWithAi
                    )
                    BarcodeUiState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProductPortion(
    product: FoodProduct,
    initialMealType: MealType,
    onAdd: (FoodProduct, Double, MealType) -> Unit,
    aiAvailable: Boolean,
    estimating: Boolean,
    estimateFailed: Boolean,
    onEstimateWithAi: () -> Unit
) {
    var gramsText by remember(product) { mutableStateOf((product.servingGrams?.takeIf { it >= 15 } ?: 100.0).toCompactString(0)) }
    var mealType by remember { mutableStateOf(initialMealType) }
    // Many Indian products are in the database without nutrition; let the user copy it from the pack.
    var kcalText by remember(product) { mutableStateOf("") }
    val enteredKcal = kcalText.toDoubleOrNull()?.takeIf { it in 1.0..1000.0 }
    val product = if (product.hasCalories) product else product.copy(kcalPer100g = enteredKcal ?: 0.0)
    val grams = gramsText.toDoubleOrNull()?.takeIf { it in 1.0..3000.0 }
    val factor = (grams ?: 0.0) / 100.0
    val canAdd = grams != null && product.kcalPer100g > 0

    Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    if (product.brand.isNotBlank()) {
        Text(product.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (!product.hasCalories) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Calories for this product aren't in the database yet. Type them from the nutrition label on the pack (per 100 g).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = kcalText,
            onValueChange = { kcalText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
            label = { Text("Calories per 100 g") },
            suffix = { Text("kcal") },
            isError = kcalText.isNotEmpty() && enteredKcal == null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("barcode_kcal")
        )
        if (aiAvailable) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEstimateWithAi,
                enabled = !estimating,
                modifier = Modifier.fillMaxWidth().testTag("barcode_ai_estimate")
            ) {
                if (estimating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Estimating...")
                } else {
                    Text("Estimate with AI")
                }
            }
            if (estimateFailed) {
                Text(
                    "AI could not estimate this product. Please enter the calories from the pack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    } else Text(
        "Per 100 g: ${product.kcalPer100g.roundToInt()} kcal · P ${product.proteinPer100g.toCompactString(1)} g · C ${product.carbsPer100g.toCompactString(1)} g · F ${product.fatPer100g.toCompactString(1)} g",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (product.aiEstimated) {
        Text(
            "AI estimate from the product name. Check it against the nutrition label on the pack.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = gramsText,
        onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
        label = { Text("Amount") },
        suffix = { Text("g") },
        isError = gramsText.isNotEmpty() && grams == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("barcode_grams")
    )
    product.servingGrams?.takeIf { it >= 15 }?.let {
        Text("1 serving = ${it.toCompactString(0)} g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(12.dp))
    Text("In ${grams?.toCompactString(0) ?: "0"} g you get:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PortionStat("kcal", (product.kcalPer100g * factor).roundToInt().toString(), Modifier.weight(1f))
        PortionStat("Protein", "${(product.proteinPer100g * factor).toCompactString(1)} g", Modifier.weight(1f))
        PortionStat("Carbs", "${(product.carbsPer100g * factor).toCompactString(1)} g", Modifier.weight(1f))
        PortionStat("Fat", "${(product.fatPer100g * factor).toCompactString(1)} g", Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    MealType.entries.chunked(2).forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { type ->
                val selected = type == mealType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { mealType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        type.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { grams?.let { onAdd(product, it, mealType) } },
        enabled = canAdd,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("barcode_add")
    ) {
        Text("Add ${(product.kcalPer100g * factor).roundToInt()} kcal to ${mealType.displayName}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PortionStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

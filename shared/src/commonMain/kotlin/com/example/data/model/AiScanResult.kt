package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One food inside a logged meal. Nutrition values are for one portion; [count] is how many were eaten. */
@Serializable
data class MealItem(
    val name: String,
    val portion: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val count: Double = 1.0
)

private val mealItemJson = Json { ignoreUnknownKeys = true }

fun List<MealItem>.toJson(): String = if (isEmpty()) "" else mealItemJson.encodeToString(this)

fun parseMealItems(json: String): List<MealItem> =
    if (json.isBlank()) emptyList() else runCatching { mealItemJson.decodeFromString<List<MealItem>>(json) }.getOrDefault(emptyList())

data class DetectedItem(
    val name: String,
    val portion: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

data class AiFoodScanResult(
    val dishName: String,
    val estimatedCalories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val portionDescription: String,
    val detectedItems: List<DetectedItem>,
    val ingredients: List<String> = emptyList(),
    val desiHealthScore: Int, // 1 to 10
    val desiCoachFeedback: String,
    val smartDesiSwaps: List<String>
)

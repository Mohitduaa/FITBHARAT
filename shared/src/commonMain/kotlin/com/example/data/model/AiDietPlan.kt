package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AiPlanFood(
    val name: String,
    val portion: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

@Serializable
data class AiPlanMeal(
    /** One of [MealType] names. */
    val mealType: String,
    val title: String,
    val foods: List<AiPlanFood>,
    val note: String = ""
) {
    val calories: Int get() = foods.sumOf { it.calories }
    val proteinG: Double get() = foods.sumOf { it.proteinG }
    val type: MealType get() = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.SNACKS
}

/** A one-day Indian meal plan Gemini made for the user's body and goal. */
@Serializable
data class AiDietPlan(
    /** Why this plan fits the user: BMI, goal, calories, protein. */
    val summary: String,
    val meals: List<AiPlanMeal>,
    val tips: List<String> = emptyList(),
    /** yyyy-MM-dd it was made, and the profile numbers it was made for. */
    val createdOn: String = "",
    val calorieTarget: Int = 0,
    val dietPreference: String = "",
    val weightKg: Double = 0.0
) {
    val totalCalories: Int get() = meals.sumOf { it.calories }
    val totalProtein: Double get() = meals.sumOf { it.proteinG }

    fun toJson(): String = json.encodeToString(this)

    /** The same plan in the shape the Home meal cards show. */
    fun toDailyPlan(stepsGoal: Int, waterGoalMl: Int): DailyDesiPlan = DailyDesiPlan(
        targetCalories = calorieTarget,
        stepsGoal = stepsGoal,
        waterGoalMl = waterGoalMl,
        morningTip = tips.firstOrNull() ?: summary,
        eveningMindsetTip = tips.getOrNull(1) ?: "",
        meals = meals.sortedBy { it.type.ordinal }.map { meal ->
            PlannedMeal(
                mealType = meal.type,
                title = meal.title,
                hindiTitle = "AI plan",
                itemsSummary = meal.foods.joinToString(" + ") { "${it.name} (${it.portion})" },
                calories = meal.calories,
                proteinG = meal.proteinG,
                carbsG = meal.foods.sumOf { it.carbsG },
                fatG = meal.foods.sumOf { it.fatG },
                foodList = meal.foods.map { FoodEntryItem(it.name, 1.0, it.portion, it.calories, it.proteinG, it.carbsG, it.fatG) },
                desiCoachNote = meal.note
            )
        }
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): AiDietPlan? =
            if (text.isBlank()) null else runCatching { json.decodeFromString<AiDietPlan>(text) }.getOrNull()
    }
}

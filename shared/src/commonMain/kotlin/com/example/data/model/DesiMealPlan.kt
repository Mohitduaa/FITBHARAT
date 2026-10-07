package com.example.data.model

import com.example.util.toFixed

data class PlannedMeal(
    val mealType: MealType,
    val title: String,
    val hindiTitle: String,
    val itemsSummary: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val foodList: List<FoodEntryItem>,
    val desiCoachNote: String
)

data class FoodEntryItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

data class DailyDesiPlan(
    val targetCalories: Int,
    val stepsGoal: Int,
    val waterGoalMl: Int,
    val morningTip: String,
    val eveningMindsetTip: String,
    val meals: List<PlannedMeal>
)

object DesiDietPlanGenerator {
    fun getPlan(dietPref: DietPreference, calorieTarget: Int): DailyDesiPlan {
        return when (dietPref) {
            DietPreference.NON_VEGETARIAN, DietPreference.EGGETARIAN -> {
                DailyDesiPlan(
                    targetCalories = calorieTarget,
                    stepsGoal = 7500,
                    waterGoalMl = 2500,
                    morningTip = "Subah khali pet 1 bada glass gunguna paani ya jeera water piyein. Metabolism activate hoga!",
                    eveningMindsetTip = "Raat ko 8:30 baje se pehle dinner complete karein, digestion aur deep sleep dono behtar hongi.",
                    meals = listOf(
                        PlannedMeal(
                            mealType = MealType.BREAKFAST,
                            title = "2 Boiled Eggs / Bhurji + 1 Roti / Multigrain Toast",
                            hindiTitle = "Andaa Bhurji aur Phulka",
                            itemsSummary = "2 Eggs + 1 Roti + 1 cup Black Coffee/Chai (Kam Cheeni)",
                            calories = 340,
                            proteinG = 18.0,
                            carbsG = 24.0,
                            fatG = 14.0,
                            foodList = listOf(
                                FoodEntryItem("Egg Bhurji (2 Eggs)", 1.0, "plate", 185, 13.5, 4.0, 12.0),
                                FoodEntryItem("Roti (Phulka)", 1.0, "roti", 85, 3.0, 17.5, 0.4),
                                FoodEntryItem("Masala Chai (Less Sugar)", 1.0, "cup", 65, 2.0, 9.0, 2.2)
                            ),
                            desiCoachNote = "Eggs keep your ghrelin (hunger hormone) suppressed until 1 PM!"
                        ),
                        PlannedMeal(
                            mealType = MealType.LUNCH,
                            title = "2 Phulka + 1 Katori Dal Tadka + Bhindi Sabzi + Salad",
                            hindiTitle = "2 Roti, Dal, Sabzi aur Kachumber",
                            itemsSummary = "2 Rotis + 1 bowl Yellow Dal + 1 bowl Bhindi + Cucumber Salad",
                            calories = 520,
                            proteinG = 18.5,
                            carbsG = 78.0,
                            fatG = 12.5,
                            foodList = listOf(
                                FoodEntryItem("Roti (Phulka - No Ghee)", 2.0, "roti", 170, 6.0, 35.0, 0.8),
                                FoodEntryItem("Dal Tadka", 1.0, "katori", 140, 7.5, 18.0, 4.0),
                                FoodEntryItem("Bhindi Masala", 1.0, "katori", 110, 2.5, 10.0, 6.5),
                                FoodEntryItem("Cucumber Tomato Salad", 1.0, "plate", 45, 1.5, 8.5, 0.4),
                                FoodEntryItem("Chaas / Buttermilk", 1.0, "glass", 45, 2.8, 4.0, 1.8)
                            ),
                            desiCoachNote = "Eat the kachumber salad FIRST before touching the roti. Natural fiber mesh slows carb absorption."
                        ),
                        PlannedMeal(
                            mealType = MealType.SNACKS,
                            title = "Roasted Makhana / Bhuna Chana + Green Tea",
                            hindiTitle = "Bhuna Chana aur Green Tea",
                            itemsSummary = "1 handful roasted chana (30g) + Lemon green tea",
                            calories = 120,
                            proteinG = 6.2,
                            carbsG = 18.0,
                            fatG = 2.0,
                            foodList = listOf(
                                FoodEntryItem("Roasted Chana", 1.0, "handful (30g)", 115, 6.0, 17.0, 2.0),
                                FoodEntryItem("Green Tea", 1.0, "cup", 5, 0.2, 0.8, 0.0)
                            ),
                            desiCoachNote = "Say strict NO to fried samosa/pakora. This crunchy snack beats the 5 PM slump without oil."
                        ),
                        PlannedMeal(
                            mealType = MealType.DINNER,
                            title = "Tandoori Chicken / Paneer + Mixed Veg Salad + Clear Soup",
                            hindiTitle = "Grilled Chicken / Paneer Salad",
                            itemsSummary = "150g grilled chicken/paneer + stir fried veggies + 1/2 katori rice or 1 roti",
                            calories = 480,
                            proteinG = 34.0,
                            carbsG = 32.0,
                            fatG = 16.0,
                            foodList = listOf(
                                FoodEntryItem("Tandoori Chicken / Paneer", 1.0, "portion", 220, 30.0, 4.0, 8.0),
                                FoodEntryItem("Roti (Phulka)", 1.0, "roti", 85, 3.0, 17.5, 0.4),
                                FoodEntryItem("Mixed Veg Soup / Salad", 1.0, "bowl", 95, 3.0, 15.0, 2.0)
                            ),
                            desiCoachNote = "High protein dinner repairs muscle fibers during overnight recovery and keeps cravings away."
                        )
                    )
                )
            }
            else -> {
                // Pure Vegetarian & Jain friendly
                DailyDesiPlan(
                    targetCalories = calorieTarget,
                    stepsGoal = 7500,
                    waterGoalMl = 2500,
                    morningTip = "Start with a glass of warm water with a dash of lemon or soaked methi dana water.",
                    eveningMindsetTip = "Din ka aakhri khana hamesha halka rakhein. Khane ke baad 15-min walk se digestion fast hota hai.",
                    meals = listOf(
                        PlannedMeal(
                            mealType = MealType.BREAKFAST,
                            title = "Besan Chilla with Paneer Filling + Mint Chutney",
                            hindiTitle = "Besan Paneer Chilla",
                            itemsSummary = "2 Besan Chillas + 40g grated paneer + green chutney",
                            calories = 360,
                            proteinG = 17.0,
                            carbsG = 32.0,
                            fatG = 14.0,
                            foodList = listOf(
                                FoodEntryItem("Besan Chilla with Paneer", 1.0, "serving", 290, 15.0, 26.0, 12.0),
                                FoodEntryItem("Masala Chai (Kam Cheeni)", 1.0, "cup", 65, 2.0, 9.0, 2.2)
                            ),
                            desiCoachNote = "Gram flour (Besan) has 2x protein of regular wheat and releases steady energy."
                        ),
                        PlannedMeal(
                            mealType = MealType.LUNCH,
                            title = "2 Phulka + 1 Katori Rajma/Chole + Dahi + Lauki Sabzi",
                            hindiTitle = "2 Roti, Rajma, Lauki Sabzi aur Dahi",
                            itemsSummary = "2 Rotis + 1 bowl Rajma curry + 1 bowl Lauki + 1 katori curd",
                            calories = 540,
                            proteinG = 20.0,
                            carbsG = 82.0,
                            fatG = 11.0,
                            foodList = listOf(
                                FoodEntryItem("Roti (Phulka - No Ghee)", 2.0, "roti", 170, 6.0, 35.0, 0.8),
                                FoodEntryItem("Rajma Masala", 1.0, "bowl", 220, 11.0, 32.0, 5.0),
                                FoodEntryItem("Lauki / Bottle Gourd Sabzi", 1.0, "katori", 75, 1.8, 8.0, 3.5),
                                FoodEntryItem("Dahi / Curd", 1.0, "katori", 70, 3.5, 4.5, 4.0)
                            ),
                            desiCoachNote = "Rajma + Curd together form a complete amino acid protein profile for vegetarians."
                        ),
                        PlannedMeal(
                            mealType = MealType.SNACKS,
                            title = "Roasted Makhana (Foxnuts) + Chaas or Green Tea",
                            hindiTitle = "Makhana aur Namkeen Chaas",
                            itemsSummary = "1 bowl crunchy roasted makhana + spiced buttermilk",
                            calories = 155,
                            proteinG = 6.0,
                            carbsG = 26.0,
                            fatG = 2.8,
                            foodList = listOf(
                                FoodEntryItem("Roasted Makhana", 1.0, "bowl", 110, 3.2, 22.0, 1.0),
                                FoodEntryItem("Chaas / Buttermilk", 1.0, "glass", 45, 2.8, 4.0, 1.8)
                            ),
                            desiCoachNote = "Chaas with roasted jeera soothes acidity and keeps evening snacking in check."
                        ),
                        PlannedMeal(
                            mealType = MealType.DINNER,
                            title = "Paneer Bhurji (150g) + 1 Roti + Big Cucumber Salad",
                            hindiTitle = "Paneer Bhurji, 1 Roti aur Salad",
                            itemsSummary = "Low-oil Paneer Bhurji with tomatoes, capsicum + 1 Phulka + Salad",
                            calories = 440,
                            proteinG = 22.5,
                            carbsG = 32.0,
                            fatG = 20.0,
                            foodList = listOf(
                                FoodEntryItem("Paneer Bhurji", 1.0, "bowl", 240, 14.5, 6.0, 18.0),
                                FoodEntryItem("Roti (Phulka)", 1.0, "roti", 85, 3.0, 17.5, 0.4),
                                FoodEntryItem("Cucumber Tomato Salad", 1.0, "plate", 45, 1.5, 8.5, 0.4)
                            ),
                            desiCoachNote = "Slow-digesting casein protein in paneer prevents muscle breakdown while you sleep."
                        )
                    )
                )
            }
        }
    }
}

/**
 * Scales every meal's portions so the day adds up to [calorieTarget]. The base plans are
 * written for ~1,800 kcal; weight-gain or larger-body targets need bigger portions.
 */
fun DailyDesiPlan.scaledTo(calorieTarget: Int): DailyDesiPlan {
    val baseCalories = meals.sumOf { it.calories }
    if (baseCalories <= 0) return this
    val factor = (calorieTarget.toDouble() / baseCalories).coerceIn(0.6, 2.2)
    if (kotlin.math.abs(factor - 1.0) < 0.05) return copy(targetCalories = calorieTarget)

    val portionLabel = "Portion ×${(factor).toFixed(1)}"
    return copy(
        targetCalories = calorieTarget,
        meals = meals.map { meal ->
            val foods = meal.foodList.map { item ->
                item.copy(
                    quantity = (item.quantity * factor * 2).let { kotlin.math.round(it) / 2 }.coerceAtLeast(0.5),
                    calories = (item.calories * factor).toInt(),
                    proteinG = item.proteinG * factor,
                    carbsG = item.carbsG * factor,
                    fatG = item.fatG * factor
                )
            }
            meal.copy(
                itemsSummary = "${meal.itemsSummary} • $portionLabel",
                calories = foods.sumOf { it.calories },
                proteinG = foods.sumOf { it.proteinG },
                carbsG = foods.sumOf { it.carbsG },
                fatG = foods.sumOf { it.fatG },
                foodList = foods
            )
        }
    )
}

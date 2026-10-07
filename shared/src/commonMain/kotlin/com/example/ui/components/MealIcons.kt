package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.MealType

/** One consistent vector icon per meal slot. */
fun MealType.icon(): ImageVector = when (this) {
    MealType.BREAKFAST -> Icons.Default.FreeBreakfast
    MealType.LUNCH -> Icons.Default.LunchDining
    MealType.SNACKS -> Icons.Default.Restaurant
    MealType.DINNER -> Icons.Default.DinnerDining
}

/** The meal slot that fits the current time of day; the user can still change it. */
fun mealTypeForNow(hour: Int = com.example.util.currentHour()): MealType = when (hour) {
    in 5..10 -> MealType.BREAKFAST
    in 11..15 -> MealType.LUNCH
    in 16..18 -> MealType.SNACKS
    else -> MealType.DINNER
}

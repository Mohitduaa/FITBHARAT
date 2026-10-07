package com.example

import com.example.data.model.ActivityLevel
import com.example.data.model.GoalType
import com.example.data.model.NutritionPlanner
import com.example.data.model.PlannerInput
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlin.test.Test

class NutritionPlannerTest {

    private fun input(
        goal: GoalType,
        current: Double = 80.0,
        target: Double = 72.0,
        gender: String = "Male",
        activity: ActivityLevel = ActivityLevel.LIGHT,
        rate: Double = 0.5
    ) = PlannerInput(gender, 28, 175.0, current, target, goal, activity, rate)

    @Test
    fun `bmr uses mifflin st jeor`() {
        // 10*80 + 6.25*175 - 5*28 + 5 = 1758.75
        assertEquals(1759, NutritionPlanner.plan(input(GoalType.MAINTAIN)).bmr)
    }

    @Test
    fun `maintain eats at maintenance`() {
        val plan = NutritionPlanner.plan(input(GoalType.MAINTAIN, target = 80.0))
        assertEquals(plan.maintenanceCalories, plan.dailyCalories)
        assertEquals(0, plan.dailyAdjustment)
        assertNull(plan.weeksToGoal)
    }

    @Test
    fun `lose creates a deficit of about 550 kcal for half a kilo a week`() {
        val plan = NutritionPlanner.plan(input(GoalType.LOSE))
        assertTrue(plan.dailyAdjustment in -560..-540)
        assertFalse(plan.wasCappedBySafetyFloor)
        // 8 kg at 0.5 kg/week
        assertEquals(16, plan.weeksToGoal)
    }

    @Test
    fun `gain creates a surplus`() {
        val plan = NutritionPlanner.plan(input(GoalType.GAIN, current = 55.0, target = 62.0, rate = 0.25))
        assertTrue(plan.dailyAdjustment > 0)
        assertTrue(plan.dailyCalories > plan.maintenanceCalories)
        assertEquals(28, plan.weeksToGoal)
    }

    @Test
    fun `aggressive loss is capped at the safety floor`() {
        val plan = NutritionPlanner.plan(
            input(GoalType.LOSE, current = 50.0, target = 45.0, gender = "Female", activity = ActivityLevel.SEDENTARY, rate = 0.75)
        )
        assertTrue(plan.wasCappedBySafetyFloor)
        assertTrue(plan.dailyCalories >= 1200)
        assertTrue(plan.dailyCalories >= plan.bmr - 10)
    }

    @Test
    fun `macros add up to roughly the calorie target`() {
        val plan = NutritionPlanner.plan(input(GoalType.LOSE))
        val fromMacros = plan.proteinG * 4 + plan.carbsG * 4 + plan.fatG * 9
        assertTrue(abs(fromMacros - plan.dailyCalories) <= 15)
    }

    @Test
    fun `goal is inferred from weights`() {
        assertEquals(GoalType.LOSE, GoalType.infer(80.0, 72.0))
        assertEquals(GoalType.GAIN, GoalType.infer(55.0, 62.0))
        assertEquals(GoalType.MAINTAIN, GoalType.infer(70.0, 70.3))
    }
}

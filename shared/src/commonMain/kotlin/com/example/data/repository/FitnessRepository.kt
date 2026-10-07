package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.ChallengeProgressEntity
import com.example.data.local.DailyLogEntity
import com.example.data.local.DailyMealTotals
import com.example.data.local.MealEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WorkoutProgressEntity
import com.example.data.local.WeightLogEntity
import com.example.data.model.ChallengeDatabase
import com.example.data.model.DesiChallenge
import com.example.data.model.FoodEntryItem
import com.example.data.model.MealType
import com.example.data.model.nutritionPlan
import com.example.util.minusDays
import com.example.util.toKey
import com.example.util.todayDate
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class FitnessRepository(private val appDao: AppDao) {

    fun getTodayDate(): String = todayDate().toKey()

    val userProfile: Flow<UserProfileEntity> = appDao.getUserProfile().map {
        it ?: UserProfileEntity()
    }

    val allWeightLogs: Flow<List<WeightLogEntity>> = appDao.getAllWeightLogs()

    val dailyMealTotals: Flow<List<DailyMealTotals>> = appDao.getDailyMealTotals()

    /** Distinct foods the user logged most recently, newest first, for one-tap re-adding. */
    val recentFoods: Flow<List<MealEntity>> = appDao.getRecentMeals().map { meals ->
        meals.distinctBy { it.foodName.trim().lowercase() }.take(15)
    }

    val completedWorkoutDays: Flow<Set<String>> = appDao.getWorkoutProgress().map { list -> list.map { it.dayId }.toSet() }

    suspend fun markWorkoutDayCompleted(dayId: String) {
        appDao.insertWorkoutProgress(WorkoutProgressEntity(dayId))
    }

    fun getMealsForToday(): Flow<List<MealEntity>> {
        return appDao.getMealsForDate(getTodayDate())
    }

    fun getTodayLog(): Flow<DailyLogEntity> {
        return appDao.getDailyLog(getTodayDate()).map {
            it ?: DailyLogEntity(date = getTodayDate(), steps = 0, waterMl = 0, caloriesBurned = 0)
        }
    }

    fun getRecentDailyLogs(limit: Int = 7): Flow<List<DailyLogEntity>> {
        return appDao.getRecentDailyLogs(limit)
    }

    val challenges: Flow<List<DesiChallenge>> = appDao.getAllChallengeProgress().map { progressList ->
        val progressMap = progressList.associateBy { it.challengeId }
        ChallengeDatabase.defaultChallenges.map { defaultChallenge ->
            val progress = progressMap[defaultChallenge.id]
            if (progress != null) {
                val today = getTodayDate()
                val yesterday = todayDate().minusDays(1).toKey()
                val completed = progress.isCompleted || progress.daysCompleted >= defaultChallenge.totalDays
                // A run that missed a day is broken; show it as starting over.
                val running = completed || progress.lastCheckInDate == today || progress.lastCheckInDate == yesterday
                defaultChallenge.copy(
                    daysCompleted = if (running) progress.daysCompleted else 0,
                    isJoined = progress.isJoined,
                    isCompleted = progress.isCompleted || progress.daysCompleted >= defaultChallenge.totalDays,
                    checkedInToday = progress.lastCheckInDate == getTodayDate()
                )
            } else {
                defaultChallenge
            }
        }
    }

    /** Emits null until the user has completed profile setup. */
    val onboardedProfile: Flow<UserProfileEntity?> = appDao.getUserProfile().map { profile ->
        profile?.takeIf { it.isOnboarded }
    }

    /**
     * Saves the profile and derives calorie and water targets from it. The first save also
     * records the starting weight so progress has a baseline.
     */
    suspend fun saveProfile(profile: UserProfileEntity) {
        val previous = appDao.getUserProfile().firstOrNull()
        val isFirstSetup = previous == null || !previous.isOnboarded
        val goalChanged = previous != null &&
            (previous.goalType != profile.goalType || previous.targetWeightKg != profile.targetWeightKg)
        val plan = profile.nutritionPlan()
        val updated = profile.copy(
            // A new goal or target starts a new journey, so progress is measured from today.
            startWeightKg = if (isFirstSetup || goalChanged) profile.currentWeightKg else profile.startWeightKg,
            calorieTarget = plan.dailyCalories,
            waterGoalMl = plan.waterMl,
            isOnboarded = true
        )
        appDao.insertOrUpdateProfile(updated)
        if (isFirstSetup || previous?.currentWeightKg != profile.currentWeightKg) {
            appDao.insertWeightLog(
                WeightLogEntity(
                    date = getTodayDate(),
                    weightKg = profile.currentWeightKg,
                    note = if (isFirstSetup) "Starting weight" else ""
                )
            )
        }
        recalculateTodayScore()
    }

    suspend fun addMeal(
        mealType: MealType,
        foodName: String,
        quantity: Double,
        unit: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double = 0.0,
        itemsJson: String = ""
    ): Long {
        val today = getTodayDate()
        val id = appDao.insertMeal(
            MealEntity(
                date = today,
                mealType = mealType.name,
                foodName = foodName,
                quantity = quantity,
                unit = unit,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fiberG = fiberG,
                itemsJson = itemsJson
            )
        )
        recalculateTodayScore()
        return id
    }

    suspend fun addPlannedMeal(meal: com.example.data.model.PlannedMeal) {
        val today = getTodayDate()
        for (item in meal.foodList) {
            appDao.insertMeal(
                MealEntity(
                    date = today,
                    mealType = meal.mealType.name,
                    foodName = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG
                )
            )
        }
        recalculateTodayScore()
    }

    /** Saves an edited meal in place (same id), e.g. a new portion or meal slot. */
    suspend fun updateMeal(meal: MealEntity) {
        appDao.insertMeal(meal)
        recalculateTodayScore()
    }

    suspend fun deleteMeal(id: Long) {
        appDao.deleteMeal(id)
        recalculateTodayScore()
    }

    suspend fun addWater(ml: Int = 250) {
        val today = getTodayDate()
        val currentLog = appDao.getDailyLog(today).firstOrNull()
            ?: DailyLogEntity(date = today, steps = 0, waterMl = 0, caloriesBurned = 0)
        val newWater = (currentLog.waterMl + ml).coerceAtLeast(0)
        appDao.insertOrUpdateDailyLog(currentLog.copy(waterMl = newWater))
        recalculateTodayScore()
    }

    suspend fun addSteps(stepsToAdd: Int = 500) {
        val today = getTodayDate()
        val currentLog = appDao.getDailyLog(today).firstOrNull()
            ?: DailyLogEntity(date = today, steps = 0, waterMl = 0, caloriesBurned = 0)
        val newSteps = currentLog.steps + stepsToAdd
        val addedBurn = (stepsToAdd * 0.04).toInt() // Approx 40 kcal per 1000 steps
        appDao.insertOrUpdateDailyLog(
            currentLog.copy(
                steps = newSteps,
                caloriesBurned = currentLog.caloriesBurned + addedBurn
            )
        )
        recalculateTodayScore()
    }

    /** Corrects today's step count by hand; the step share of calories burned follows it. */
    suspend fun setTodaySteps(steps: Int) {
        val today = getTodayDate()
        val currentLog = appDao.getDailyLog(today).firstOrNull()
            ?: DailyLogEntity(date = today, steps = 0, waterMl = 0, caloriesBurned = 0)
        val newSteps = steps.coerceAtLeast(0)
        val burnChange = ((newSteps - currentLog.steps) * 0.04).toInt()
        appDao.insertOrUpdateDailyLog(
            currentLog.copy(
                steps = newSteps,
                caloriesBurned = (currentLog.caloriesBurned + burnChange).coerceAtLeast(0)
            )
        )
        recalculateTodayScore()
    }

    suspend fun logWorkoutCompleted(workoutTitle: String, caloriesBurned: Int, durationMinutes: Int) {
        val today = getTodayDate()
        val currentLog = appDao.getDailyLog(today).firstOrNull()
            ?: DailyLogEntity(date = today, steps = 0, waterMl = 0, caloriesBurned = 0)
        val newBurn = currentLog.caloriesBurned + caloriesBurned
        appDao.insertOrUpdateDailyLog(
            currentLog.copy(
                caloriesBurned = newBurn,
                note = "Workout completed: $workoutTitle ($durationMinutes min, -$caloriesBurned kcal)"
            )
        )
        recalculateTodayScore()
    }

    suspend fun logWeight(weightKg: Double, note: String = "") {
        val today = getTodayDate()
        appDao.insertWeightLog(
            WeightLogEntity(
                date = today,
                weightKg = weightKg,
                note = note
            )
        )
        val profile = appDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        val updated = profile.copy(currentWeightKg = weightKg)
        val plan = updated.nutritionPlan()
        appDao.insertOrUpdateProfile(updated.copy(calorieTarget = plan.dailyCalories, waterGoalMl = plan.waterMl))
        recalculateTodayScore()
    }

    suspend fun checkInChallenge(challengeId: String) {
        val existingList = appDao.getAllChallengeProgress().firstOrNull() ?: emptyList()
        val existing = existingList.firstOrNull { it.challengeId == challengeId }
        val today = getTodayDate()
        if (existing == null) {
            appDao.insertOrUpdateChallenge(
                ChallengeProgressEntity(
                    challengeId = challengeId,
                    daysCompleted = 1,
                    isJoined = true,
                    lastCheckInDate = today
                )
            )
        } else {
            if (existing.lastCheckInDate == today) return
            val total = ChallengeDatabase.defaultChallenges.firstOrNull { it.id == challengeId }?.totalDays ?: Int.MAX_VALUE
            // Challenges are about consecutive days: a missed day starts the count again.
            val yesterday = todayDate().minusDays(1).toKey()
            val continues = existing.lastCheckInDate == yesterday
            val newDays = if (continues && !existing.isCompleted) existing.daysCompleted + 1 else 1
            appDao.insertOrUpdateChallenge(
                existing.copy(
                    daysCompleted = newDays,
                    isJoined = true,
                    isCompleted = newDays >= total,
                    lastCheckInDate = today
                )
            )
        }
    }

    suspend fun toggleJoinChallenge(challengeId: String) {
        val existingList = appDao.getAllChallengeProgress().firstOrNull() ?: emptyList()
        val existing = existingList.firstOrNull { it.challengeId == challengeId }
        if (existing == null) {
            appDao.insertOrUpdateChallenge(
                ChallengeProgressEntity(
                    challengeId = challengeId,
                    daysCompleted = 0,
                    isJoined = true
                )
            )
        } else {
            appDao.insertOrUpdateChallenge(
                existing.copy(isJoined = !existing.isJoined)
            )
        }
    }

    suspend fun toggleProStatus(enable: Boolean) {
        val profile = appDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        appDao.insertOrUpdateProfile(profile.copy(isPro = enable))
    }

    private suspend fun recalculateTodayScore() {
        val today = getTodayDate()
        val log = appDao.getDailyLog(today).firstOrNull() ?: return
        val profile = appDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        val meals = appDao.getMealsForDate(today).firstOrNull() ?: emptyList()

        val totalConsumed = meals.sumOf { it.calories }
        val targetCalories = profile.calorieTarget

        var score = 50

        // Calorie discipline
        val diff = abs(totalConsumed - targetCalories)
        if (diff <= 150) score += 20
        else if (diff <= 300) score += 10
        else if (totalConsumed > targetCalories + 400) score -= 10

        // Steps discipline
        if (log.steps >= profile.stepGoal) score += 15
        else if (log.steps >= profile.stepGoal * 0.7) score += 10

        // Water discipline
        if (log.waterMl >= profile.waterGoalMl) score += 15
        else if (log.waterMl >= profile.waterGoalMl * 0.7) score += 10

        score = score.coerceIn(40, 100)
        appDao.insertOrUpdateDailyLog(log.copy(score = score))
    }
}

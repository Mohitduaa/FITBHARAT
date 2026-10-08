package com.example.ui.viewmodel

import com.example.data.model.MealItem
import com.example.data.model.toJson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.backup.BackupException
import com.example.data.backup.BackupManager
import com.example.data.remote.FoodProduct
import com.example.data.remote.FoodProductException
import com.example.data.remote.FoodProductService
import com.example.data.remote.FoodScanException
import com.example.data.local.DailyMealTotals
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import com.example.data.local.DailyLogEntity
import com.example.data.local.MealEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightLogEntity
import com.example.data.model.AiFoodScanResult
import com.example.data.model.ChatMessage
import com.example.data.model.CoachPromptSuggestions
import com.example.data.model.DailyDesiPlan
import com.example.data.model.DesiChallenge
import com.example.data.model.DesiDietPlanGenerator
import com.example.data.model.DietPreference
import com.example.data.model.MealType
import com.example.data.model.Badge
import com.example.data.model.BadgeCalculator
import com.example.data.model.NutritionPlan
import com.example.data.model.StreakCalculator
import com.example.data.model.StreakInfo
import com.example.util.todayDate
import com.example.data.model.nutritionPlan
import com.example.data.model.scaledTo
import com.example.data.model.PlannedMeal
import com.example.data.model.ProgramDay
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutRoutine
import com.example.data.remote.GeminiService
import com.example.data.repository.FitnessRepository
import com.example.data.sensor.StepTrackingMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    TODAY,
    DIET,
    WORKOUTS,
    PROGRESS,
    COACH
}

sealed interface BarcodeUiState {
    object Idle : BarcodeUiState
    data class Loading(val barcode: String) : BarcodeUiState
    data class Found(val product: FoodProduct, val estimating: Boolean = false, val estimateFailed: Boolean = false) : BarcodeUiState
    data class Error(val message: String) : BarcodeUiState
}

sealed interface AiScanUiState {
    object Idle : AiScanUiState
    object Scanning : AiScanUiState
    data class Success(val result: AiFoodScanResult) : AiScanUiState
    data class Error(val message: String) : AiScanUiState
}

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val geminiService = GeminiService(container.platform)

    /** Whether this build can reach Gemini; shown on the coach screen. */
    val aiConfigured: Boolean get() = geminiService.isConfigured

    private val backupManager = BackupManager(container.database.appDao())

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun setBackupMessage(message: String?) {
        _backupMessage.value = message
    }

    /** Builds the backup file text and hands it to [onReady] together with a suggested file name. */
    fun createBackup(onReady: (fileName: String, content: String) -> Unit) {
        viewModelScope.launch {
            try {
                onReady(backupManager.suggestedFileName(), backupManager.export())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                container.platform.logError("MainViewModel", "Backup export failed", e)
                _backupMessage.value = "The backup could not be created."
            }
        }
    }

    fun restoreBackup(content: String) {
        viewModelScope.launch {
            try {
                backupManager.restore(content)
                _backupMessage.value = "Backup restored successfully."
                _showProfileDialog.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                _backupMessage.value = e.message
            } catch (e: Exception) {
                container.platform.logError("MainViewModel", "Backup restore failed", e)
                _backupMessage.value = "The backup could not be restored."
            }
        }
    }

    val stepTrackingMode: StateFlow<StepTrackingMode> = container.platform.stepTracker.mode

    init {
        container.platform.stepTracker.onSteps = { addSteps(it) }
    }

    fun requestNotificationPermission() = container.platform.reminders.requestPermission()

    val autoBackup: com.example.platform.AutoBackup get() = container.platform.autoBackup

    fun refreshStepTracking() = container.platform.stepTracker.refresh()
    fun stopStepTracking() = container.platform.stepTracker.stop()
    fun requestStepPermission() = container.platform.stepTracker.requestPermission()

    val userProfile: StateFlow<UserProfileEntity> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    /** null while the database is loading, then whether first-run profile setup is needed. */
    val needsOnboarding: StateFlow<Boolean?> = repository.onboardedProfile
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nutritionPlan: StateFlow<NutritionPlan> = userProfile
        .map { it.nutritionPlan() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity().nutritionPlan())

    val todayLog: StateFlow<DailyLogEntity> = repository.getTodayLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyLogEntity(date = repository.getTodayDate()))

    val todayMeals: StateFlow<List<MealEntity>> = repository.getMealsForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeightLogs: StateFlow<List<WeightLogEntity>> = repository.allWeightLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDailyLogs: StateFlow<List<DailyLogEntity>> = repository.getRecentDailyLogs(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Intermittent fasting
    val activeFast: StateFlow<com.example.data.local.FastingSessionEntity?> = repository.activeFast
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastCompletedFast: StateFlow<com.example.data.local.FastingSessionEntity?> = repository.recentFasts
        .map { list -> list.firstOrNull { it.endMillis != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startFast(hours: Int) {
        viewModelScope.launch {
            val fast = repository.startFast(hours)
            container.platform.reminders.requestPermission()
            container.platform.reminders.scheduleFastEnd(fast.startMillis + hours * 3_600_000L)
        }
    }

    fun endFast() {
        viewModelScope.launch {
            repository.endFast()
            container.platform.reminders.scheduleFastEnd(null)
        }
    }

    /** This week's report; recomputed whenever logs, meals, weight or fasts change. */
    val weeklyReport: StateFlow<com.example.data.model.WeeklyReport?> = combine(
        userProfile,
        repository.allWeightLogs,
        repository.getRecentDailyLogs(14),
        repository.dailyMealTotals,
        repository.recentFasts
    ) { profile, weights, logs, totals, fasts ->
        com.example.data.model.WeeklyReport.compute(profile, weights, logs, totals, fasts, todayDate())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The last 30 days of step / water logs for the history chart. */
    val historyLogs: StateFlow<List<DailyLogEntity>> = repository.getRecentDailyLogs(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streaks: StateFlow<List<StreakInfo>> = combine(
        userProfile,
        nutritionPlan,
        repository.getRecentDailyLogs(400),
        repository.dailyMealTotals,
        todayLog
    ) { profile, plan, logs, totals, _ ->
        StreakCalculator.computeAll(profile, plan, logs, totals, todayDate())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedWorkoutDays: StateFlow<Set<String>> = repository.completedWorkoutDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val challenges: StateFlow<List<DesiChallenge>> = repository.challenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(MainTab.TODAY)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _aiScanUiState = MutableStateFlow<AiScanUiState>(AiScanUiState.Idle)
    val aiScanUiState: StateFlow<AiScanUiState> = _aiScanUiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    init {
        // Keep the coach greeting in sync with the profile until the user starts chatting.
        viewModelScope.launch {
            repository.onboardedProfile.filterNotNull().collect { profile ->
                container.platform.reminders.apply(
                    profile.remindWater, profile.remindMeals, profile.remindWalk,
                    com.example.data.model.ReminderTimes.fromJson(profile.reminderTimes)
                )
                if (_chatMessages.value.none { it.isUser }) {
                    _chatMessages.value = listOf(
                        ChatMessage(
                            text = GeminiService.coachGreeting(profile),
                            isUser = false,
                            suggestions = CoachPromptSuggestions.starterQuestions
                        )
                    )
                }
            }
        }
    }

    init {
        // Smart reminders: tell the scheduler what is already done today.
        viewModelScope.launch {
            combine(todayMeals, todayLog, userProfile) { meals, log, profile ->
                com.example.data.model.TodayStatus(
                    lunchLogged = meals.any { it.mealType == MealType.LUNCH.name },
                    dinnerLogged = meals.any { it.mealType == MealType.DINNER.name },
                    steps = log.steps,
                    waterMl = log.waterMl,
                    waterGoalMl = profile.waterGoalMl
                ) to com.example.data.model.ReminderTimes.fromJson(profile.reminderTimes)
            }.collect { (status, times) -> container.platform.reminders.onTodayStatus(status, times) }
        }
    }

    private val _isCoachTyping = MutableStateFlow(false)
    val isCoachTyping: StateFlow<Boolean> = _isCoachTyping.asStateFlow()

    private val _showProDialog = MutableStateFlow(false)
    val showProDialog: StateFlow<Boolean> = _showProDialog.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    private val _showAddMealDialog = MutableStateFlow(false)
    val showAddMealDialog: StateFlow<Boolean> = _showAddMealDialog.asStateFlow()

    private val _selectedMealType = MutableStateFlow(MealType.BREAKFAST)
    val selectedMealType: StateFlow<MealType> = _selectedMealType.asStateFlow()

    // Workout execution state
    private val _activeWorkout = MutableStateFlow<WorkoutRoutine?>(null)
    val activeWorkout: StateFlow<WorkoutRoutine?> = _activeWorkout.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _exerciseRemainingSeconds = MutableStateFlow(0)
    val exerciseRemainingSeconds: StateFlow<Int> = _exerciseRemainingSeconds.asStateFlow()

    private val _isWorkoutPaused = MutableStateFlow(false)
    val isWorkoutPaused: StateFlow<Boolean> = _isWorkoutPaused.asStateFlow()

    private var workoutJob: Job? = null

    // AI diet plan
    val aiDietPlan: StateFlow<com.example.data.model.AiDietPlan?> = userProfile
        .map { com.example.data.model.AiDietPlan.fromJson(it.aiDietPlan) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dietPlanBusy = MutableStateFlow(false)
    val dietPlanBusy: StateFlow<Boolean> = _dietPlanBusy.asStateFlow()

    private val _dietPlanError = MutableStateFlow<String?>(null)
    val dietPlanError: StateFlow<String?> = _dietPlanError.asStateFlow()

    fun generateDietPlan(extra: String) {
        if (_dietPlanBusy.value) return
        viewModelScope.launch {
            _dietPlanBusy.value = true
            _dietPlanError.value = null
            try {
                val profile = userProfile.value
                val plan = geminiService.generateDietPlan(profile, profile.nutritionPlan(), extra)
                repository.editProfile { it.copy(aiDietPlan = plan.toJson()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FoodScanException) {
                _dietPlanError.value = e.message
            } catch (e: Exception) {
                container.platform.logError("MainViewModel", "Diet plan failed", e)
                _dietPlanError.value = "Plan nahi ban paaya. Dobara try karein."
            } finally {
                _dietPlanBusy.value = false
            }
        }
    }

    fun clearDietPlan() {
        viewModelScope.launch { repository.editProfile { it.copy(aiDietPlan = "") } }
    }

    // Daily plan: the AI plan when there is one, otherwise derived from diet preference
    val dailyDesiPlan: StateFlow<DailyDesiPlan> = combine(userProfile) { profileArray ->
        val profile = profileArray[0]
        com.example.data.model.AiDietPlan.fromJson(profile.aiDietPlan)?.let {
            return@combine it.toDailyPlan(profile.stepGoal, profile.waterGoalMl)
        }
        val pref = try {
            DietPreference.valueOf(profile.dietPreference)
        } catch (e: Exception) {
            DietPreference.VEGETARIAN
        }
        DesiDietPlanGenerator.getPlan(pref, profile.calorieTarget).scaledTo(profile.calorieTarget)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DesiDietPlanGenerator.getPlan(DietPreference.VEGETARIAN, 1850)
    )

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openAddMeal(mealType: MealType = MealType.BREAKFAST) {
        _selectedMealType.value = mealType
        _showAddMealDialog.value = true
    }

    fun closeAddMeal() {
        _showAddMealDialog.value = false
    }

    fun addMeal(
        foodName: String,
        quantity: Double,
        unit: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double = 0.0
    ) {
        viewModelScope.launch {
            repository.addMeal(
                mealType = _selectedMealType.value,
                foodName = foodName,
                quantity = quantity,
                unit = unit,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fiberG = fiberG
            )
            _showAddMealDialog.value = false
        }
    }

    // Recent foods: one tap re-adds a food the user logged before, into the meal being added.
    val recentFoods: StateFlow<List<MealEntity>> = repository.recentFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRecentFood(food: MealEntity) = addMeal(
        foodName = food.foodName,
        quantity = food.quantity,
        unit = food.unit,
        calories = food.calories,
        proteinG = food.proteinG,
        carbsG = food.carbsG,
        fatG = food.fatG,
        fiberG = food.fiberG
    )

    // Per-day food totals, for the weekly calorie chart.
    val dailyMealTotals: StateFlow<List<DailyMealTotals>> = repository.dailyMealTotals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<Badge>> = combine(
        combine(userProfile, streaks, completedWorkoutDays, dailyMealTotals) { p, s, w, t -> listOf(p, s, w, t) },
        repository.getRecentDailyLogs(1000),
        repository.recentFasts
    ) { (profile, streakList, workoutDays, totals), logs, fasts ->
        @Suppress("UNCHECKED_CAST")
        BadgeCalculator.compute(
            profile as UserProfileEntity,
            streakList as List<StreakInfo>,
            workoutDays as Set<String>,
            totals as List<DailyMealTotals>,
            logs,
            fasts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** A badge earned since the user last looked, to celebrate once. */
    val newBadge: StateFlow<Badge?> = combine(badges, userProfile) { list, profile ->
        if (!profile.isOnboarded || list.isEmpty() || profile.seenBadges.isBlank()) return@combine null
        val seen = profile.seenBadges.split(',').toSet()
        list.firstOrNull { it.earned && it.id !in seen }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // First run of this feature: count already-earned badges as seen, so only new ones pop up.
        viewModelScope.launch {
            combine(badges, userProfile) { list, profile -> list to profile }.collect { (list, profile) ->
                if (profile.isOnboarded && list.isNotEmpty() && profile.seenBadges.isBlank()) {
                    val ids = listOf("_") + list.filter { it.earned }.map { it.id }
                    repository.editProfile { it.copy(seenBadges = ids.joinToString(",")) }
                }
            }
        }
    }

    fun markBadgeSeen(id: String) {
        viewModelScope.launch {
            repository.editProfile { p ->
                val seen = p.seenBadges.split(',').filter { it.isNotBlank() }.toSet()
                if (id in seen) p else p.copy(seenBadges = (seen + id).joinToString(","))
            }
        }
    }

    // Barcode lookup of packaged foods
    private val foodProductService = FoodProductService()
    private val _barcodeState = MutableStateFlow<BarcodeUiState>(BarcodeUiState.Idle)
    val barcodeState: StateFlow<BarcodeUiState> = _barcodeState.asStateFlow()

    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            _barcodeState.value = BarcodeUiState.Loading(barcode)
            _barcodeState.value = try {
                BarcodeUiState.Found(foodProductService.lookup(barcode))
            } catch (e: CancellationException) {
                throw e
            } catch (e: FoodProductException) {
                BarcodeUiState.Error(e.message.orEmpty())
            } catch (e: Exception) {
                container.platform.logError("MainViewModel", "Barcode lookup failed", e)
                BarcodeUiState.Error("Could not look up this product. Please try again.")
            }
        }
    }

    /** Fills in a scanned product's missing nutrition with an AI estimate from its name. */
    fun estimateBarcodeWithAi() {
        val found = _barcodeState.value as? BarcodeUiState.Found ?: return
        _barcodeState.value = found.copy(estimating = true, estimateFailed = false)
        viewModelScope.launch {
            val estimate = geminiService.estimatePackagedFood(found.product.name, found.product.brand)
            _barcodeState.value = if (estimate != null) {
                BarcodeUiState.Found(
                    found.product.copy(
                        kcalPer100g = estimate.kcalPer100g,
                        proteinPer100g = estimate.proteinPer100g,
                        carbsPer100g = estimate.carbsPer100g,
                        fatPer100g = estimate.fatPer100g,
                        fiberPer100g = estimate.fiberPer100g,
                        hasCalories = true,
                        aiEstimated = true
                    )
                )
            } else {
                found.copy(estimating = false, estimateFailed = true)
            }
        }
    }

    fun reportBarcodeError(message: String) {
        _barcodeState.value = BarcodeUiState.Error(message)
    }

    fun dismissBarcode() {
        _barcodeState.value = BarcodeUiState.Idle
    }

    /** Logs [grams] of a scanned packaged food into [mealType]. */
    fun addBarcodeProduct(product: FoodProduct, grams: Double, mealType: MealType) {
        val factor = grams / 100.0
        viewModelScope.launch {
            repository.addMeal(
                mealType = mealType,
                foodName = listOf(product.brand, product.name).filter { it.isNotBlank() }.joinToString(" "),
                quantity = grams,
                unit = "g",
                calories = (product.kcalPer100g * factor).roundToInt(),
                proteinG = product.proteinPer100g * factor,
                carbsG = product.carbsPer100g * factor,
                fatG = product.fatPer100g * factor,
                fiberG = product.fiberPer100g * factor
            )
            _barcodeState.value = BarcodeUiState.Idle
            _showAddMealDialog.value = false
        }
    }

    fun logPlannedMeal(meal: PlannedMeal) {
        viewModelScope.launch {
            repository.addPlannedMeal(meal)
        }
    }

    fun updateMeal(meal: com.example.data.local.MealEntity) {
        viewModelScope.launch {
            repository.updateMeal(meal)
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun addWater(ml: Int = 250) {
        viewModelScope.launch {
            repository.addWater(ml)
        }
    }

    fun setTodaySteps(steps: Int) {
        viewModelScope.launch { repository.setTodaySteps(steps) }
    }

    fun addSteps(steps: Int = 500) {
        viewModelScope.launch {
            repository.addSteps(steps)
        }
    }

    fun logWeight(weight: Double, note: String) {
        viewModelScope.launch {
            repository.logWeight(weight, note)
        }
    }

    fun checkInChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.checkInChallenge(challengeId)
        }
    }

    fun toggleJoinChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.toggleJoinChallenge(challengeId)
        }
    }

    fun openProDialog() {
        _showProDialog.value = true
    }

    fun closeProDialog() {
        _showProDialog.value = false
    }

    fun activatePro() {
        viewModelScope.launch {
            repository.toggleProStatus(true)
            _showProDialog.value = false
        }
    }

    fun openProfileDialog() {
        _showProfileDialog.value = true
    }

    fun closeProfileDialog() {
        _showProfileDialog.value = false
    }

    fun updateProfile(updated: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(updated)
            _showProfileDialog.value = false
        }
    }

    // AI Food Scanning
    fun scanFoodPhoto(jpeg: ByteArray) {
        viewModelScope.launch {
            _aiScanUiState.value = AiScanUiState.Scanning
            _aiScanUiState.value = try {
                AiScanUiState.Success(geminiService.analyzeFoodImage(jpeg, userProfile.value))
            } catch (e: CancellationException) {
                throw e
            } catch (e: FoodScanException) {
                AiScanUiState.Error(e.message.orEmpty())
            } catch (e: Exception) {
                container.platform.logError("MainViewModel", "Food scan failed", e)
                AiScanUiState.Error("The scan failed. Please try again.")
            }
        }
    }


    fun addScannedMealToDiary(mealType: MealType, result: AiFoodScanResult) {
        viewModelScope.launch {
            repository.addMeal(
                mealType = mealType,
                foodName = result.dishName,
                quantity = 1.0,
                unit = result.portionDescription,
                calories = result.estimatedCalories,
                proteinG = result.proteinG,
                carbsG = result.carbsG,
                fatG = result.fatG,
                fiberG = result.fiberG,
                itemsJson = result.detectedItems.map {
                    MealItem(it.name, it.portion, it.calories, it.proteinG, it.carbsG, it.fatG)
                }.toJson()
            )
            _aiScanUiState.value = AiScanUiState.Idle
            _selectedTab.value = MainTab.DIET
        }
    }

    fun resetAiScan() {
        _aiScanUiState.value = AiScanUiState.Idle
    }

    // AI Coach Chat
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(text = text, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isCoachTyping.value = true

        viewModelScope.launch {
            val history = _chatMessages.value.map { it.text to it.isUser }
            val reply = geminiService.chatWithDesiCoach(text, history, userProfile.value, todaySummary())
            _isCoachTyping.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage(text = reply, isUser = false)
        }
    }

    /** Today's numbers, so the coach can answer "what should I eat for dinner?" concretely. */
    private fun todaySummary(): String {
        val meals = todayMeals.value
        val log = todayLog.value
        val eaten = meals.sumOf { it.calories }
        val protein = meals.sumOf { it.proteinG }.toInt()
        val foods = meals.joinToString { it.foodName }.ifBlank { "nothing logged yet" }
        return "$eaten of ${userProfile.value.calorieTarget} kcal eaten, ${protein}g protein, " +
            "${log.steps} steps, ${log.waterMl} ml water. Foods: $foods."
    }

    // Workouts Runner
    fun startWorkout(workout: WorkoutRoutine) {
        _activeWorkout.value = workout
        _currentExerciseIndex.value = 0
        _isWorkoutPaused.value = false
        val firstExercise = workout.exercises.firstOrNull()
        _exerciseRemainingSeconds.value = firstExercise?.durationSeconds ?: 30
        runWorkoutTimer()
    }

    private fun runWorkoutTimer() {
        workoutJob?.cancel()
        workoutJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isWorkoutPaused.value) {
                    val remaining = _exerciseRemainingSeconds.value
                    if (remaining > 1) {
                        _exerciseRemainingSeconds.value = remaining - 1
                    } else {
                        nextExercise()
                    }
                }
            }
        }
    }

    fun toggleWorkoutPause() {
        _isWorkoutPaused.value = !_isWorkoutPaused.value
    }

    fun nextExercise() {
        val workout = _activeWorkout.value ?: return
        val nextIdx = _currentExerciseIndex.value + 1
        if (nextIdx < workout.exercises.size) {
            _currentExerciseIndex.value = nextIdx
            _exerciseRemainingSeconds.value = workout.exercises[nextIdx].durationSeconds
        } else {
            completeWorkout()
        }
    }

    fun startProgramDay(day: ProgramDay) = startWorkout(day.toRoutine(userProfile.value.currentWeightKg))

    fun completeWorkout() {
        workoutJob?.cancel()
        val workout = _activeWorkout.value ?: return
        viewModelScope.launch {
            repository.logWorkoutCompleted(
                workoutTitle = workout.title,
                caloriesBurned = workout.caloriesBurned,
                durationMinutes = workout.durationMinutes
            )
            if (WorkoutProgram.isProgramDay(workout.id)) repository.markWorkoutDayCompleted(workout.id)
            _activeWorkout.value = null
        }
    }

    fun cancelWorkout() {
        workoutJob?.cancel()
        _activeWorkout.value = null
    }

    override fun onCleared() {
        super.onCleared()
        workoutJob?.cancel()
    }
}

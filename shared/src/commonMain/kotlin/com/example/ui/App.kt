package com.example.ui

import com.example.data.sensor.StepTrackingMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddMealDialog
import com.example.ui.components.FloatingNavBar
import com.example.ui.components.FloatingNavItem
import com.example.ui.components.AiFoodScanDialog
import com.example.ui.components.LogWeightDialog
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.components.ProfileEditDialog
import com.example.ui.components.WorkoutRunnerDialog
import com.example.ui.screens.CoachScreen
import com.example.ui.screens.DietScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.viewmodel.AiScanUiState
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.AppContainer
import com.example.platform.PlatformBackHandler
import com.example.platform.rememberBackupFiles
import com.example.util.todayDate
import com.example.platform.rememberBarcodeScanner
import com.example.ui.components.BarcodeProductDialog
import com.example.ui.components.mealTypeForNow
import com.example.ui.theme.FitBharatTheme

private data class NavItem(val tab: MainTab, val label: String, val filled: ImageVector, val outlined: ImageVector)

/** Entry point shared by Android and iOS. */
@Composable
fun FitBharatRoot(container: AppContainer) {
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory { initializer { MainViewModel(container) } }
    )
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val dark = when (profile.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    com.example.platform.SystemBarsAppearance(darkTheme = dark)
    FitBharatTheme(darkTheme = dark) {
        FitBharatApp(viewModel = viewModel)
    }
}

@Composable
fun FitBharatApp(viewModel: MainViewModel) {
    val stepTrackingMode by viewModel.stepTrackingMode.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val todayLog by viewModel.todayLog.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val allWeightLogs by viewModel.allWeightLogs.collectAsStateWithLifecycle()
    val recentDailyLogs by viewModel.recentDailyLogs.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val dailyPlan by viewModel.dailyDesiPlan.collectAsStateWithLifecycle()
    val nutritionPlan by viewModel.nutritionPlan.collectAsStateWithLifecycle()
    val streaks by viewModel.streaks.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    val dailyMealTotals by viewModel.dailyMealTotals.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val barcodeState by viewModel.barcodeState.collectAsStateWithLifecycle()
    val barcodeScanner = rememberBarcodeScanner(
        onScanned = { viewModel.lookupBarcode(it) },
        onError = { viewModel.reportBarcodeError(it) }
    )
    val completedWorkoutDays by viewModel.completedWorkoutDays.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val backupFiles = rememberBackupFiles(
        onPicked = { viewModel.restoreBackup(it) },
        onSaved = { viewModel.setBackupMessage("Backup saved.") },
        onError = { viewModel.setBackupMessage(it) }
    )

    val aiScanUiState by viewModel.aiScanUiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isCoachTyping by viewModel.isCoachTyping.collectAsStateWithLifecycle()
    val showProDialog by viewModel.showProDialog.collectAsStateWithLifecycle()
    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showAddMealDialog by viewModel.showAddMealDialog.collectAsStateWithLifecycle()
    val selectedMealType by viewModel.selectedMealType.collectAsStateWithLifecycle()

    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val currentExerciseIndex by viewModel.currentExerciseIndex.collectAsStateWithLifecycle()
    val exerciseRemainingSeconds by viewModel.exerciseRemainingSeconds.collectAsStateWithLifecycle()
    val isWorkoutPaused by viewModel.isWorkoutPaused.collectAsStateWithLifecycle()

    // Saveable so the scanner (and its pending photo result) survives the app being killed while the gallery is open.
    var showAiScanModalManual by rememberSaveable { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }

    // BackHandler: return to Today screen if on a sub-screen
    PlatformBackHandler(enabled = selectedTab != MainTab.TODAY) {
        viewModel.selectTab(MainTab.TODAY)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            FloatingNavBar(
                items = listOf(
                    FloatingNavItem(MainTab.TODAY.name, "Home", Icons.Filled.Home, Icons.Outlined.Home),
                    FloatingNavItem(MainTab.DIET.name, "Diet", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
                    FloatingNavItem(MainTab.WORKOUTS.name, "Workouts", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
                    FloatingNavItem(MainTab.PROGRESS.name, "Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                    FloatingNavItem(MainTab.COACH.name, "Coach", Icons.Filled.SupportAgent, Icons.Outlined.SupportAgent)
                ),
                selectedKey = selectedTab.name,
                onSelect = { viewModel.selectTab(MainTab.valueOf(it)) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, animationSpec = tween(220), label = "tab") { tab ->
            when (tab) {
                MainTab.TODAY -> {
                    TodayScreen(
                        profile = userProfile,
                        plan = nutritionPlan,
                        streaks = streaks,
                        daysSinceWeighIn = allWeightLogs.maxOfOrNull { it.date }?.let { last -> runCatching { (todayDate().toEpochDays() - kotlinx.datetime.LocalDate.parse(last).toEpochDays()).toInt() }.getOrNull() },
                        onLogWeight = { showLogWeightDialog = true },
                        todayLog = todayLog,
                        todayMeals = todayMeals,
                        dailyPlan = dailyPlan,
                        onAddSteps = { viewModel.addSteps(it) },
                        onSetSteps = { viewModel.setTodaySteps(it) },
                        stepTrackingMode = stepTrackingMode,
                        onEnableStepTracking = { viewModel.requestStepPermission() },
                        onAddWater = { viewModel.addWater(it) },
                        onLogPlannedMeal = { viewModel.logPlannedMeal(it) },
                        onOpenAddMeal = { viewModel.openAddMeal(it) },
                        onOpenAiScan = { showAiScanModalManual = true },
                        onOpenPro = { viewModel.openProDialog() },
                        onOpenProfile = { viewModel.openProfileDialog() },
                        onOpenProgress = { viewModel.selectTab(MainTab.PROGRESS) }
                    )
                }
                MainTab.DIET -> {
                    DietScreen(
                        meals = todayMeals,
                        targetCalories = userProfile.calorieTarget,
                        onOpenAddMeal = { viewModel.openAddMeal(it) },
                        onDeleteMeal = { viewModel.deleteMeal(it) },
                        onUpdateMeal = { viewModel.updateMeal(it) },
                        onOpenAiScan = { showAiScanModalManual = true }
                    )
                }
                MainTab.WORKOUTS -> {
                    WorkoutsScreen(
                        burnedToday = todayLog.caloriesBurned,
                        completedDays = completedWorkoutDays,
                        weightKg = userProfile.currentWeightKg,
                        onStartWorkout = { viewModel.startWorkout(it) },
                        onStartProgramDay = { viewModel.startProgramDay(it) }
                    )
                }
                MainTab.PROGRESS -> {
                    ProgressScreen(
                        todaySteps = todayLog.steps,
                        todayWaterMl = todayLog.waterMl,
                        badges = badges,
                        mealTotals = dailyMealTotals,
                        calorieTarget = userProfile.calorieTarget,
                        streaks = streaks,
                        profile = userProfile,
                        weightLogs = allWeightLogs,
                        recentDailyLogs = recentDailyLogs,
                        historyLogs = historyLogs,
                        challenges = challenges,
                        onOpenLogWeightDialog = { showLogWeightDialog = true },
                        onCheckInChallenge = { viewModel.checkInChallenge(it) },
                        onToggleJoinChallenge = { viewModel.toggleJoinChallenge(it) }
                    )
                }
                MainTab.COACH -> {
                    CoachScreen(
                        messages = chatMessages,
                        isTyping = isCoachTyping,
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        aiConfigured = viewModel.aiConfigured
                    )
                }
            }
            }
        }
    }

    // Dialogs & Modals
    if (showAddMealDialog) {
        AddMealDialog(
            mealType = selectedMealType,
            onDismiss = { viewModel.closeAddMeal() },
            onAddMeal = { name, qty, unit, cal, prot, carbs, fat, fiber ->
                viewModel.addMeal(name, qty, unit, cal, prot, carbs, fat, fiber)
            },
            recentFoods = recentFoods,
            onAddRecent = { viewModel.addRecentFood(it) },
            barcodeAvailable = barcodeScanner.isAvailable,
            onScanBarcode = { barcodeScanner.scan() },
            vegOnly = userProfile?.dietPreference in setOf("VEGETARIAN", "JAIN", "VEGAN")
        )
    }

    BarcodeProductDialog(
        state = barcodeState,
        initialMealType = if (showAddMealDialog) selectedMealType else mealTypeForNow(),
        onAdd = { product, grams, mealType -> viewModel.addBarcodeProduct(product, grams, mealType) },
        onScanAgain = {
            viewModel.dismissBarcode()
            barcodeScanner.scan()
        },
        onDismiss = { viewModel.dismissBarcode() },
        aiAvailable = viewModel.aiConfigured,
        onEstimateWithAi = { viewModel.estimateBarcodeWithAi() }
    )

    if (showAiScanModalManual || aiScanUiState !is AiScanUiState.Idle) {
        AiFoodScanDialog(
            uiState = aiScanUiState,
            onDismiss = {
                showAiScanModalManual = false
                viewModel.resetAiScan()
            },
            onScanPhoto = { viewModel.scanFoodPhoto(it) },
            onAddManually = {
                showAiScanModalManual = false
                viewModel.resetAiScan()
                viewModel.openAddMeal()
            },
            onAddToDiary = { mealType, result ->
                viewModel.addScannedMealToDiary(mealType, result)
                showAiScanModalManual = false
            }
        )
    }

    if (activeWorkout != null) {
        WorkoutRunnerDialog(
            workout = activeWorkout!!,
            currentExerciseIndex = currentExerciseIndex,
            remainingSeconds = exerciseRemainingSeconds,
            isPaused = isWorkoutPaused,
            onTogglePause = { viewModel.toggleWorkoutPause() },
            onNextExercise = { viewModel.nextExercise() },
            onCancel = { viewModel.cancelWorkout() }
        )
    }

    if (showProDialog) {
        ProUpgradeDialog(
            isAlreadyPro = userProfile.isPro,
            onDismiss = { viewModel.closeProDialog() },
            onActivatePro = { viewModel.activatePro() }
        )
    }

    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
    if (needsOnboarding == true) {
        ProfileEditDialog(
            profile = userProfile,
            isFirstSetup = true,
            onDismiss = {},
            onSave = { viewModel.updateProfile(it) },
            backupMessage = backupMessage,
            onRestore = { backupFiles.pick() },
            onEnableReminders = { viewModel.requestNotificationPermission() }
        )
    } else if (showProfileDialog) {
        ProfileEditDialog(
            profile = userProfile,
            onDismiss = { viewModel.closeProfileDialog() },
            onSave = { viewModel.updateProfile(it) },
            backupMessage = backupMessage,
            onBackup = { viewModel.createBackup { name, content -> backupFiles.save(name, content) } },
            onRestore = { backupFiles.pick() },
            onEnableReminders = { viewModel.requestNotificationPermission() },
            autoBackup = viewModel.autoBackup
        )
    }

    if (showLogWeightDialog) {
        LogWeightDialog(
            initialWeight = userProfile.currentWeightKg,
            onDismiss = { showLogWeightDialog = false },
            onConfirm = { w, note ->
                viewModel.logWeight(w, note)
                showLogWeightDialog = false
            }
        )
    }
}

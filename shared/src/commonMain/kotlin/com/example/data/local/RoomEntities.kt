package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.example.util.currentTimeMillis

@Serializable
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int = 0,
    val waterMl: Int = 0,
    val caloriesBurned: Int = 0,
    val score: Int = 0,
    val note: String = ""
)

@Serializable
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // BREAKFAST, LUNCH, SNACKS, DINNER
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val loggedAt: Long = currentTimeMillis(),
    /** JSON list of [com.example.data.model.MealItem] for meals made of several foods; empty otherwise. */
    @ColumnInfo(defaultValue = "") val itemsJson: String = ""
)

@Serializable
@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val weightKg: Double,
    val note: String = "",
    val loggedAt: Long = currentTimeMillis()
)

@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val gender: String = "Male",
    val age: Int = 28,
    val heightCm: Double = 170.0,
    val startWeightKg: Double = 75.0,
    val currentWeightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val calorieTarget: Int = 1850,
    val stepGoal: Int = 7500,
    val waterGoalMl: Int = 2500,
    val dietPreference: String = "VEGETARIAN",
    val isPro: Boolean = false,
    @ColumnInfo(defaultValue = "LOSE") val goalType: String = "LOSE",
    @ColumnInfo(defaultValue = "LIGHT") val activityLevel: String = "LIGHT",
    @ColumnInfo(defaultValue = "0.5") val weeklyRateKg: Double = 0.5,
    @ColumnInfo(defaultValue = "0") val isOnboarded: Boolean = false,
    /** "preset:NN" for a bundled avatar, "photo" for [avatarPhoto], empty for the default. */
    @ColumnInfo(defaultValue = "") val avatar: String = "",
    /** The user's own picture as a small Base64 JPEG; empty when not set. */
    @ColumnInfo(defaultValue = "") val avatarPhoto: String = "",
    // Reminder switches (notifications)
    @ColumnInfo(defaultValue = "0") val remindWater: Boolean = false,
    @ColumnInfo(defaultValue = "0") val remindMeals: Boolean = false,
    @ColumnInfo(defaultValue = "0") val remindWalk: Boolean = false,
    /** JSON of [com.example.data.model.ReminderTimes]; empty means the defaults. */
    @ColumnInfo(defaultValue = "") val reminderTimes: String = "",
    /** JSON of [com.example.data.model.WidgetConfig]; empty means the defaults. */
    @ColumnInfo(defaultValue = "") val widgetConfig: String = "",
    /** App theme: "SYSTEM", "LIGHT" or "DARK". */
    @ColumnInfo(defaultValue = "SYSTEM") val themeMode: String = "SYSTEM",
    /** JSON of [com.example.data.model.AiDietPlan] made by Gemini; empty when none. */
    @ColumnInfo(defaultValue = "") val aiDietPlan: String = "",
    /** Comma-separated ids of badges the user has already been congratulated on; empty = not started. */
    @ColumnInfo(defaultValue = "") val seenBadges: String = ""
)

@Serializable
@Entity(tableName = "challenge_progress")
data class ChallengeProgressEntity(
    @PrimaryKey val challengeId: String,
    val daysCompleted: Int = 0,
    val isJoined: Boolean = true,
    val isCompleted: Boolean = false,
    val lastCheckInDate: String = ""
)

/** Per-day food totals, used to work out calorie / protein / logging streaks. */
data class DailyMealTotals(
    val date: String,
    val calories: Int,
    val protein: Double
)

/** One completed day of the 4-week workout program; the id looks like "w2d3". */
@Serializable
@Entity(tableName = "workout_progress")
data class WorkoutProgressEntity(
    @PrimaryKey val dayId: String,
    val completedAt: Long = currentTimeMillis()
)

/** One intermittent fast; [endMillis] is null while it is running. */
@Serializable
@Entity(tableName = "fasting_sessions")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val targetHours: Int
)

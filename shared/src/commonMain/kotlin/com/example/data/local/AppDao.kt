package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun getDailyLog(date: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyLogs(limit: Int): Flow<List<DailyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyLog(log: DailyLogEntity)

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY loggedAt ASC")
    fun getMealsForDate(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY loggedAt DESC LIMIT 200")
    fun getRecentMeals(): Flow<List<MealEntity>>

    @Query("SELECT date, SUM(calories) AS calories, SUM(proteinG) AS protein FROM meals GROUP BY date")
    fun getDailyMealTotals(): Flow<List<DailyMealTotals>>

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, loggedAt ASC")
    fun getMealsInRange(startDate: String, endDate: String): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM weight_logs ORDER BY date ASC, loggedAt ASC")
    fun getAllWeightLogs(): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLog(id: Long)

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM challenge_progress")
    fun getAllChallengeProgress(): Flow<List<ChallengeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChallenge(progress: ChallengeProgressEntity)

    @Query("SELECT * FROM workout_progress")
    fun getWorkoutProgress(): Flow<List<WorkoutProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutProgress(progress: WorkoutProgressEntity)

    // Backup / restore
    @Query("SELECT * FROM daily_logs") suspend fun allDailyLogsOnce(): List<DailyLogEntity>
    @Query("SELECT * FROM meals") suspend fun allMealsOnce(): List<MealEntity>
    @Query("SELECT * FROM weight_logs") suspend fun allWeightLogsOnce(): List<WeightLogEntity>
    @Query("SELECT * FROM challenge_progress") suspend fun allChallengesOnce(): List<ChallengeProgressEntity>
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1") suspend fun profileOnce(): UserProfileEntity?

    @Query("DELETE FROM daily_logs") suspend fun clearDailyLogs()
    @Query("DELETE FROM meals") suspend fun clearMeals()
    @Query("DELETE FROM weight_logs") suspend fun clearWeightLogs()
    @Query("DELETE FROM challenge_progress") suspend fun clearChallenges()
    @Query("SELECT * FROM workout_progress") suspend fun allWorkoutProgressOnce(): List<WorkoutProgressEntity>
    @Query("DELETE FROM workout_progress") suspend fun clearWorkoutProgress()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkoutProgressList(items: List<WorkoutProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDailyLogs(logs: List<DailyLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMeals(meals: List<MealEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWeightLogs(logs: List<WeightLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertChallenges(items: List<ChallengeProgressEntity>)
}

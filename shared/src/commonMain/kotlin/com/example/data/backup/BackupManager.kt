package com.example.data.backup

import com.example.data.local.AppDao
import com.example.data.local.ChallengeProgressEntity
import com.example.data.local.DailyLogEntity
import com.example.data.local.MealEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.WeightLogEntity
import com.example.data.local.WorkoutProgressEntity
import com.example.util.currentTimeMillis
import com.example.util.toKey
import com.example.util.todayDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the user has entered, in one portable file that works across Android and iOS. */
@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val formatVersion: Int = FORMAT_VERSION,
    val createdAtMillis: Long,
    val profile: UserProfileEntity?,
    val dailyLogs: List<DailyLogEntity>,
    val meals: List<MealEntity>,
    val weightLogs: List<WeightLogEntity>,
    val challenges: List<ChallengeProgressEntity>,
    val workoutProgress: List<WorkoutProgressEntity> = emptyList(),
    val fastingSessions: List<com.example.data.local.FastingSessionEntity> = emptyList()
) {
    companion object {
        const val APP_ID = "fitbharat"
        const val FORMAT_VERSION = 1
    }
}

class BackupException(message: String) : Exception(message)

class BackupManager(private val dao: AppDao) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun suggestedFileName(): String = "fitbharat-backup-${todayDate().toKey()}.json"

    suspend fun export(): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            createdAtMillis = currentTimeMillis(),
            profile = dao.profileOnce(),
            dailyLogs = dao.allDailyLogsOnce(),
            meals = dao.allMealsOnce(),
            weightLogs = dao.allWeightLogsOnce(),
            challenges = dao.allChallengesOnce(),
            workoutProgress = dao.allWorkoutProgressOnce(),
            fastingSessions = dao.allFastsOnce()
        )
    )

    /**
     * Replaces all current data with the contents of [content]. The file is fully parsed and validated
     * before anything is deleted, so a bad file never wipes existing data.
     */
    suspend fun restore(content: String) {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), content)
        } catch (e: Exception) {
            throw BackupException("This file is not a valid FitBharat backup.")
        }
        if (backup.app != BackupFile.APP_ID) throw BackupException("This file is not a FitBharat backup.")
        if (backup.formatVersion > BackupFile.FORMAT_VERSION) {
            throw BackupException("This backup was created by a newer version of FitBharat. Please update the app.")
        }
        val profile = backup.profile ?: throw BackupException("The backup does not contain a profile.")

        dao.clearDailyLogs()
        dao.clearMeals()
        dao.clearWeightLogs()
        dao.clearChallenges()
        dao.clearWorkoutProgress()
        dao.clearFasts()
        dao.insertOrUpdateProfile(profile.copy(isOnboarded = true))
        dao.insertDailyLogs(backup.dailyLogs)
        dao.insertMeals(backup.meals)
        dao.insertWeightLogs(backup.weightLogs)
        dao.insertChallenges(backup.challenges)
        dao.insertWorkoutProgressList(backup.workoutProgress)
        dao.insertFasts(backup.fastingSessions)
    }
}

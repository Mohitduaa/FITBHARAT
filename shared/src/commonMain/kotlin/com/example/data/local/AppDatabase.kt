package com.example.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

const val DATABASE_NAME = "losemate_fitdesi.db"

@Database(
    entities = [
        DailyLogEntity::class,
        MealEntity::class,
        WeightLogEntity::class,
        UserProfileEntity::class,
        ChallengeProgressEntity::class,
        WorkoutProgressEntity::class
    ],
    version = 9,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}

// Room generates the actual implementation for each platform.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** v2: goal type, activity level, weekly pace and onboarding flag on the profile. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN goalType TEXT NOT NULL DEFAULT 'LOSE'")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN activityLevel TEXT NOT NULL DEFAULT 'LIGHT'")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN weeklyRateKg REAL NOT NULL DEFAULT 0.5")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN isOnboarded INTEGER NOT NULL DEFAULT 0")
    }
}

/** v3: completed days of the workout program. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `workout_progress` (`dayId` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, PRIMARY KEY(`dayId`))")
    }
}

/** v4: avatar choice and the user's own avatar photo. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN avatar TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN avatarPhoto TEXT NOT NULL DEFAULT ''")
    }
}

/** v5: reminder switches. */
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN widgetConfig TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN reminderTimes TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE meals ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN remindWater INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN remindMeals INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE user_profile ADD COLUMN remindWalk INTEGER NOT NULL DEFAULT 0")
    }
}

fun RoomDatabase.Builder<AppDatabase>.configure(): AppDatabase =
    addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(databaseDispatcher())
        .build()

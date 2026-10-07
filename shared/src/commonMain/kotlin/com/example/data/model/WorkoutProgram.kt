package com.example.data.model

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    ABS("Abs"),
    ARMS("Arms"),
    LEGS("Legs"),
    BACK_SHOULDERS("Back & Shoulders"),
    FULL_BODY("Full Body"),
    REST("Recovery")
}

data class ProgramDay(
    val id: String,
    val week: Int,
    val day: Int,
    val focus: MuscleGroup,
    val exercises: List<WorkoutExercise>
) {
    val isRecovery: Boolean get() = focus == MuscleGroup.REST

    /** Time on the clock: work intervals plus a short rest between exercises. */
    val durationMinutes: Int
        get() = ((exercises.sumOf { it.durationSeconds } + exercises.size * REST_SECONDS) / 60.0).let { kotlin.math.ceil(it).toInt() }

    /** Estimated burn using MET values (bodyweight training ~5, stretching ~2.3). */
    fun estimatedCalories(weightKg: Double): Int {
        val met = if (isRecovery) 2.3 else 5.0
        val hours = (exercises.sumOf { it.durationSeconds } + exercises.size * REST_SECONDS) / 3600.0
        return (met * weightKg * hours).toInt()
    }

    /** A runnable routine for the existing workout player. */
    fun toRoutine(weightKg: Double): WorkoutRoutine = WorkoutRoutine(
        id = id,
        title = "Week $week · Day $day: ${focus.label}",
        hindiTitle = "",
        durationMinutes = durationMinutes,
        caloriesBurned = estimatedCalories(weightKg),
        level = if (isRecovery) "Easy" else "Week $week intensity",
        equipment = "No equipment",
        description = focus.label,
        iconName = "fitness_center",
        exercises = exercises
    )

    companion object {
        const val REST_SECONDS = 15
    }
}

data class ProgramWeek(val number: Int, val days: List<ProgramDay>)

/**
 * A 4-week, no-equipment home program. Six training days (chest, abs, arms, legs, back & shoulders,
 * full body) and one recovery day per week. Work time per exercise grows each week, and the exercise
 * selection rotates so weeks do not repeat.
 */
object WorkoutProgram {
    const val WEEKS = 4
    const val DAYS_PER_WEEK = 7
    const val TOTAL_DAYS = WEEKS * DAYS_PER_WEEK

    private val schedule = listOf(
        MuscleGroup.CHEST, MuscleGroup.ABS, MuscleGroup.ARMS, MuscleGroup.LEGS,
        MuscleGroup.BACK_SHOULDERS, MuscleGroup.FULL_BODY, MuscleGroup.REST
    )

    private fun exercisesFor(focus: MuscleGroup, week: Int): List<WorkoutExercise> {
        val templates = exerciseLibrary.getValue(focus)
        val workSeconds = if (focus == MuscleGroup.REST) 30 else 30 + 5 * (week - 1)
        val count = minOf(8, templates.size)
        // Rotate the starting point each week so the selection changes.
        val offset = ((week - 1) * 2) % templates.size
        return (0 until count).map { i ->
            val t = templates[(offset + i) % templates.size]
            WorkoutExercise(
                name = t.name,
                hindiName = "",
                imageId = t.imageId,
                steps = t.steps,
                muscles = t.muscles,
                durationSeconds = workSeconds,
                repsOrDetails = t.reps,
                description = t.description,
                tips = t.tip
            )
        }
    }

    val weeks: List<ProgramWeek> = (1..WEEKS).map { week ->
        ProgramWeek(
            number = week,
            days = schedule.mapIndexed { index, focus ->
                val day = index + 1
                ProgramDay(id = dayId(week, day), week = week, day = day, focus = focus, exercises = exercisesFor(focus, week))
            }
        )
    }

    fun dayId(week: Int, day: Int) = "w${week}d$day"

    fun isProgramDay(id: String) = id.length == 4 && id[0] == 'w' && id[2] == 'd'

    /** The first day that has not been completed yet, or null when the whole program is done. */
    fun nextDay(completed: Set<String>): ProgramDay? =
        weeks.flatMap { it.days }.firstOrNull { it.id !in completed }
}

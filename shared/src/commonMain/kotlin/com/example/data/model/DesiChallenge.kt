package com.example.data.model

/** A daily target that can be verified from the app's own data instead of an honour-system tap. */
sealed interface ChallengeRequirement {
    data class Steps(val minimum: Int) : ChallengeRequirement
    data class WaterMl(val minimum: Int) : ChallengeRequirement
}

data class DesiChallenge(
    val id: String,
    val title: String,
    val totalDays: Int,
    val description: String,
    val dailyAction: String,
    /** When set, today's check-in unlocks only after this is met. */
    val requirement: ChallengeRequirement? = null,
    val daysCompleted: Int = 0,
    val isJoined: Boolean = false,
    val isCompleted: Boolean = false,
    val checkedInToday: Boolean = false
)

object ChallengeDatabase {
    val defaultChallenges = listOf(
        DesiChallenge(
            id = "no_sugar_7",
            title = "7-Day Sugar-Free",
            totalDays = 7,
            description = "Cut out refined sugar, sweets, sodas and biscuits for 7 days in a row to reset cravings.",
            dailyAction = "Tea or coffee without sugar; have fruit or cinnamon if a craving hits."
        ),
        DesiChallenge(
            id = "steps_10k_14",
            title = "14-Day 8,000 Steps",
            totalDays = 14,
            description = "Walk at least 8,000 steps every day for 2 weeks. Everyday movement burns more than most people expect.",
            dailyAction = "Take walking calls, use the stairs and walk 15 minutes after dinner.",
            requirement = ChallengeRequirement.Steps(8_000)
        ),
        DesiChallenge(
            id = "water_2500_7",
            title = "7-Day 2.5 L Water",
            totalDays = 7,
            description = "Thirst is often mistaken for hunger. Drink at least 2.5 litres every day for a week.",
            dailyAction = "Log at least 2,500 ml of water before 9 PM.",
            requirement = ChallengeRequirement.WaterMl(2_500)
        ),
        DesiChallenge(
            id = "salad_first_21",
            title = "21-Day Salad Before Lunch",
            totalDays = 21,
            description = "Eat a plate of cucumber, tomato or carrot salad before your main meal. Easy portion control.",
            dailyAction = "Finish a bowl of raw vegetables 10 minutes before lunch."
        ),
        DesiChallenge(
            id = "dinner_by_830_30",
            title = "30-Day Early Dinner",
            totalDays = 30,
            description = "Finish dinner at least 2.5 hours before bed for better digestion and sleep.",
            dailyAction = "Finish dinner by 8:30 PM."
        )
    )
}

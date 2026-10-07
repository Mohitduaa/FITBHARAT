package com.example.data.model

import com.example.util.currentTimeMillis
import kotlin.random.Random

data class ChatMessage(
    val id: String = "${currentTimeMillis()}-${Random.nextLong().toULong().toString(16)}",
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = currentTimeMillis(),
    val suggestions: List<String> = emptyList()
)

object CoachPromptSuggestions {
    val starterQuestions = listOf(
        "How do I stay on track at weddings and parties?",
        "What are healthy options for late-night cravings?",
        "Roti or rice: which is better for my goal?",
        "How can I reach my protein target on a vegetarian diet?",
        "How do I get started at home without a gym?"
    )
}

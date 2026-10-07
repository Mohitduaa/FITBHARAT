package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
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

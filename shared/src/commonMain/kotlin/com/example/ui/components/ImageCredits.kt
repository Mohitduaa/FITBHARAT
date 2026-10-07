package com.example.ui.components

/**
 * Attribution for exercise photos taken from Wikimedia Commons (CC BY / CC BY-SA require credit).
 * Keyed by image id; photos from free-exercise-db are public domain and need none.
 */
private val CREDITS: Map<String, String> = mapOf(
    "wm_burpee" to "Taco fleur, CC BY-SA 4.0, via Wikimedia Commons",
    "wm_bird_dog" to "PTPioneer, CC BY 2.0, via Wikimedia Commons"
)

fun exerciseImageCredit(imageId: String): String? = CREDITS[imageId]

/** Every credit, for the About / licences screen. */
val allImageCredits: Map<String, String> get() = CREDITS

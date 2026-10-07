package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the three bars of the home screen widget can show. */
enum class WidgetMetric(val label: String) {
    STEPS("Steps"),
    WATER("Water"),
    PROTEIN("Protein"),
    BURNED("Burned"),
    CARBS("Carbs"),
    FAT("Fat")
}

/** Home screen widget choices, saved with the profile. */
@Serializable
data class WidgetConfig(
    /** "DARK" or "LIGHT". */
    val theme: String = "DARK",
    /** Ring shows calories "LEFT" or calories "EATEN". */
    val ring: String = "LEFT",
    val rows: List<String> = listOf(WidgetMetric.STEPS.name, WidgetMetric.WATER.name, WidgetMetric.PROTEIN.name)
) {
    val isLight: Boolean get() = theme == "LIGHT"
    val showsEaten: Boolean get() = ring == "EATEN"

    /** Always exactly three valid metrics, padded with the defaults. */
    val metrics: List<WidgetMetric>
        get() = (rows.mapNotNull { name -> WidgetMetric.entries.firstOrNull { it.name == name } } +
            listOf(WidgetMetric.STEPS, WidgetMetric.WATER, WidgetMetric.PROTEIN)).distinct().take(3)

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): WidgetConfig =
            if (text.isBlank()) WidgetConfig() else runCatching { json.decodeFromString<WidgetConfig>(text) }.getOrDefault(WidgetConfig())
    }
}

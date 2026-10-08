package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** User-chosen reminder times. All times are minutes after midnight. */
@Serializable
data class ReminderTimes(
    val waterEveryHours: Int = 2,
    val waterStart: Int = 9 * 60,
    val waterEnd: Int = 21 * 60,
    val lunch: Int = 14 * 60,
    val dinner: Int = 21 * 60,
    val walk: Int = 18 * 60 + 30,
    /** Sunday 7 PM summary of the week. */
    val weeklyReport: Boolean = false,
    /** Skip a reminder when it is already done (lunch logged, enough steps, water goal met). */
    val smart: Boolean = true,
    /** The walk reminder only fires below this many steps. */
    val walkMinSteps: Int = 3000
) {
    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun fromJson(text: String): ReminderTimes =
            if (text.isBlank()) ReminderTimes() else runCatching { json.decodeFromString<ReminderTimes>(text) }.getOrDefault(ReminderTimes())
    }
}

/** "1:30 PM" style label for minutes after midnight. */
fun formatClock(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}

package com.example.platform

import com.example.data.model.buildWidgetSnapshot
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/**
 * Keeps the home screen widget's data up to date: whenever meals, steps, water or the profile change, the
 * latest snapshot is written into the App Group the widget extension reads. The Swift shell asks WidgetKit to
 * redraw when the app goes to the background.
 */
object IosWidgetBridge {
    private const val DEFAULT_GROUP = "group.com.fitbharat.app"
    const val SNAPSHOT_KEY = "fb_widget_snapshot"

    /** AltStore may register the group under a different id and lists the real one in ALTAppGroups. */
    fun appGroupId(): String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("ALTAppGroups") as? List<*>)
            ?.firstOrNull() as? String ?: DEFAULT_GROUP

    @OptIn(FlowPreview::class)
    suspend fun keepUpdated(repository: FitnessRepository) {
        val shared = NSUserDefaults(suiteName = appGroupId())
        combine(repository.userProfile, repository.getMealsForToday(), repository.getTodayLog()) { profile, meals, log ->
            buildWidgetSnapshot(profile, meals, log)
        }
            .debounce(500)
            .collect { snapshot -> shared.setObject(snapshot.toJson(), forKey = SNAPSHOT_KEY) }
    }
}

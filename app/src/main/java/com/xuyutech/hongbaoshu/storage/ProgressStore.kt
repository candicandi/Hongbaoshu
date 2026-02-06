package com.xuyutech.hongbaoshu.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "reading_progress"

val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

data class ProgressState(
    val chapterIndex: Int = 0,
    val pageIndex: Int = 0,
    val narrationSentenceId: String? = null,
    val narrationPosition: Long = 0L,
    val isNightMode: Boolean = false,
    val hasShownMenuGuide: Boolean = false,
    val narrationSpeed: Float = 1.0f
)

class ProgressStore(private val context: Context) {

    private fun keyChapter(packId: String) = intPreferencesKey("${packId}_chapter_index")
    private fun keyPage(packId: String) = intPreferencesKey("${packId}_page_index")
    private fun keyNarration(packId: String) = stringPreferencesKey("${packId}_narration_sentence_id")
    private fun keyNarrationPos(packId: String) = longPreferencesKey("${packId}_narration_position")
    private fun keyIsNightMode(packId: String) = booleanPreferencesKey("${packId}_is_night_mode")
    private fun keyHasShownMenuGuide(packId: String) = booleanPreferencesKey("${packId}_has_shown_menu_guide")
    private fun keyNarrationSpeed(packId: String) = floatPreferencesKey("${packId}_narration_speed")

    // Legacy keys (v1 single-book).
    private val legacyKeyChapter = intPreferencesKey("chapter_index")
    private val legacyKeyPage = intPreferencesKey("page_index")
    private val legacyKeyNarration = stringPreferencesKey("narration_sentence_id")
    private val legacyKeyNarrationPos = longPreferencesKey("narration_position")
    private val legacyKeyIsNightMode = booleanPreferencesKey("is_night_mode")
    private val legacyKeyHasShownMenuGuide = booleanPreferencesKey("has_shown_menu_guide")
    private val legacyKeyNarrationSpeed = floatPreferencesKey("narration_speed")

    fun progress(packId: String): Flow<ProgressState> = context.progressDataStore.data.map { prefs ->
        // builtin: allow fallback to legacy keys to keep existing users' progress.
        val fallbackLegacy = packId == "builtin" && prefs.contains(legacyKeyChapter)

        val chapterIndex = prefs[keyChapter(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyChapter] ?: 0) else 0
        val pageIndex = prefs[keyPage(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyPage] ?: 0) else 0
        val narrationSentenceId = prefs[keyNarration(packId)] ?: if (fallbackLegacy) prefs[legacyKeyNarration] else null
        val narrationPosition = prefs[keyNarrationPos(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyNarrationPos] ?: 0L) else 0L
        val isNightMode = prefs[keyIsNightMode(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyIsNightMode] ?: false) else false
        val hasShownMenuGuide = prefs[keyHasShownMenuGuide(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyHasShownMenuGuide] ?: false) else false
        val narrationSpeed = prefs[keyNarrationSpeed(packId)] ?: if (fallbackLegacy) (prefs[legacyKeyNarrationSpeed] ?: 1.0f) else 1.0f

        ProgressState(
            chapterIndex = chapterIndex,
            pageIndex = pageIndex,
            narrationSentenceId = narrationSentenceId,
            narrationPosition = narrationPosition,
            isNightMode = isNightMode,
            hasShownMenuGuide = hasShownMenuGuide,
            narrationSpeed = narrationSpeed
        )
    }

    suspend fun save(packId: String, state: ProgressState) {
        context.progressDataStore.edit { prefs ->
            prefs[keyChapter(packId)] = state.chapterIndex
            prefs[keyPage(packId)] = state.pageIndex
            if (state.narrationSentenceId == null) {
                prefs.remove(keyNarration(packId))
                prefs.remove(keyNarrationPos(packId))
            } else {
                prefs[keyNarration(packId)] = state.narrationSentenceId
                prefs[keyNarrationPos(packId)] = state.narrationPosition
            }
            prefs[keyIsNightMode(packId)] = state.isNightMode
            prefs[keyHasShownMenuGuide(packId)] = state.hasShownMenuGuide
            prefs[keyNarrationSpeed(packId)] = state.narrationSpeed
        }
    }

    suspend fun clear() {
        context.progressDataStore.edit { it.clear() }
    }

    /** 检查是否有保存的阅读进度（章节 > 0 或页码 > 0） */
    val hasProgress: Flow<Boolean> = context.progressDataStore.data.map { prefs ->
        (prefs[legacyKeyChapter] ?: 0) > 0 || (prefs[legacyKeyPage] ?: 0) > 0
    }
}

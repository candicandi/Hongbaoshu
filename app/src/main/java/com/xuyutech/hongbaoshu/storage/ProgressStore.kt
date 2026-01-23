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
    val hasShownToolbarHint: Boolean = false,
    val narrationSpeed: Float = 1.0f
)

class ProgressStore(private val context: Context) {

    private val keyChapter = intPreferencesKey("chapter_index")
    private val keyPage = intPreferencesKey("page_index")
    private val keyNarration = stringPreferencesKey("narration_sentence_id")
    private val keyNarrationPos = longPreferencesKey("narration_position")
    private val keyIsNightMode = booleanPreferencesKey("is_night_mode")
    private val keyHasShownMenuGuide = booleanPreferencesKey("has_shown_menu_guide")
    private val keyHasShownToolbarHint = booleanPreferencesKey("has_shown_toolbar_hint")
    private val keyNarrationSpeed = floatPreferencesKey("narration_speed")

    val progress: Flow<ProgressState> = context.progressDataStore.data.map { prefs ->
        ProgressState(
            chapterIndex = prefs[keyChapter] ?: 0,
            pageIndex = prefs[keyPage] ?: 0,
            narrationSentenceId = prefs[keyNarration],
            narrationPosition = prefs[keyNarrationPos] ?: 0L,
            isNightMode = prefs[keyIsNightMode] ?: false,
            hasShownMenuGuide = prefs[keyHasShownMenuGuide] ?: false,
            hasShownToolbarHint = prefs[keyHasShownToolbarHint] ?: false,
            narrationSpeed = prefs[keyNarrationSpeed] ?: 1.0f
        )
    }

    suspend fun save(state: ProgressState) {
        context.progressDataStore.edit { prefs ->
            prefs[keyChapter] = state.chapterIndex
            prefs[keyPage] = state.pageIndex
            if (state.narrationSentenceId == null) {
                prefs.remove(keyNarration)
                prefs.remove(keyNarrationPos)
            } else {
                prefs[keyNarration] = state.narrationSentenceId
                prefs[keyNarrationPos] = state.narrationPosition
            }
            prefs[keyIsNightMode] = state.isNightMode
            prefs[keyHasShownMenuGuide] = state.hasShownMenuGuide
            prefs[keyHasShownToolbarHint] = state.hasShownToolbarHint
            prefs[keyNarrationSpeed] = state.narrationSpeed
        }
    }

    suspend fun clear() {
        context.progressDataStore.edit { it.clear() }
    }

    /** 检查是否有保存的阅读进度（章节 > 0 或页码 > 0） */
    val hasProgress: Flow<Boolean> = context.progressDataStore.data.map { prefs ->
        (prefs[keyChapter] ?: 0) > 0 || (prefs[keyPage] ?: 0) > 0
    }
}

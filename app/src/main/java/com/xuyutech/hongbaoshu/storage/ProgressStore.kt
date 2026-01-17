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
    val bgmIndex: Int = 0,
    val bgmEnabled: Boolean = false,
    val bgmVolume: Float = 1.0f,
    val isNightMode: Boolean = false,
    val hasShownMenuGuide: Boolean = false
)

class ProgressStore(private val context: Context) {

    private val keyChapter = intPreferencesKey("chapter_index")
    private val keyPage = intPreferencesKey("page_index")
    private val keyNarration = stringPreferencesKey("narration_sentence_id")
    private val keyNarrationPos = longPreferencesKey("narration_position")
    private val keyBgmIndex = intPreferencesKey("bgm_index")
    private val keyBgmEnabled = booleanPreferencesKey("bgm_enabled")
    private val keyBgmVolume = floatPreferencesKey("bgm_volume")
    private val keyIsNightMode = booleanPreferencesKey("is_night_mode")
    private val keyHasShownMenuGuide = booleanPreferencesKey("has_shown_menu_guide")

    val progress: Flow<ProgressState> = context.progressDataStore.data.map { prefs ->
        ProgressState(
            chapterIndex = prefs[keyChapter] ?: 0,
            pageIndex = prefs[keyPage] ?: 0,
            narrationSentenceId = prefs[keyNarration],
            narrationPosition = prefs[keyNarrationPos] ?: 0L,
            bgmIndex = prefs[keyBgmIndex] ?: 0,
            bgmEnabled = prefs[keyBgmEnabled] ?: false,
            bgmVolume = prefs[keyBgmVolume] ?: 1.0f,
            isNightMode = prefs[keyIsNightMode] ?: false,
            hasShownMenuGuide = prefs[keyHasShownMenuGuide] ?: false
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
            prefs[keyBgmIndex] = state.bgmIndex
            prefs[keyBgmEnabled] = state.bgmEnabled
            prefs[keyBgmVolume] = state.bgmVolume
            prefs[keyIsNightMode] = state.isNightMode
            prefs[keyHasShownMenuGuide] = state.hasShownMenuGuide
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

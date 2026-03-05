package com.xuyutech.hongbaoshu.pack.index

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xuyutech.hongbaoshu.pack.model.PackIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "pack_index"
private val Context.packIndexDataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

class PackIndexStore(
    private val context: Context
) {
    private val keyIndexes = stringPreferencesKey("pack_indexes_json")

    val packs: Flow<List<PackIndex>> = context.packIndexDataStore.data.map { prefs ->
        PackIndexCodec.decode(prefs[keyIndexes])
    }

    suspend fun find(packId: String): PackIndex? {
        return packs.first().firstOrNull { it.packId == packId }
    }

    suspend fun upsert(pack: PackIndex) {
        context.packIndexDataStore.edit { prefs ->
            val current = readCurrent(prefs)
            val next = current.filterNot { it.packId == pack.packId } + pack
            prefs[keyIndexes] = PackIndexCodec.encode(next.sortedByDescending { it.lastOpenedAt ?: it.importedAt })
        }
    }

    suspend fun delete(packId: String) {
        context.packIndexDataStore.edit { prefs ->
            val current = readCurrent(prefs)
            prefs[keyIndexes] = PackIndexCodec.encode(current.filterNot { it.packId == packId })
        }
    }

    suspend fun markOpened(packId: String, openedAt: Long = System.currentTimeMillis()) {
        context.packIndexDataStore.edit { prefs ->
            val current = readCurrent(prefs)
            val next = current.map { p ->
                if (p.packId == packId) p.copy(lastOpenedAt = openedAt) else p
            }
            prefs[keyIndexes] = PackIndexCodec.encode(next.sortedByDescending { it.lastOpenedAt ?: it.importedAt })
        }
    }

    private fun readCurrent(prefs: Preferences): List<PackIndex> {
        return PackIndexCodec.decode(prefs[keyIndexes])
    }
}

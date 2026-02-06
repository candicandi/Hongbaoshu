package com.xuyutech.hongbaoshu.reader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.storage.PageCacheStore
import com.xuyutech.hongbaoshu.storage.ProgressStore
import com.xuyutech.hongbaoshu.audio.AudioManager

class ReaderViewModelFactory(
    private val app: Application,
    private val packId: String,
    private val loader: ContentLoader,
    private val progressStore: ProgressStore,
    private val audioManager: AudioManager,
    private val pageCacheStore: PageCacheStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(app, packId, loader, progressStore, audioManager, pageCacheStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

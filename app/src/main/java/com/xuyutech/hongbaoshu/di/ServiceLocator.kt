package com.xuyutech.hongbaoshu.di

import android.content.Context
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.audio.AudioManagerImpl
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.data.ContentLoaderImpl
import com.xuyutech.hongbaoshu.pack.index.PackIndexStore
import com.xuyutech.hongbaoshu.pack.importer.ZipPackImporter
import com.xuyutech.hongbaoshu.pack.storage.PackFileStore
import com.xuyutech.hongbaoshu.storage.PageCacheStore
import com.xuyutech.hongbaoshu.storage.ProgressStore

/**
 * Lightweight service locator to wire core singletons.
 * Replace with DI framework (e.g., Hilt) later if需要。
 */
object ServiceLocator {
    @Volatile
    private var loader: ContentLoader? = null

    @Volatile
    private var audio: AudioManager? = null

    @android.annotation.SuppressLint("StaticFieldLeak") // Holds ApplicationContext, safe
    @Volatile
    private var progressStore: ProgressStore? = null
    
    @android.annotation.SuppressLint("StaticFieldLeak") // Holds ApplicationContext, safe
    @Volatile
    private var pageCacheStore: PageCacheStore? = null

    @android.annotation.SuppressLint("StaticFieldLeak") // Holds ApplicationContext, safe
    @Volatile
    private var packIndexStore: PackIndexStore? = null

    @android.annotation.SuppressLint("StaticFieldLeak") // Holds ApplicationContext, safe
    @Volatile
    private var packFileStore: PackFileStore? = null

    @android.annotation.SuppressLint("StaticFieldLeak") // Holds ApplicationContext, safe
    @Volatile
    private var packImporter: ZipPackImporter? = null

    fun provideContentLoader(): ContentLoader =
        loader ?: synchronized(this) {
            loader ?: ContentLoaderImpl().also { loader = it }
        }

    fun provideAudioManager(context: Context): AudioManager =
        audio ?: synchronized(this) {
            audio ?: AudioManagerImpl(context.applicationContext, provideContentLoader()).also {
                audio = it
            }
        }

    fun provideProgressStore(context: Context): ProgressStore =
        progressStore ?: synchronized(this) {
            progressStore ?: ProgressStore(context.applicationContext).also { progressStore = it }
        }
    
    fun providePageCacheStore(context: Context): PageCacheStore =
        pageCacheStore ?: synchronized(this) {
            pageCacheStore ?: PageCacheStore(context.applicationContext).also { pageCacheStore = it }
        }

    fun providePackIndexStore(context: Context): PackIndexStore =
        packIndexStore ?: synchronized(this) {
            packIndexStore ?: PackIndexStore(context.applicationContext).also { packIndexStore = it }
        }

    fun providePackFileStore(context: Context): PackFileStore =
        packFileStore ?: synchronized(this) {
            packFileStore ?: PackFileStore(context.applicationContext).also { packFileStore = it }
        }

    fun providePackImporter(context: Context): ZipPackImporter =
        packImporter ?: synchronized(this) {
            packImporter ?: ZipPackImporter(
                context = context.applicationContext,
                packIndexStore = providePackIndexStore(context),
                packFileStore = providePackFileStore(context)
            ).also { packImporter = it }
        }

    fun clear() {
        audio?.release()
        audio = null
        loader = null
        progressStore = null
        pageCacheStore = null
        packIndexStore = null
        packFileStore = null
        packImporter = null
    }
}

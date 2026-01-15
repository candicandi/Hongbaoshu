package com.xuyutech.hongbaoshu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.xuyutech.hongbaoshu.data.ContentLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
class AudioManagerImpl(
    private val context: Context,
    private val contentLoader: ContentLoader
) : AudioManager {

    private val _state = MutableStateFlow(AudioState())
    override val state: StateFlow<AudioState> = _state

    private val audioAttributes = Media3AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val bgmPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setWakeMode(C.WAKE_MODE_NETWORK)  // 使用更强的唤醒模式
        .build()
    
    private var narrationPlayer: ExoPlayer = createNarrationPlayer()
    
    private fun createNarrationPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)  // 使用更强的唤醒模式
            .build().also { player ->
                player.addListener(createNarrationListener())
            }
    }
    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            .build()
    }

    private var bgmUris: List<Uri> = emptyList()
    private var flipSoundId: Int = 0
    private var narrationCompletionCallback: NarrationCompletionCallback? = null

    private fun createNarrationListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val completedId = _state.value.narrationSentenceId
                    _state.value = _state.value.copy(
                        narrationSentenceId = null,
                        narrationPosition = 0L,
                        narrationPlaying = false
                    )
                    // 通知回调播放下一句（在后台线程执行，不依赖 UI）
                    if (completedId != null) {
                        narrationCompletionCallback?.onSentenceCompleted(completedId)
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(
                    narrationPosition = narrationPlayer.currentPosition,
                    narrationPlaying = isPlaying
                )
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("AudioManager", "Player error: ${error.message}")
                // 播放器出错时重置状态
                _state.value = _state.value.copy(
                    narrationSentenceId = null,
                    narrationPosition = 0L,
                    narrationPlaying = false
                )
            }
        }
    }
    
    override fun setNarrationCompletionCallback(callback: NarrationCompletionCallback?) {
        narrationCompletionCallback = callback
    }
    
    /**
     * 确保播放器处于可用状态，必要时重新创建
     */
    private fun ensureNarrationPlayerReady() {
        try {
            // 检查播放器是否处于错误状态
            if (narrationPlayer.playerError != null) {
                android.util.Log.w("AudioManager", "Recreating narration player due to error")
                narrationPlayer.release()
                narrationPlayer = createNarrationPlayer()
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "Error checking player state: ${e.message}")
            narrationPlayer = createNarrationPlayer()
        }
    }

    private fun ensureResourcesLoaded() {
        if (bgmUris.isEmpty()) {
            bgmUris = contentLoader.bgmPlaylist()
        }
        if (flipSoundId == 0) {
            contentLoader.flipSound()?.let { uri ->
                val rawPath = uri.path ?: return@let
                // file:///android_asset/path → strip prefix for AssetManager
                val assetPath = rawPath.removePrefix("/android_asset/")
                try {
                    context.assets.openFd(assetPath).use { afd ->
                        flipSoundId = soundPool.load(afd, 1)
                    }
                } catch (_: Exception) {
                    flipSoundId = 0
                }
            }
        }
    }

    override fun playBgm(index: Int?) {
        ensureResourcesLoaded()
        if (bgmUris.isEmpty()) return
        val target = index ?: _state.value.bgmIndex
        val idx = target.coerceIn(0, bgmUris.lastIndex)
        if (idx != _state.value.bgmIndex || !bgmPlayer.isPlaying) {
            bgmPlayer.setMediaItem(MediaItem.fromUri(bgmUris[idx]))
            bgmPlayer.prepare()
        }
        bgmPlayer.playWhenReady = true
        _state.value = _state.value.copy(bgmEnabled = true, bgmIndex = idx)
    }

    override fun pauseBgm() {
        bgmPlayer.pause()
        _state.value = _state.value.copy(bgmEnabled = false)
    }

    override fun nextBgm() {
        ensureResourcesLoaded()
        if (bgmUris.isEmpty()) return
        val next = (_state.value.bgmIndex + 1) % bgmUris.size
        playBgm(next)
    }

    override fun playSentence(sentenceId: String): Boolean {
        val uri = contentLoader.narrationUri(sentenceId)
        android.util.Log.d("AudioManager", "playSentence: id=$sentenceId, uri=$uri")
        if (uri == null) return false
        
        // 确保播放器可用
        ensureNarrationPlayerReady()
        
        try {
            narrationPlayer.setMediaItem(MediaItem.fromUri(uri))
            narrationPlayer.prepare()
            narrationPlayer.playWhenReady = true
            _state.value = _state.value.copy(
                narrationSentenceId = sentenceId,
                narrationPosition = 0L,
                narrationPlaying = true
            )
            return true
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "Error playing sentence: ${e.message}")
            // 尝试重建播放器后重试一次
            narrationPlayer.release()
            narrationPlayer = createNarrationPlayer()
            try {
                narrationPlayer.setMediaItem(MediaItem.fromUri(uri))
                narrationPlayer.prepare()
                narrationPlayer.playWhenReady = true
                _state.value = _state.value.copy(
                    narrationSentenceId = sentenceId,
                    narrationPosition = 0L,
                    narrationPlaying = true
                )
                return true
            } catch (e2: Exception) {
                android.util.Log.e("AudioManager", "Retry failed: ${e2.message}")
                return false
            }
        }
    }

    override fun pauseSentence() {
        narrationPlayer.pause()
        _state.value = _state.value.copy(
            narrationPosition = narrationPlayer.currentPosition,
            narrationPlaying = false
        )
    }

    override fun resumeSentence() {
        if (_state.value.narrationSentenceId != null) {
            narrationPlayer.playWhenReady = true
            _state.value = _state.value.copy(narrationPlaying = true)
        }
    }

    override fun stopSentence() {
        narrationPlayer.stop()
        _state.value = _state.value.copy(
            narrationSentenceId = null,
            narrationPosition = 0L,
            narrationPlaying = false
        )
    }

    override fun playFlip() {
        ensureResourcesLoaded()
        if (flipSoundId != 0) {
            soundPool.play(flipSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun currentPosition(): Long = narrationPlayer.currentPosition

    override fun seekTo(positionMs: Long) {
        if (_state.value.narrationSentenceId != null && positionMs >= 0) {
            narrationPlayer.seekTo(positionMs)
            _state.value = _state.value.copy(narrationPosition = positionMs)
        }
    }

    override fun release() {
        bgmPlayer.release()
        narrationPlayer.release()
        soundPool.release()
    }
    override fun setBgmVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        bgmPlayer.volume = safeVolume
        _state.value = _state.value.copy(bgmVolume = safeVolume)
    }
}

package com.xuyutech.hongbaoshu.audio

import kotlinx.coroutines.flow.StateFlow

data class AudioState(
    val bgmEnabled: Boolean = false,
    val bgmIndex: Int = 0,
    val narrationSentenceId: String? = null,
    val narrationPosition: Long = 0L,
    val narrationPlaying: Boolean = false,
    val bgmVolume: Float = 1.0f
)

/**
 * 朗读完成回调，用于在句子播放完成后请求下一句
 */
fun interface NarrationCompletionCallback {
    /**
     * 当前句子播放完成，请求下一句
     * @param completedSentenceId 刚播放完成的句子 ID
     */
    fun onSentenceCompleted(completedSentenceId: String)
}

interface AudioManager {
    val state: StateFlow<AudioState>

    fun playBgm(index: Int? = null)
    fun pauseBgm()
    fun nextBgm()
    fun setBgmVolume(volume: Float)

    /** 播放句子朗读，返回 true 表示成功，false 表示音频缺失 */
    fun playSentence(sentenceId: String): Boolean
    fun pauseSentence()
    fun resumeSentence()
    fun stopSentence()
    
    /** 设置朗读完成回调 */
    fun setNarrationCompletionCallback(callback: NarrationCompletionCallback?)

    fun playFlip()

    fun currentPosition(): Long

    fun seekTo(positionMs: Long)

    fun release()
}

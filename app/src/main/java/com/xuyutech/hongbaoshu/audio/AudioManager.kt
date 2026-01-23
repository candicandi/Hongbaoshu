package com.xuyutech.hongbaoshu.audio

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

data class AudioState(
    val narrationSentenceId: String? = null,
    val narrationPosition: Long = 0L,
    val narrationPlaying: Boolean = false,
    val narrationError: String? = null,
    val narrationSpeed: Float = 1.0f
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

    /** 播放句子列表，从指定索引开始 */
    fun playNarrationList(sentenceIds: List<String>, startIndex: Int): Boolean
    
    /** 播放单个句子（兼容旧接口，内部可转为列表） */
    fun playSentence(sentenceId: String): Boolean
    fun pauseSentence()
    fun resumeSentence()
    fun stopSentence()
    
    /** 设置朗读完成回调 */
    fun setNarrationCompletionCallback(callback: NarrationCompletionCallback?)

    fun setNarrationSpeed(speed: Float)

    fun playFlip()

    fun currentPosition(): Long

    fun seekTo(positionMs: Long)

    fun activePlayer(): Player?

    fun release()
}

package com.xuyutech.hongbaoshu.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlaybackServiceController(
    private val context: Context
) {
    fun start() {
        val intent = Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop() {
        val intent = Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_STOP)
        context.startService(intent)
    }
}

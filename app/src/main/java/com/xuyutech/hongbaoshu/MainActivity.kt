package com.xuyutech.hongbaoshu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.di.ServiceLocator
import com.xuyutech.hongbaoshu.reader.ReaderViewModel
import com.xuyutech.hongbaoshu.reader.ReaderViewModelFactory
import com.xuyutech.hongbaoshu.reader.ReaderScreen
import com.xuyutech.hongbaoshu.ui.theme.HongbaoshuTheme
import com.xuyutech.hongbaoshu.storage.ProgressStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HongbaoshuTheme {
                HongbaoshuApp()
            }
        }
    }
}

@Composable
private fun HongbaoshuApp() {
    val context = LocalContext.current
    val loader: ContentLoader = remember { ServiceLocator.provideContentLoader() }
    val audioManager: AudioManager = remember { ServiceLocator.provideAudioManager(context) }
    val progressStore: ProgressStore = remember { ServiceLocator.provideProgressStore(context) }
    val pageCacheStore = remember { ServiceLocator.providePageCacheStore(context) }
    
    // 启动时立即创建 ViewModel 并开始加载文档
    val viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModelFactory(
            context.applicationContext as android.app.Application,
            loader,
            progressStore,
            audioManager,
            pageCacheStore
        )
    )
    
    DisposableEffect(Unit) {
        onDispose { audioManager.release() }
    }
    val screen = remember { mutableStateOf(Screen.Cover) }
    
    // 监听加载状态
    val readerState = viewModel.state.observeAsState()
    val isLoading = readerState.value?.isLoading ?: true
    
    // 预加载封面图，避免页面切换时闪烁
    val coverPainter = rememberAsyncImagePainter("file:///android_asset/images/cover.png")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ReaderNavHost(
            screen = screen.value,
            isLoading = isLoading,
            coverPainter = coverPainter,
            onEnterReader = { screen.value = Screen.Reader },
            onBackToCover = { screen.value = Screen.Cover },
            viewModel = viewModel,
            audioManager = audioManager
        )
    }
}

@Composable
private fun CoverScreen(
    isLoading: Boolean,
    coverPainter: androidx.compose.ui.graphics.painter.Painter,
    onEnter: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = coverPainter,
            contentDescription = "封面",
            modifier = Modifier
                .fillMaxSize()
                .clickable { onEnter() },
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.Center
        )
        
        // 加载中显示转圈动画
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                color = androidx.compose.ui.graphics.Color(0xFFFFD700),
                strokeWidth = 3.dp
            )
        }
    }
}

private enum class Screen { Cover, Reader }

@Composable
private fun ReaderNavHost(
    screen: Screen,
    isLoading: Boolean,
    coverPainter: androidx.compose.ui.graphics.painter.Painter,
    onEnterReader: () -> Unit,
    onBackToCover: () -> Unit,
    viewModel: ReaderViewModel,
    audioManager: AudioManager
) {
    when (screen) {
        Screen.Cover -> CoverScreen(
            isLoading = isLoading,
            coverPainter = coverPainter,
            onEnter = onEnterReader
        )
        Screen.Reader -> ReaderScreen(
            viewModel = viewModel,
            audioManager = audioManager,
            onBack = onBackToCover
        )
    }
}

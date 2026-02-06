package com.xuyutech.hongbaoshu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xuyutech.hongbaoshu.bookshelf.BookshelfBook
import com.xuyutech.hongbaoshu.bookshelf.BookshelfScreen
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
    val screen = remember { mutableStateOf(Screen.Bookshelf) }
    
    // 监听加载状态
    val readerState = viewModel.state.observeAsState()
    val isLoading = readerState.value?.isLoading ?: true
    
    val book = readerState.value?.book
    val missingCount = readerState.value?.missingAudio?.size ?: 0
    val statusText = when {
        missingCount <= 0 -> "文本+音频"
        missingCount < 10_000 -> "音频缺失($missingCount)"
        else -> "音频缺失(较多)"
    }
    val builtinBook = BookshelfBook(
        packId = "builtin",
        title = book?.title ?: "内置书籍",
        author = book?.author ?: "",
        edition = book?.edition,
        coverUri = "file:///android_asset/images/cover.png",
        statusText = statusText
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ReaderNavHost(
            screen = screen.value,
            isLoading = isLoading,
            books = listOf(builtinBook),
            onOpenBook = { screen.value = Screen.Reader },
            onBackToBookshelf = {
                viewModel.pauseNarration()
                screen.value = Screen.Bookshelf
            },
            viewModel = viewModel,
            audioManager = audioManager
        )
    }
}

private enum class Screen { Bookshelf, Reader }

@Composable
private fun ReaderNavHost(
    screen: Screen,
    isLoading: Boolean,
    books: List<BookshelfBook>,
    onOpenBook: (BookshelfBook) -> Unit,
    onBackToBookshelf: () -> Unit,
    viewModel: ReaderViewModel,
    audioManager: AudioManager
) {
    when (screen) {
        Screen.Bookshelf -> BookshelfScreen(
            books = books,
            onOpenBook = onOpenBook,
            onImport = {},
            isLoading = isLoading,
            modifier = Modifier.fillMaxSize()
        )
        Screen.Reader -> ReaderScreen(
            viewModel = viewModel,
            audioManager = audioManager,
            onBack = onBackToBookshelf
        )
    }
}

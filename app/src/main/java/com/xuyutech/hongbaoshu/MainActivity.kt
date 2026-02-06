package com.xuyutech.hongbaoshu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xuyutech.hongbaoshu.bookshelf.BookshelfBook
import com.xuyutech.hongbaoshu.bookshelf.BookshelfScreen
import com.xuyutech.hongbaoshu.bookshelf.BookshelfViewModel
import com.xuyutech.hongbaoshu.bookshelf.BookshelfViewModelFactory
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.data.ContentLoader
import com.xuyutech.hongbaoshu.di.ServiceLocator
import com.xuyutech.hongbaoshu.reader.ReaderViewModel
import com.xuyutech.hongbaoshu.reader.ReaderViewModelFactory
import com.xuyutech.hongbaoshu.reader.ReaderScreen
import com.xuyutech.hongbaoshu.ui.theme.HongbaoshuTheme
import com.xuyutech.hongbaoshu.storage.ProgressStore
import com.xuyutech.hongbaoshu.pack.model.PackIndex
import kotlinx.coroutines.launch

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
    val packIndexStore = remember { ServiceLocator.providePackIndexStore(context) }
    
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

    val bookshelfViewModel: BookshelfViewModel = viewModel(
        factory = BookshelfViewModelFactory(packIndexStore)
    )
    val packs = bookshelfViewModel.packs.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 监听加载状态
    val readerState = viewModel.state.observeAsState()
    val isLoading = readerState.value?.isLoading ?: true
    
    val book = readerState.value?.book
    val missingCount = readerState.value?.missingAudio?.size ?: 0
    // Ensure builtin exists in bookshelf index.
    LaunchedEffect(book) {
        if (book != null) {
            packIndexStore.upsert(
                PackIndex(
                    packId = "builtin",
                    packVersion = 1,
                    formatVersion = 1,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    bookEdition = book.edition,
                    importedAt = System.currentTimeMillis(),
                    hasCover = true,
                    hasFlipSound = true,
                    hasNarration = true,
                    missingNarrationSentenceCount = missingCount,
                    isValid = true
                )
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ReaderNavHost(
            screen = screen.value,
            isLoading = isLoading,
            books = packs.value.map { p ->
                BookshelfBook(
                    packId = p.packId,
                    title = p.bookTitle,
                    author = p.bookAuthor,
                    edition = p.bookEdition,
                    coverUri = if (p.packId == "builtin") "file:///android_asset/images/cover.png" else null,
                    statusText = when {
                        !p.isValid -> "不可用"
                        !p.hasNarration -> "仅文本"
                        p.missingNarrationSentenceCount > 0 -> "音频缺失(${p.missingNarrationSentenceCount})"
                        else -> "文本+音频"
                    }
                )
            },
            onOpenBook = { selected ->
                scope.launch { packIndexStore.markOpened(selected.packId) }
                screen.value = Screen.Reader
            },
            onDeletePack = { target ->
                scope.launch { packIndexStore.delete(target.packId) }
            },
            onRevalidatePack = { target ->
                if (target.packId == "builtin" && book != null) {
                    scope.launch {
                        packIndexStore.upsert(
                            PackIndex(
                                packId = "builtin",
                                packVersion = 1,
                                formatVersion = 1,
                                bookTitle = book.title,
                                bookAuthor = book.author,
                                bookEdition = book.edition,
                                importedAt = System.currentTimeMillis(),
                                hasCover = true,
                                hasFlipSound = true,
                                hasNarration = true,
                                missingNarrationSentenceCount = missingCount,
                                isValid = true
                            )
                        )
                    }
                }
            },
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
    onDeletePack: (BookshelfBook) -> Unit,
    onRevalidatePack: (BookshelfBook) -> Unit,
    onBackToBookshelf: () -> Unit,
    viewModel: ReaderViewModel,
    audioManager: AudioManager
) {
    when (screen) {
        Screen.Bookshelf -> BookshelfScreen(
            books = books,
            onOpenBook = onOpenBook,
            onImport = {},
            onDeletePack = onDeletePack,
            onRevalidatePack = onRevalidatePack,
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

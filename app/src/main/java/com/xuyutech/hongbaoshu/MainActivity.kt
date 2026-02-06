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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.xuyutech.hongbaoshu.pack.loader.FilePackContentLoader
import kotlinx.coroutines.launch
import java.io.File

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
    val audioManager: AudioManager = remember { ServiceLocator.provideAudioManager(context) }
    val progressStore: ProgressStore = remember { ServiceLocator.provideProgressStore(context) }
    val pageCacheStore = remember { ServiceLocator.providePageCacheStore(context) }
    val packIndexStore = remember { ServiceLocator.providePackIndexStore(context) }
    val packFileStore = remember { ServiceLocator.providePackFileStore(context) }
    val packImporter = remember { ServiceLocator.providePackImporter(context) }

    val activePackId = remember { mutableStateOf("builtin") }
    val loaderForPack: ContentLoader = remember(activePackId.value) {
        if (activePackId.value == "builtin") {
            ServiceLocator.provideContentLoader()
        } else {
            FilePackContentLoader(packFileStore.packDir(activePackId.value))
        }
    }
    
    val viewModel: ReaderViewModel = viewModel(
        key = "reader_${activePackId.value}",
        factory = ReaderViewModelFactory(
            context.applicationContext as android.app.Application,
            activePackId.value,
            loaderForPack,
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
    val bookshelfMessage = remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = packImporter.import(uri)
            bookshelfMessage.value = result.message
        }
    }
    
    // 监听加载状态
    val readerState = viewModel.state.observeAsState()
    val isLoading = readerState.value?.isLoading ?: true
    
    val book = readerState.value?.book
    val missingCount = readerState.value?.missingAudio?.size ?: 0
    // Ensure builtin exists in bookshelf index.
    LaunchedEffect(book, activePackId.value) {
        if (activePackId.value == "builtin" && book != null) {
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
                val coverUri = run {
                    val cover = File(packFileStore.packDir(p.packId), "images/cover.png")
                    if (cover.exists()) cover.toURI().toString() else null
                }
                BookshelfBook(
                    packId = p.packId,
                    title = p.bookTitle,
                    author = p.bookAuthor,
                    edition = p.bookEdition,
                    coverUri = if (p.packId == "builtin") "file:///android_asset/images/cover.png" else coverUri,
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
                activePackId.value = selected.packId
                screen.value = Screen.Reader
            },
            onDeletePack = { target ->
                scope.launch {
                    packIndexStore.delete(target.packId)
                    packFileStore.deletePack(target.packId)
                }
            },
            onRevalidatePack = { target ->
                scope.launch {
                    if (target.packId == "builtin" && book != null) {
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
                        return@launch
                    }

                    val existing = packIndexStore.find(target.packId) ?: return@launch
                    val inspection = packFileStore.inspect(target.packId)
                    packIndexStore.upsert(
                        existing.copy(
                            hasCover = inspection.hasCover,
                            hasFlipSound = inspection.hasFlipSound,
                            hasNarration = inspection.hasNarration,
                            isValid = inspection.isValid
                        )
                    )
                }
            },
            onBackToBookshelf = {
                viewModel.pauseNarration()
                screen.value = Screen.Bookshelf
            },
            message = bookshelfMessage.value,
            onMessageShown = { bookshelfMessage.value = null },
            onImport = {
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            },
            viewModel = viewModel,
            narrationControlsEnabled = activePackId.value == "builtin",
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
    message: String?,
    onMessageShown: () -> Unit,
    onImport: () -> Unit,
    viewModel: ReaderViewModel,
    narrationControlsEnabled: Boolean,
    audioManager: AudioManager
) {
    when (screen) {
        Screen.Bookshelf -> BookshelfScreen(
            books = books,
            onOpenBook = onOpenBook,
            onImport = onImport,
            onDeletePack = onDeletePack,
            onRevalidatePack = onRevalidatePack,
            message = message,
            onMessageShown = onMessageShown,
            isLoading = isLoading,
            modifier = Modifier.fillMaxSize()
        )
        Screen.Reader -> ReaderScreen(
            viewModel = viewModel,
            audioManager = audioManager,
            narrationControlsEnabled = narrationControlsEnabled,
            onBack = onBackToBookshelf
        )
    }
}

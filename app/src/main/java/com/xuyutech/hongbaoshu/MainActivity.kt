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
import com.xuyutech.hongbaoshu.bookshelf.ImportProgressDialog
import com.xuyutech.hongbaoshu.bookshelf.ImportState
import com.xuyutech.hongbaoshu.audio.AudioManager
import com.xuyutech.hongbaoshu.di.ServiceLocator
import com.xuyutech.hongbaoshu.reader.ReaderViewModel
import com.xuyutech.hongbaoshu.reader.ReaderViewModelFactory
import com.xuyutech.hongbaoshu.reader.ReaderScreen
import com.xuyutech.hongbaoshu.ui.theme.HongbaoshuTheme
import com.xuyutech.hongbaoshu.storage.ProgressStore
import com.xuyutech.hongbaoshu.pack.model.PackIndex
import com.xuyutech.hongbaoshu.pack.storage.PackInspector
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
    val activePackLoader = remember { ServiceLocator.provideActivePackContentLoader(context) }
    activePackLoader.setActivePackId(activePackId.value)
    val loaderForPack = activePackLoader
    
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
    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }

    val performImport: (android.net.Uri) -> Unit = { targetUri ->
        importState = ImportState.Importing
        scope.launch {
            val result = packImporter.import(targetUri)
            if (result.status == com.xuyutech.hongbaoshu.pack.importer.PackImportResult.Status.SUCCESS || 
                result.status == com.xuyutech.hongbaoshu.pack.importer.PackImportResult.Status.SKIPPED) {
                importState = ImportState.Idle
                bookshelfMessage.value = result.message
            } else {
                importState = ImportState.Error(result.message ?: "导入报错", targetUri)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            performImport(uri)
        }
    }
    
    // 监听加载状态
    val readerState = viewModel.state.observeAsState()
    val isLoading = readerState.value?.isLoading ?: true
    
    val book = readerState.value?.book
    val missingCount = readerState.value?.missingAudio?.size ?: 0
    val activePackIndex = packs.value.firstOrNull { it.packId == activePackId.value }
    // 统一所有包的朗读控制逻辑,基于实际的音频资源可用性
    val narrationControlsEnabled = activePackIndex?.hasNarration == true && activePackIndex.isValid
    val builtinMigrator = remember { ServiceLocator.provideBuiltinMigrator(context) }
    var builtinMigrated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        builtinMigrator.invoke()
        builtinMigrated = true
    }

    if (!builtinMigrated) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text("正在初始化资源...")
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ReaderNavHost(
            screen = screen.value,
            isLoading = isLoading,
            books = packs.value.map { p ->
                val coverUri = PackInspector.resolveCoverPath(packFileStore.packDir(p.packId))
                    ?.toURI()
                    ?.toString()
                BookshelfBook(
                    packId = p.packId,
                    title = p.bookTitle,
                    author = p.bookAuthor,
                    edition = p.bookEdition,
                    coverUri = coverUri,
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
            narrationControlsEnabled = narrationControlsEnabled,
            audioManager = audioManager
        )

        ImportProgressDialog(
            state = importState,
            onRetry = { performImport(it) },
            onCancel = { importState = ImportState.Idle }
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

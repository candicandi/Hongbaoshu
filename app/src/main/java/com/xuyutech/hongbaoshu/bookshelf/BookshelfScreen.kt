package com.xuyutech.hongbaoshu.bookshelf

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xuyutech.hongbaoshu.ui.components.PrimaryOutlinedButton
import com.xuyutech.hongbaoshu.ui.components.SoftCard
import com.xuyutech.hongbaoshu.ui.components.StatusChip
import com.xuyutech.hongbaoshu.ui.theme.Dimens
import kotlinx.coroutines.launch

data class BookshelfBook(
    val packId: String,
    val title: String,
    val author: String,
    val edition: String? = null,
    val coverUri: String? = null,
    val statusText: String = "文本+音频"
)

@Composable
fun BookshelfScreen(
    books: List<BookshelfBook>,
    onOpenBook: (BookshelfBook) -> Unit,
    onImport: () -> Unit,
    onDeletePack: (BookshelfBook) -> Unit = {},
    onRevalidatePack: (BookshelfBook) -> Unit = {},
    message: String? = null,
    onMessageShown: () -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var actionTarget by remember { mutableStateOf<BookshelfBook?>(null) }
    var showActions by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "红宝匣", style = MaterialTheme.typography.headlineMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        PrimaryOutlinedButton(
                            text = "导入",
                            modifier = Modifier.padding(end = Dimens.l),
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("导入功能开发中")
                                }
                                onImport()
                            }
                        )
                    }
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        if (books.isEmpty()) {
            EmptyBookshelf(
                onImport = onImport,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Dimens.l),
            verticalArrangement = Arrangement.spacedBy(Dimens.l),
            horizontalArrangement = Arrangement.spacedBy(Dimens.l)
        ) {
            items(books, key = { it.packId }) { book ->
                BookshelfCard(
                    book = book,
                    onClick = { onOpenBook(book) },
                    onLongPress = {
                        actionTarget = book
                        showActions = true
                    }
                )
            }
        }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        onMessageShown()
    }

    if (showActions && actionTarget != null) {
        BookActionsSheet(
            book = actionTarget!!,
            onDismiss = { showActions = false },
            onOpen = {
                showActions = false
                onOpenBook(actionTarget!!)
            },
            onDetails = {
                showActions = false
                showDetails = true
            },
            onRevalidate = {
                showActions = false
                scope.launch { snackbarHostState.showSnackbar("重新校验中") }
                onRevalidatePack(actionTarget!!)
            },
            onDelete = {
                showActions = false
                showDeleteConfirm = true
            }
        )
    }

    if (showDetails && actionTarget != null) {
        BookDetailsDialog(
            book = actionTarget!!,
            onDismiss = { showDetails = false }
        )
    }

    if (showDeleteConfirm && actionTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = "删除书籍？") },
            text = { Text(text = "将从书架移除并删除本地资源（尚未实现）。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val target = actionTarget!!
                        if (target.packId == "builtin") {
                            scope.launch { snackbarHostState.showSnackbar("内置书籍不可删除") }
                        } else {
                            onDeletePack(target)
                            scope.launch { snackbarHostState.showSnackbar("已从书架移除") }
                        }
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun EmptyBookshelf(
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Dimens.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "书架还是空的",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "导入一个资源包开始阅读",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.s, bottom = Dimens.l)
            )
            PrimaryOutlinedButton(text = "导入资源包", onClick = onImport)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BookshelfCard(
    book: BookshelfBook,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoftCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                )
                .padding(Dimens.m)
        ) {
            val coverShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            AsyncImage(
                model = book.coverUri,
                contentDescription = "封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(coverShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Dimens.m)
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.xs)
            )
            StatusChip(
                text = book.statusText,
                modifier = Modifier.padding(top = Dimens.s)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookActionsSheet(
    book: BookshelfBook,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    onRevalidate: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.l)
                .padding(bottom = Dimens.xl)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.xs)
                )
            }
            StatusChip(text = book.statusText, modifier = Modifier.padding(top = Dimens.s))

            PrimaryOutlinedButton(
                text = "打开",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.l),
                onClick = onOpen
            )
            PrimaryOutlinedButton(
                text = "详情",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.s),
                onClick = onDetails
            )
            PrimaryOutlinedButton(
                text = "重新校验",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.s),
                onClick = onRevalidate
            )
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.s)
            ) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BookDetailsDialog(
    book: BookshelfBook,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "书籍详情") },
        text = {
            Column {
                Text(text = "标题：${book.title}")
                if (book.author.isNotBlank()) Text(text = "作者：${book.author}")
                if (!book.edition.isNullOrBlank()) Text(text = "版本：${book.edition}")
                Text(text = "PackId：${book.packId}")
                Text(text = "状态：${book.statusText}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

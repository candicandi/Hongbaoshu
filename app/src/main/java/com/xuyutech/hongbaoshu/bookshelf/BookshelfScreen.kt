package com.xuyutech.hongbaoshu.bookshelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    onClick = { onOpenBook(book) }
                )
            }
        }
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
private fun BookshelfCard(
    book: BookshelfBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SoftCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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

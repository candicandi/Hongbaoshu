package com.xuyutech.hongbaoshu.bookshelf

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

sealed class ImportState {
    object Idle : ImportState()
    object Importing : ImportState()
    data class Error(val message: String, val uri: Uri) : ImportState()
}

@Composable
fun ImportProgressDialog(
    state: ImportState,
    onRetry: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    if (state is ImportState.Idle) return

    val dismissProps = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    )

    when (state) {
        is ImportState.Importing -> {
            AlertDialog(
                onDismissRequest = { /* blocked */ },
                properties = dismissProps,
                title = { Text("正在导入资源包") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("请耐心等待，请勿关闭应用")
                    }
                },
                confirmButton = {}
            )
        }
        is ImportState.Error -> {
            AlertDialog(
                onDismissRequest = onCancel,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "错误",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导入失败")
                    }
                },
                text = {
                    Column {
                        Text(state.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("建议您检查压缩包格式是否为支持的 2.0 标准包。", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onRetry(state.uri) }) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancel) {
                        Text("取消")
                    }
                }
            )
        }
        else -> {}
    }
}

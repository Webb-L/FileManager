package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.FileInfo
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils


@Composable
fun FileInfoDialog(fileInfo: FileInfo, rootPath: String, onCancel: () -> Unit) {
    val rootPathTotalSpace = FileUtils.totalSpace(rootPath)
    val rootPathFreeSpace = FileUtils.freeSpace(rootPath)

    var fileCount by remember {
        mutableStateOf(0)
    }
    var folderCount by remember {
        mutableStateOf(0)
    }
    var size by remember {
        mutableStateOf(-1L)
    }
    LaunchedEffect(Unit) {
        size = if (fileInfo.isDirectory) {
            PathUtils.traverse(fileInfo.path).sumOf {
                if (it.isDirectory) {
                    folderCount++
                    0
                } else {
                    fileCount++
                    it.size
                }
            }
        } else {
            fileInfo.size
        }
    }
    AlertDialog(
        icon = { FileIcon(fileInfo) },
        title = { SelectionContainer { Text(fileInfo.name) } },
        text = {
            SelectionContainer {
                println(rootPathTotalSpace)
                Column {
                    LinearProgressIndicator(
                        progress = (size.toFloat() / rootPathTotalSpace.toFloat()),
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .height(10.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    DisableSelection {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${rootPathTotalSpace.formatFileSize()} 总计")
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProgressIndicatorDefaults.linearColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                if (size < 0) {
                                    Text("计算中 已用")
                                } else {
                                    Text("${size.formatFileSize()} 已用")
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProgressIndicatorDefaults.linearTrackColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("${rootPathFreeSpace.formatFileSize()} 剩余")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("位置", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.path, Modifier.weight(0.7f))
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("类型", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        DisableSelection {
                            Text(if (fileInfo.isDirectory) "文件夹" else "文件", Modifier.weight(0.7f))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("权限", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Column 2", Modifier.weight(0.7f))
                    }
                    if (fileCount > 0) {
                        Row(Modifier.fillMaxWidth().padding(4.dp)) {
                            DisableSelection {
                                Text("文件", Modifier.weight(0.3f))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$fileCount", Modifier.weight(0.7f))
                        }
                    }
                    if (folderCount > 0) {
                        Row(Modifier.fillMaxWidth().padding(4.dp)) {
                            DisableSelection {
                                Text("文件夹", Modifier.weight(0.3f))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$folderCount", Modifier.weight(0.7f))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("创建时间", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.createdDate.timestampToSyncDate(), Modifier.weight(0.7f))
                    }
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("更新时间", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.updatedDate.timestampToSyncDate(), Modifier.weight(0.7f))
                    }
                }
            }
        },
        onDismissRequest = onCancel,
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun FileRenameDialog(fileInfo: FileInfo, onCancel: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(fileInfo.name) }

    AlertDialog(
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("重命名") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("名称") },
                leadingIcon = { FileIcon(fileInfo) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton({ text = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton({ onCancel(text) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton({ onCancel("") }) {
                Text("取消")
            }
        }

    )
}
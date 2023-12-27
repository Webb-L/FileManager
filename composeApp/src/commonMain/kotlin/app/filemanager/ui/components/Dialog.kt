package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.FileInfo
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import org.koin.compose.koinInject


@Composable
fun FileInfoDialog(fileInfo: FileInfo, onCancel: () -> Unit) {
    val mainState = koinInject<MainState>()
    val rootPath by mainState.rootPath.collectAsState()
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

@Composable
fun TextFieldDialog(title: String, label: String = "", initText: String = "", onCancel: (String) -> Unit) {
    var text by remember { mutableStateOf(initText) }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.focusRequester(focusRequester),
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun FileOperationDialog(title: String) {
    val fileOperationState = koinInject<FileOperationState>()
    val currentIndex by fileOperationState.currentIndex.collectAsState()

    var isExpandDevice by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        title = { Text(title) },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = (currentIndex / fileOperationState.fileInfos.size).toFloat(),
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .height(10.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${fileOperationState.fileInfos.size} 总计")
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(
                            Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ProgressIndicatorDefaults.linearColor)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$currentIndex 完成")
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
                        Text("${fileOperationState.fileInfos.size - currentIndex} 剩余")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Text(
                        "当前：${fileOperationState.fileInfos[currentIndex].path}", Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        if (isExpandDevice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        Modifier.clip(RoundedCornerShape(25.dp)).clickable {
                            isExpandDevice = !isExpandDevice
                        }
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (isExpandDevice) {
                    BasicTextField(
                        fileOperationState.logs.reversed().joinToString("\n"),
                        textStyle = LocalTextStyle.current.copy(LocalContentColor.current.copy(0.6f)),
                        readOnly = true,
                        modifier = Modifier
                            .heightIn(max = 180.dp),
                        onValueChange = {})
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            TextButton({ }) {
                Text("取消")
            }
        }
    )
}
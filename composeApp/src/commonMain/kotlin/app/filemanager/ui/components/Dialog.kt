package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileInfo
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileOperationType
import app.filemanager.ui.state.file.FileState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import org.koin.compose.koinInject


@Composable
fun FileInfoDialog(fileInfo: FileInfo, onCancel: () -> Unit) {
    val fileState = koinInject<FileState>()
    val rootPath by fileState.rootPath.collectAsState()
    val rootPathTotalSpace = FileUtils.totalSpace(rootPath)
    val rootPathFreeSpace = FileUtils.freeSpace(rootPath)

    val fileFilterState = koinInject<FileFilterState>()

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
                            if (fileInfo.isDirectory) {
                                Text("文件夹", Modifier.weight(0.7f))
                            } else {
                                val fileFilter = fileFilterState.getFilterFileByFileExtension(fileInfo.mineType)
                                Text(
                                    "文件${if (fileFilter != null) "(${fileFilter.name})" else ""}",
                                    Modifier.weight(0.7f)
                                )
                            }
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
fun FileRenameDialog(
    fileInfo: FileInfo,
    verifyFun: (String) -> Pair<Boolean, String>,
    onCancel: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(fileInfo.name) }
    val verify = verifyFun(text)

    AlertDialog(
        icon = { Icon(Icons.Outlined.Edit, null) },
        title = { Text("重命名") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("名称") },
                isError = verify.first,
                leadingIcon = { FileIcon(fileInfo) },
                modifier = Modifier.semantics { if (verify.first) error(verify.second) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton({ text = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                supportingText = {
                    if (verify.first) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = verify.second,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
            )
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                { onCancel(text) },
                enabled = if (text.isEmpty()) false else !verify.first
            ) {
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
fun TextFieldDialog(
    title: String,
    label: String = "",
    initText: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    verifyFun: suspend (String) -> Pair<Boolean, String> = { _ -> Pair(false, "") },
    onCancel: (String) -> Unit
) {
    var text by remember { mutableStateOf(initText) }
    val focusRequester = remember { FocusRequester() }
    var verify by remember { mutableStateOf(Pair(false, "")) }

    LaunchedEffect(text) {
        verify = verifyFun(text)
    }

    AlertDialog(
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                isError = verify.first,
                modifier = Modifier.focusRequester(focusRequester)
                    .semantics { if (verify.first) error(verify.second) },
                leadingIcon = leadingIcon,
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton({ text = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                supportingText = {
                    if (verify.first) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = verify.second,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
            )
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                { onCancel(text) },
                enabled = if (text.isEmpty()) false else !verify.first
            ) {
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
fun FileOperationDialog(onCancel: () -> Unit, onDismiss: () -> Unit) {
    val fileOperationState = koinInject<FileOperationState>()
    val currentIndex by fileOperationState.currentIndex.collectAsState()
    val isFinished by fileOperationState.isFinished.collectAsState()

    var isExpandDevice by remember {
        mutableStateOf(true)
    }

    AlertDialog(
        title = { Text(fileOperationState.title) },
        text = {
            if (fileOperationState.fileInfos.isEmpty() && currentIndex == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("正在统计...")
                }
                return@AlertDialog
            }
            Column {
                LinearProgressIndicator(
                    progress = (currentIndex / fileOperationState.fileInfos.size.toFloat()),
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

                    if (fileOperationState.logs.isEmpty()) return@Row
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
            if (isFinished) {
                TextButton(onDismiss) { Text("关闭") }
                return@AlertDialog
            }
            TextButton(onCancel) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileWarningOperationDialog() {
    val operationState = koinInject<FileOperationState>()
    val type by operationState.warningOperationType.collectAsState()
    val isUseAll by operationState.warningUseAll.collectAsState()
    val files by operationState.warningFiles.collectAsState()

    val newFile = files.first
    val oldFile = files.second

    AlertDialog(
        onDismissRequest = {},
        title = { Text("是否需要替换") },
        text = {
            Column {
                Spacer(Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedCard {
                        ListItem(
                            headlineContent = { Text(newFile.name) },
                            supportingContent = { FileWarningDialogItem(newFile) },
                            leadingContent = { FileIcon(newFile) },
                            trailingContent = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ) { Text("新文件") }
                            },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(4.dp))
                    OutlinedCard {
                        ListItem(
                            headlineContent = { Text(oldFile.name) },
                            supportingContent = { FileWarningDialogItem(oldFile) },
                            leadingContent = { FileIcon(oldFile) },
                            trailingContent = { Badge { Text("旧文件") } },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                FlowRow {
                    FilterChip(
                        onClick = { operationState.updateWarningOperationType(FileOperationType.Replace) },
                        label = {
                            if (newFile.isDirectory && oldFile.isDirectory) {
                                Text("替换文件夹")
                            } else {
                                Text("替换文件")
                            }
                        },
                        selected = type == FileOperationType.Replace
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        onClick = { operationState.updateWarningOperationType(FileOperationType.Jump) },
                        label = {
                            if (newFile.isDirectory && oldFile.isDirectory) {
                                Text("跳过文件夹")
                            } else {
                                Text("跳过文件")
                            }
                        },
                        selected = type == FileOperationType.Jump
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        onClick = { operationState.updateWarningOperationType(FileOperationType.Reserve) },
                        label = {
                            if (newFile.isDirectory && oldFile.isDirectory) {
                                Text("保留文件夹")
                            } else {
                                Text("保留文件")
                            }
                        },
                        selected = type == FileOperationType.Reserve
                    )
                }

                if (!newFile.isDirectory && !oldFile.isDirectory) return@Column
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .toggleable(
                            value = isUseAll,
                            onValueChange = { operationState.updateWarningUseAll(!isUseAll) },
                            role = Role.Checkbox
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUseAll,
                        onCheckedChange = null
                    )
                    Text(
                        text = "应用此操作到所有文件夹和文件",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton({
                when (type) {
                    FileOperationType.Replace -> {}
                    FileOperationType.Jump -> operationState.isContinue = true
                    FileOperationType.Reserve -> {}
                }
                operationState.updateWarningOperationDialog(false)
            }) { Text("确认") }
        },
        dismissButton = {
            TextButton({
                operationState.updateWarningOperationDialog(false)
                operationState.isCancel = true
            }) { Text("取消") }
        }
    )
}

@Composable
private fun FileWarningDialogItem(newFile: FileInfo) {
    Row {
        if (newFile.isDirectory) {
            Text("${newFile.size}项", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(newFile.size.formatFileSize(), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            newFile.path.replace(newFile.name, ""),
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
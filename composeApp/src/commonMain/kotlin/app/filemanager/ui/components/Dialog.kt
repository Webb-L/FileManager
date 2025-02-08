package app.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.FileSizeInfo
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileOperationType
import app.filemanager.ui.state.file.FileState
import org.koin.compose.koinInject
import kotlin.math.roundToInt


@Composable
fun FileInfoDialog(fileInfo: FileSimpleInfo, onCancel: () -> Unit) {
    val fileState = koinInject<FileState>()
    val rootPath by fileState.rootPath.collectAsState()

    val fileFilterState = koinInject<FileFilterState>()

    var errorText by remember {
        mutableStateOf("")
    }
    var fileSize by remember {
        mutableStateOf<FileSizeInfo?>(null)
    }

    LaunchedEffect(fileInfo) {
        val result = fileState.getSizeInfo(fileInfo, rootPath)

        if (result.isSuccess) {
            fileSize = result.getOrNull() ?: FileSizeInfo(
                fileSize = 0,
                fileCount = 0,
                folderCount = 0,
                totalSpace = -1,
                freeSpace = -1
            )
        } else {
            errorText = result.exceptionOrNull()?.message ?: ""
        }
    }
    AlertDialog(
        icon = { FileIcon(fileInfo) },
        title = { SelectionContainer { Text(fileInfo.name) } },
        text = {
            SelectionContainer {
                Column {
                    val progressValue =
                        if (fileSize == null) 0F else (fileSize!!.fileSize.toFloat() / fileSize!!.totalSpace.toFloat())
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .height(10.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    DisableSelection {
                        if (errorText.isNotEmpty()) {
                            Text(
                                "获取大小失败：$errorText",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            return@DisableSelection
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (fileSize==null) {
                                Text("计算中 总计")
                            } else {
                                Text("${fileSize!!.totalSpace.formatFileSize()} 总计")
                            }
                            Spacer(Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProgressIndicatorDefaults.linearColor)
                                )
                                Spacer(Modifier.width(4.dp))
                                if (fileSize==null) {
                                    Text("计算中 已用")
                                } else {
                                    Text("${fileSize!!.fileSize.formatFileSize()} 已用(${(progressValue * 100).roundToInt()}%)")
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
                                if (fileSize==null) {
                                    Text("计算中 剩余")
                                } else {
                                    Text("${fileSize!!.freeSpace.formatFileSize()} 剩余")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        DisableSelection {
                            Text("位置", Modifier.weight(0.3f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fileInfo.path.replace(fileInfo.name, ""), Modifier.weight(0.7f))
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
                    if (fileSize!=null) {
                        Row(Modifier.fillMaxWidth().padding(4.dp)) {
                            DisableSelection {
                                Text("文件", Modifier.weight(0.3f))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${fileSize!!.fileCount}", Modifier.weight(0.7f))
                        }
                    }
                    if (fileSize!=null) {
                        Row(Modifier.fillMaxWidth().padding(4.dp)) {
                            DisableSelection {
                                Text("文件夹", Modifier.weight(0.3f))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${fileSize!!.folderCount}", Modifier.weight(0.7f))
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
    fileInfo: FileSimpleInfo,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileWarningOperationDialog() {
    val operationState = koinInject<FileOperationState>()

    AlertDialog(
        modifier = Modifier.padding(16.dp),
        onDismissRequest = {},
        title = { Text("是否需要替换") },
        text = {
            LazyColumn {
                val fileOperations = operationState.files.filter { it.isConflict }

                itemsIndexed(fileOperations) { index, file ->
                    Column {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(
                                top = 12.dp,
                                bottom = 14.dp
                            )
                        ) {
                            OutlinedCard {
                                ListItem(
                                    headlineContent = { Text(file.src.name) },
                                    supportingContent = { FileWarningDialogItem(file.src) },
                                    leadingContent = { FileIcon(file.src) },
                                    trailingContent = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ) { Text("新") }
                                    },
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(4.dp))
                            OutlinedCard {
                                ListItem(
                                    headlineContent = { Text(file.dest.name) },
                                    supportingContent = { FileWarningDialogItem(file.dest) },
                                    leadingContent = { FileIcon(file.dest) },
                                    trailingContent = {
                                        Badge { Text("旧") }
                                    },
                                )
                            }
                        }

                        FlowRow {
                            FileOperationType.entries.forEach { operationType ->
                                FilterChip(
                                    onClick = {
                                        operationState.files.indexOf(file).takeIf { it >= 0 }?.let { index ->
                                            operationState.files[index] = file.withCopy(type = operationType)
                                        }
                                    },
                                    label = {
                                        val labelText = when {
                                            file.dest.isDirectory && file.src.isDirectory -> when (operationType) {
                                                FileOperationType.Replace -> "覆盖文件夹"
                                                FileOperationType.Jump -> "跳过文件夹"
                                                FileOperationType.Reserve -> "保留文件夹"
                                            }

                                            else -> when (operationType) {
                                                FileOperationType.Replace -> "覆盖文件"
                                                FileOperationType.Jump -> "跳过文件"
                                                FileOperationType.Reserve -> "保留文件"
                                            }
                                        }
                                        Text(labelText)
                                    },
                                    selected = file.type == operationType
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }

                        if (!file.dest.isDirectory && !file.src.isDirectory) return@Column

                        Spacer(Modifier.height(8.dp))

//                        Row(
//                            Modifier
//                                .toggleable(
//                                    value = file.isUseAll,
//                                    onValueChange = {
//                                        operationState.files.indexOf(file).takeIf { it >= 0 }?.let { index ->
//                                            operationState.files[index] = file.withCopy(
//                                                isUseAll = !file.isUseAll
//                                            )
//                                        }
//                                    },
//                                    role = Role.Checkbox
//                                ),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Checkbox(
//                                checked = file.isUseAll,
//                                onCheckedChange = null
//                            )
//                            Text(
//                                text = "应用此操作到所有文件夹和文件",
//                                style = MaterialTheme.typography.bodyLarge,
//                                modifier = Modifier.padding(start = 16.dp)
//                            )
//                        }
                    }

                    if (index < fileOperations.size - 1) {
                        HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton({
                operationState.updateWarningOperationDialog(false)
            }) { Text("确认") }
        },
        dismissButton = {
            TextButton({
                operationState.files.clear()
                operationState.updateWarningOperationDialog(false)
            }) { Text("取消") }
        }
    )
}

@Composable
private fun FileWarningDialogItem(newFile: FileSimpleInfo) {
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
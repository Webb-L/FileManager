package app.filemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.getFileFilterType
import app.filemanager.db.FileFavorite
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCard(
    file: FileInfo,
    onClick: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val dismissState = rememberDismissState()

    SwipeToDismiss(
        state = dismissState,
        background = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> Color.Transparent
                    DismissValue.DismissedToStart -> MaterialTheme.colorScheme.onError
                    DismissValue.DismissedToEnd -> MaterialTheme.colorScheme.primaryContainer
                }
            )
            Box(Modifier.fillMaxSize().background(color))
        },
        dismissContent = {
            ListItem(
                overlineContent = if (file.description.isNotEmpty()) {
                    { Text(file.description) }
                } else {
                    null
                },
                headlineContent = { if (file.name.isNotEmpty()) Text(file.name) },
                supportingContent = {
                    Row {
//                Text(file.user, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
//                Text(file.userGroup, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
                        if (file.isDirectory) {
                            Text("${file.size}项", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(file.size.formatFileSize(), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(file.createdDate.timestampToSyncDate(), style = MaterialTheme.typography.bodySmall)
                    }
                },
                leadingContent = { FileIcon(file) },
                trailingContent = { FileCardMenu(file, onRemove) },
                modifier = Modifier.clickable(onClick = onClick)
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileFavoriteCard(
    favorite: FileFavorite,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = { if (favorite.name.isNotEmpty()) Text(favorite.name) },
        supportingContent = {
            Row {
//                Text(favorite.user, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
//                Text(favorite.userGroup, style = MaterialTheme.typography.bodySmall)
//                Spacer(Modifier.width(8.dp))
                if (favorite.isDirectory) {
                    Text("${favorite.size}项", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(favorite.size.formatFileSize(), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                Text(favorite.createdDate.timestampToSyncDate(), style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            FileIcon(
                FileInfo(
                    name = "",
                    description = "",
                    isDirectory = favorite.isDirectory,
                    isHidden = false,
                    path = "",
                    mineType = favorite.mineType,
                    size = 0,
                    permissions = 0,
                    user = "",
                    userGroup = "",
                    createdDate = 0,
                    updatedDate = 0
                )
            )
        },
        trailingContent = { FileFavoriteCardMenu(favorite = favorite, onRemove = onRemove) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun FileIcon(file: FileInfo) {
    val fileFilterState = koinInject<FileFilterState>()

    if (file.isDirectory) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = file.name,
        )
        return
    }

    val fileFilter = fileFilterState.getFilterFileByFileExtension(file.mineType)
    if (fileFilter == null) {
        Icon(
            Icons.Default.Note,
            contentDescription = file.name,
        )
        return
    }
    getFileFilterType(fileFilter.type)
}

@Composable
fun FileCardMenu(
    file: FileInfo,
    onRemove: (String) -> Unit
) {
    val fileState = koinInject<FileState>()
    val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
    val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var shareExpanded by remember { mutableStateOf(false) }

    val fileOperationState = koinInject<FileOperationState>()
    val database = koinInject<FileManagerDatabase>()

    val deviceState = koinInject<DeviceState>()
    val scope = rememberCoroutineScope()
    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
        Icon(
            Icons.Filled.MoreVert,
            null,
            modifier = Modifier.clip(RoundedCornerShape(25.dp)).clickable {
                expanded = true
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 粘贴文件
            if ((isPasteCopyFile || isPasteMoveFile)) {
                DropdownMenuItem(
                    text = { Text("粘贴") },
                    onClick = {
                        scope.launch {
                            if (isPasteCopyFile) {
                                fileState.pasteCopyFile(file.path, fileOperationState)
                            }
                            if (isPasteMoveFile) {
                                fileState.pasteMoveFile(file.path, fileOperationState)
                            }
                        }
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = null
                        )
                    })
                Divider()
            }
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    fileState.copyFile(file.path)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FileCopy,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("移动") },
                onClick = {
                    fileState.moveFile(file.path)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.ContentCut,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    onRemove(file.path)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                })
            Divider()
            if (deviceState.devices.isNotEmpty()) {
                ShareButton(shareExpanded, file) {
                    shareExpanded = it
                }
                Divider()
            }
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = {
                    fileState.updateFileInfo(file)
                    fileState.updateRenameFile(true)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("设置") },
                onClick = { /* Handle settings! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null
                    )
                })
            Divider()
            DropdownMenuItem(
                text = { Text("收藏") },
                onClick = {
                    scope.launch {
                        database.fileFavoriteQueries.insert(
                            name = file.name,
                            isDirectory = file.isDirectory,
                            isFixed = false,
                            path = file.path,
                            mineType = file.mineType,
                            size = file.size,
                            createdDate = file.createdDate,
                            updatedDate = file.updatedDate,
                            protocol = FileProtocol.Local,
                            protocolId = ""
                        )
                    }
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                })
            Divider()
            DropdownMenuItem(
                text = { Text("属性") },
                onClick = {
                    scope.launch {
                        fileState.updateFileInfo(file)
                    }
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null
                    )
                })
        }
    }
}

@Composable
fun FileFavoriteCardMenu(
    favorite: FileFavorite,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
        Icon(
            Icons.Filled.MoreVert,
            null,
            modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    if (favorite.isFixed) {
                        Text("取消")
                    } else {
                        Text("固定")
                    }
                },
                onClick = {
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.VerticalAlignTop,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    onRemove()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                })
        }
    }
}
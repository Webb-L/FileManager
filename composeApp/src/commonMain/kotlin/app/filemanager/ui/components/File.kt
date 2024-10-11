package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.getFileFilterType
import app.filemanager.db.FileFavorite
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun FileCard(
    file: FileSimpleInfo,
    checkedState: Boolean,
    onStateChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val database = koinInject<FileManagerDatabase>()

    ListItem(
        overlineContent = if (file.description.isNotEmpty()) {
            { Text(file.description) }
        } else {
            null
        },
        headlineContent = { if (file.name.isNotEmpty()) Text(file.name) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (file.protocol) {
                    FileProtocol.Local -> {}
                    FileProtocol.Device -> {
                        Icon(
                            Icons.Default.Devices,
                            null,
                            Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                    }

                    FileProtocol.Network -> {
                        Icon(Icons.Default.Public, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                    }
                }

                val isFavorite = database.fileFavoriteQueries
                    .queryByPathProtocol(file.path, file.protocol)
                    .executeAsList()

                if (isFavorite.isNotEmpty()) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                }

                if (file.isDirectory) {
                    Text("${file.size}项", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(file.size.formatFileSize(), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                Text(file.createdDate.timestampToSyncDate(), style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .toggleable(
                        value = checkedState,
                        onValueChange = { onStateChange(!checkedState) },
                        role = Role.Checkbox,
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!checkedState) {
                    FileIcon(file)
                } else {
                    Checkbox(checkedState, onCheckedChange = null)
                }
            }
        },
        trailingContent = { FileCardMenu(file, onRemove) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun FileFavoriteCard(
    favorite: FileFavorite,
    onClick: () -> Unit,
    onFixed: () -> Unit,
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
        colors = ListItemDefaults.colors(
            containerColor = if (favorite.isFixed) {
                MaterialTheme.colorScheme.primaryContainer
            } else ListItemDefaults.containerColor
        ),
        leadingContent = {
            FileIcon(
                FileSimpleInfo(
                    name = "",
                    description = "",
                    isDirectory = favorite.isDirectory,
                    isHidden = false,
                    path = "",
                    mineType = favorite.mineType,
                    size = 0,
                    createdDate = 0,
                    updatedDate = 0
                )
            )
        },
        trailingContent = { FileFavoriteCardMenu(favorite = favorite, onFixed = onFixed, onRemove = onRemove) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun FileIcon(file: FileSimpleInfo) {
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
    file: FileSimpleInfo,
    onRemove: (String) -> Unit
) {
    val fileState = koinInject<FileState>()
    val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
    val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val fileOperationState = koinInject<FileOperationState>()
    val database = koinInject<FileManagerDatabase>()

    val scope = rememberCoroutineScope()
    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
        Icon(
            Icons.Filled.MoreVert,
            null,
            modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .clickable {
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

            val isFavorite = database.fileFavoriteQueries
                .queryByPathProtocol(file.path, FileProtocol.Local)
                .executeAsList()

            DropdownMenuItem(
                text = { Text("收藏") },
                onClick = {
                    if (isFavorite.isNotEmpty()) {
                        database.fileFavoriteQueries.deleteById(isFavorite.first())
                    } else {
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
                        if (isFavorite.isNotEmpty())
                            Icons.Outlined.Favorite
                        else
                            Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("属性") },
                onClick = {
                    scope.launch {
                        fileState.updateFileInfo(file)
                        fileState.updateViewFile(true)
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
    onFixed: () -> Unit,
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
                    onFixed()
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
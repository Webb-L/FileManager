package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.getFileFilterType
import app.filemanager.data.file.toIcon
import app.filemanager.db.FileFavorite
import app.filemanager.db.FileManagerDatabase
import app.filemanager.extensions.formatFileSize
import app.filemanager.extensions.timestampToSyncDate
import app.filemanager.ui.screen.file.FileShareScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
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
                file.protocol.toIcon()

                val isFavorite = database.fileFavoriteQueries
                    .queryByPathProtocol(file.path, file.protocol)
                    .executeAsList()

                if (isFavorite.isNotEmpty()) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        null,
                        Modifier.size(12.dp),
                        tint = colorScheme.primary
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
        trailingContent = {
            if (file.protocol == FileProtocol.Share) {
                FileShareMenu(file)
                return@ListItem
            }
            FileCardMenu(file, onRemove)
        },
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
                colorScheme.primaryContainer
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
            Icons.AutoMirrored.Default.Note,
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
    val mainState = koinInject<MainState>()
    val fileState = koinInject<FileState>()
    val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
    val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

    val fileOperationState = koinInject<FileOperationState>()
    val database = koinInject<FileManagerDatabase>()

    val isFileChecked = fileState.checkedFileSimpleInfo.contains(file)
    val isFavorite = database.fileFavoriteQueries
        .queryByPathProtocol(file.path, FileProtocol.Local)
        .executeAsList()

    FileMenu(
        paste = (isPasteCopyFile || isPasteMoveFile) && !isFileChecked,
        onPaste = {
            if (isPasteCopyFile) {
                fileState.pasteCopyFile(file.path, fileOperationState)
            }
            if (isPasteMoveFile) {
                fileState.pasteMoveFile(file.path, fileOperationState)
            }
        },
        copy = (!isPasteCopyFile && !isPasteMoveFile) && !isFileChecked,
        onCopy = {
            fileState.checkedFileSimpleInfo.add(file)
            fileState.copyFile()
        },
        move = (!isPasteCopyFile && !isPasteMoveFile) && !isFileChecked,
        onMove = {
            fileState.checkedFileSimpleInfo.add(file)
            fileState.moveFile()
        },
        onDelete = { onRemove(file.path) },
        onRename = {
            fileState.updateFileInfo(file)
            fileState.updateRenameFile(true)
        },
        onSetting = {
            println("需要设置的文件 ${fileState.checkedFileSimpleInfo.toList()}")
        },
        favoriteStatus = isFavorite.isNotEmpty(),
        onFavorite = {
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
        },
        onShare = {
            mainState.updateScreen(FileShareScreen(listOf(file)))
        },
        onInfo = {
            fileState.updateFileInfo(file)
            fileState.updateViewFile(true)
        },
    )
}

@Composable
fun FileShareMenu(
    file: FileSimpleInfo,
) {
    val fileState = koinInject<FileState>()
    IconButton({
        // 已经选择后不能再选择。
        if (fileState.checkedFileSimpleInfo.contains(file)) return@IconButton
        fileState.checkedFileSimpleInfo.add(file)
        fileState.copyFile()
    }) {
        Icon(Icons.Outlined.FileCopy, null)
    }
}

@Composable
fun FileBottomAppMenu(
    onRemove: (List<String>) -> Unit,
) {
    val mainState = koinInject<MainState>()

    val fileState = koinInject<FileState>()
    val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
    val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

    FileMenu(
        paste = false,
        onPaste = {},
        copy = !isPasteCopyFile && !isPasteMoveFile,
        onCopy = { fileState.copyFile() },
        move = !isPasteCopyFile && !isPasteMoveFile,
        onMove = { fileState.moveFile() },
        delete = true,
        onDelete = { onRemove(fileState.checkedFileSimpleInfo.map { it.path }) },
        rename = false,
        setting = true,
        onSetting = {
            println("需要设置的文件 ${fileState.checkedFileSimpleInfo.toList()}")
        },
        favorite = false,
        share = true,
        onShare = {
            mainState.updateScreen(FileShareScreen(fileState.checkedFileSimpleInfo.toList()))
        },
        info = true,
        onInfo = { fileState.updateViewFile(true) },
    )
}

@Composable
fun FileMenu(
    paste: Boolean = true,
    onPaste: suspend () -> Unit = {},
    copy: Boolean = true,
    onCopy: suspend () -> Unit = {},
    move: Boolean = true,
    onMove: suspend () -> Unit = {},
    delete: Boolean = true,
    onDelete: suspend () -> Unit = {},
    rename: Boolean = true,
    onRename: suspend () -> Unit = {},
    setting: Boolean = true,
    onSetting: suspend () -> Unit = {},
    favoriteStatus: Boolean = false,
    favorite: Boolean = true,
    onFavorite: suspend () -> Unit = {},
    share: Boolean = true,
    onShare: suspend () -> Unit = {},
    info: Boolean = true,
    onInfo: suspend () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                null,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (paste) {
                DropdownMenuItem(
                    text = { Text("粘贴") },
                    onClick = {
                        expanded = false
                        scope.launch { onPaste() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.ContentPaste, null)
                    })
                HorizontalDivider()
            }

            if (copy) {
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = {
                        expanded = false
                        scope.launch { onCopy() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.FileCopy, null)
                    })
            }

            if (move) {
                DropdownMenuItem(
                    text = { Text("移动") },
                    onClick = {
                        expanded = false
                        scope.launch { onMove() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.ContentCut, null)
                    })
            }

            if (delete) {
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        expanded = false
                        scope.launch { onDelete() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Delete, null)
                    })
            }

            if (rename || setting) {
                HorizontalDivider()
            }

            if (rename) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        expanded = false
                        scope.launch { onRename() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, null)
                    })
            }

            if (setting) {
                DropdownMenuItem(
                    text = { Text("设置") },
                    onClick = {
                        expanded = false
                        scope.launch { onSetting() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Settings, null)
                    })
            }

            if (favorite || share) {
                HorizontalDivider()
            }

            if (favorite) {
                DropdownMenuItem(
                    text = { Text("收藏") },
                    onClick = {
                        expanded = false
                        scope.launch { onFavorite() }
                    },
                    leadingIcon = {
                        Icon(
                            if (favoriteStatus)
                                Icons.Outlined.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
                            contentDescription = null
                        )
                    })
            }

            if (share) {
                DropdownMenuItem(
                    text = { Text("分享") },
                    onClick = {
                        expanded = false
                        scope.launch { onShare() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Share, null)
                    })
            }

            if (info) {
                HorizontalDivider()
            }

            if (info) {
                DropdownMenuItem(
                    text = { Text("属性") },
                    onClick = {
                        expanded = false
                        scope.launch { onInfo() }
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Info, null)
                    })
            }
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
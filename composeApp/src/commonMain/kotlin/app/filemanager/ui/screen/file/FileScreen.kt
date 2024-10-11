package app.filemanager.ui.screen.file

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.getFileFilterType
import app.filemanager.ui.components.*
import app.filemanager.ui.screen.file.filter.FileFilterScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FileScreen(snackbarHostState: SnackbarHostState) {
    val navigator = LocalNavigator.currentOrThrow

    val fileState = koinInject<FileState>()
    val path by fileState.path.collectAsState()
    val isLoading by fileState.isLoading.collectAsState()
    val exception by fileState.exception.collectAsState()
    val fileInfo by fileState.fileInfo.collectAsState()
    val isRenameFile by fileState.isRenameFile.collectAsState()
    val isViewFile by fileState.isViewFile.collectAsState()

    val fileFilterState = koinInject<FileFilterState>()
    val updateKey by fileFilterState.updateKey.collectAsState()

    val fileOperationState = koinInject<FileOperationState>()

    val scope = rememberCoroutineScope()

    FileFilterButtons(fileState.fileAndFolder) {
        navigator.push(FileFilterScreen())
    }

    val files = fileFilterState.filter(fileState.fileAndFolder, updateKey)
    GridList(isLoading = isLoading, exception) {
        items(files, key = { it.path }) {
            FileCard(
                file = it,
                checkedState = fileState.checkedPath.contains(it.path),
                onStateChange = { status ->
//                    if (status) {
//                        fileState.checkedPath.add(it.path)
//                    } else {
//                        fileState.checkedPath.remove(it.path)
//                    }
                },
                onClick = {
                    scope.launch {
                        if (it.isDirectory) {
                            fileState.updatePath(it.path)
                        } else {
                            FileUtils.openFile(it.path)
                        }
                    }
                },
                onRemove = { deletePath ->
                    scope.launch {
                        val showSnackbar = snackbarHostState.showSnackbar(
                            message = deletePath,
                            actionLabel = "删除",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                        when (showSnackbar) {
                            SnackbarResult.Dismissed -> {}
                            SnackbarResult.ActionPerformed -> {
                                fileOperationState.updateOperationDialog(true)
                                scope.launch {
                                    fileState.deleteFile(
                                        fileOperationState,
                                        deletePath
                                    )
                                    fileState.updateFileAndFolder()
                                    fileFilterState.updateFilerKey()
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    if (isRenameFile && fileInfo != null) {
        FileRenameDialog(fileInfo!!, {
            VerificationUtils.folder(it, fileState.fileAndFolder, listOf(fileInfo!!.name))
        }) {
            fileState.updateRenameFile(false)
            if (it.isEmpty()) {
                fileState.updateFileInfo(null)
                return@FileRenameDialog
            }
            scope.launch {
                fileState.rename(path, fileInfo!!.name, it)
                fileFilterState.updateFilerKey()
                fileState.updateFileInfo(null)
            }
        }
    }
    if (isViewFile && fileInfo != null) {
        FileInfoDialog(fileInfo!!) {
            fileState.updateFileInfo(null)
            fileState.updateViewFile(false)
        }
    }
}


@Composable
fun FileFilterButtons(fileAndFolder: List<FileSimpleInfo>, onToFilterScreen: () -> Unit) {
    val fileFilterState = koinInject<FileFilterState>()
    val extensions =
        fileAndFolder
            .filter { !it.isDirectory }
            .filter {
                if (fileFilterState.isHideFile.value) {
                    !it.isHidden
                } else {
                    true
                }
            }
            .groupBy { it.mineType }
            .mapValues { (_, value) -> value.size }

    val folderCount = fileAndFolder
        .filter { it.isDirectory }
        .count {
            if (fileFilterState.isHideFile.value) {
                !it.isHidden
            } else {
                true
            }
        }

    Row {
        Spacer(Modifier.width(4.dp))
        IconButton(onToFilterScreen) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }

        val filterExtensions = fileFilterState.filterFileTypes
            .filter { filterFileType -> filterFileType.extensions.any { it in extensions.keys } }


        LazyRow(Modifier.weight(1f)) {
            item {
                if (folderCount < 1) return@item

                val isSelected = fileFilterState.filterFileExtensions.contains(FileFilterType.Folder)

                FilterChip(selected = isSelected,
                    label = { Text("文件夹($folderCount)") },
                    leadingIcon = { getFileFilterType(FileFilterType.Folder) },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        if (isSelected) {
                            fileFilterState.filterFileExtensions.remove(FileFilterType.Folder)
                        } else {
                            fileFilterState.filterFileExtensions.add(FileFilterType.Folder)
                        }
                        fileFilterState.updateFilerKey()
                    })
                Spacer(Modifier.width(8.dp))
            }

            itemsIndexed(filterExtensions) { index, fileFilter ->
                val isSelected = fileFilterState.filterFileExtensions.contains(fileFilter.type)
                val fileCount = fileFilter.extensions.intersect(extensions.keys).sumOf { key ->
                    extensions.filterKeys { it == key }.values.sum()
                }

                FilterChip(selected = isSelected,
                    label = { Text("${fileFilter.name}($fileCount)") },
                    leadingIcon = { getFileFilterType(fileFilter.type) },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        if (isSelected) {
                            fileFilterState.filterFileExtensions.remove(fileFilter.type)
                        } else {
                            fileFilterState.filterFileExtensions.add(fileFilter.type)
                        }
                        fileFilterState.updateFilerKey()
                    })
                Spacer(Modifier.width(8.dp))
            }

            item {
                val fileCount = extensions[""] ?: return@item

                val isSelected = fileFilterState.filterFileExtensions.contains(FileFilterType.File)

                FilterChip(selected = isSelected,
                    label = { Text("文件($fileCount)") },
                    leadingIcon = { getFileFilterType(FileFilterType.File) },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        if (isSelected) {
                            fileFilterState.filterFileExtensions.remove(FileFilterType.File)
                        } else {
                            fileFilterState.filterFileExtensions.add(FileFilterType.File)
                        }
                        fileFilterState.updateFilerKey()
                    })
                Spacer(Modifier.width(8.dp))
            }
        }



        Row(Modifier.padding(start = 16.dp, end = 12.dp)) {
            val isHideFile by fileFilterState.isHideFile.collectAsState()
            FilterChip(selected = isHideFile,
                label = { Text("隐藏文件") },
                shape = RoundedCornerShape(25.dp),
                onClick = { fileFilterState.updateHideFile(!isHideFile) })

            SortButton()
        }
    }
}
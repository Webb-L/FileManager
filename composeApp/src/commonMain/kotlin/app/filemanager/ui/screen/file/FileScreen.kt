package app.filemanager.ui.screen.file

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
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
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.ui.components.*
import app.filemanager.ui.components.buttons.FileFilterButtonGroup
import app.filemanager.ui.screen.file.filter.FileFilterScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.Task
import app.filemanager.ui.state.main.TaskType
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

    val scope = rememberCoroutineScope()

    FileFilterButtons(fileState.fileAndFolder) {
        navigator.push(FileFilterScreen())
    }

    val files = fileFilterState.filter(fileState.fileAndFolder, updateKey)
    GridList(isLoading = isLoading, exception) {
        items(files, key = { it.path }) {
            FileCard(
                file = it,
                checkedState = fileState.checkedFileSimpleInfo.contains(it),
                onStateChange = { status ->
                    if (status) {
                        fileState.checkedFileSimpleInfo.add(it)
                    } else {
                        fileState.checkedFileSimpleInfo.remove(it)
                    }
                },
                onClick = {
                    if (fileState.checkedFileSimpleInfo.contains(it)) {
                        fileState.checkedFileSimpleInfo.remove(it)
                        return@FileCard
                    }
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
                            message = it.name,
                            actionLabel = "删除",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                        when (showSnackbar) {
                            SnackbarResult.Dismissed -> {}
                            SnackbarResult.ActionPerformed -> {
//                                fileOperationState.updateOperationDialog(true)
                                scope.launch {
                                    fileState.deleteFile(
                                        Task(
                                            taskType = TaskType.Delete,
                                            status = StatusEnum.LOADING,
                                            values = mutableMapOf("path" to deletePath)
                                        ),
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
    val fileFilterSortType by fileFilterState.sortType.collectAsState()

    Row {
        Spacer(Modifier.width(4.dp))
        IconButton(onToFilterScreen) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }

        FileFilterButtonGroup(
            fileAndFolder = fileAndFolder,
            filterFileTypes = fileFilterState.filterFileTypes,
            filterFileExtensions = fileFilterState.filterFileExtensions,
            isHide = fileFilterState.isHideFile.value,
            onCheckedFileFilterTypeChange = { isSelected, fileFilterType ->
                if (isSelected) {
                    fileFilterState.filterFileExtensions.remove(fileFilterType)
                } else {
                    fileFilterState.filterFileExtensions.add(fileFilterType)
                }
                fileFilterState.updateFilerKey()
            },
            modifier = Modifier.weight(1f)
        )

        Row(Modifier.padding(start = 16.dp, end = 12.dp)) {
            val isHideFile by fileFilterState.isHideFile.collectAsState()
            FilterChip(
                selected = isHideFile,
                label = { Text("隐藏文件") },
                shape = RoundedCornerShape(25.dp),
                onClick = { fileFilterState.updateHideFile(!isHideFile) })

            SortButton(
                sortType = fileFilterSortType,
                onUpdateSort = fileFilterState::updateSortType
            )
        }
    }
}
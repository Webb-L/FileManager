package app.filemanager.ui.screen.file

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.getFileFilterType
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.ui.components.*
import app.filemanager.ui.screen.file.filter.FileFilterScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val mainState = koinInject<MainState>()
        val path by mainState.path.collectAsState()

        val fileState = koinInject<FileState>()
        val fileInfo by fileState.fileInfo.collectAsState()
        val isRenameFile by fileState.isRenameFile.collectAsState()

        val fileFilterState = koinInject<FileFilterState>()
        val updateKey by fileFilterState.updateKey.collectAsState()

        val fileOperationState = koinInject<FileOperationState>()

        val scope = rememberCoroutineScope()

        val fileAndFolder = path.getFileAndFolder()
        FileFilterButtons(fileAndFolder) {
            navigator.push(FileFilterScreen())
        }

        val files = fileFilterState.filter(fileAndFolder, updateKey)
        GridList(files.isEmpty()) {
            items(files) {
                FileCard(
                    file = it,
                    onClick = {
                        if (it.isDirectory) {
                            mainState.updatePath(it.path)
                        } else {
                            FileUtils.openFile(it.path)
                        }
                    },
                    onRemove = { deletePath ->
//                        scope.launch {
//                            val showSnackbar = snackbarHostState.showSnackbar(
//                                message = deletePath,
//                                actionLabel = "删除",
//                                withDismissAction = true,
//                                duration = SnackbarDuration.Short
//                            )
//                            when (showSnackbar) {
//                                SnackbarResult.Dismissed -> {}
//                                SnackbarResult.ActionPerformed -> {
//                                    fileOperationState.updateOperationDialog(true)
//                                    scope.launch {
//                                        fileState.deleteFile(
//                                            fileOperationState,
//                                            deletePath
//                                        )
//                                        fileFilterState.updateFilerKey()
//                                    }
//                                }
//                            }
//                        }
                    }
                )
            }
        }

        if (isRenameFile && fileInfo != null) {
            FileRenameDialog(fileInfo!!, {
                VerificationUtils.folder(it, fileAndFolder, listOf(fileInfo!!.name))
            }) {
                fileState.updateRenameFile(false)
                fileState.updateFileInfo(null)
                if (it.isEmpty()) return@FileRenameDialog
                FileUtils.renameFolder(path, fileInfo!!.name, it)
                fileFilterState.updateFilerKey()
            }
        } else if (fileInfo != null) {
            FileInfoDialog(fileInfo!!) {
                fileState.updateFileInfo(null)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FileFilterButtons(fileAndFolder: List<FileInfo>, onToFilterScreen: () -> Unit) {
        val fileFilterState = koinInject<FileFilterState>()
        val extensions =
            fileAndFolder.filter { !it.isDirectory }.groupBy { it.mineType }.mapValues { (_, value) -> value.size }
        Row {
            Spacer(Modifier.width(4.dp))
            IconButton(onToFilterScreen) {
                Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).weight(1f)) {
                fileFilterState.filterFileTypes
                    .filter { filterFileType -> filterFileType.extensions.any { it in extensions.keys } }
                    .forEachIndexed { index, fileFilter ->
                        val isSelected = fileFilterState.filterFileExtensions.contains(fileFilter.type)
                        val fileCount = fileFilter.extensions.intersect(extensions.keys).map { key ->
                            extensions.filterKeys { it == key }.values.sum()
                        }.sum()

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
}
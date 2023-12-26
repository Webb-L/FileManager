package app.filemanager.ui.screen.file

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import app.filemanager.data.file.getFileFilterIcon
import app.filemanager.extensions.getAllFilesInDirectory
import app.filemanager.ui.components.FileCard
import app.filemanager.ui.components.FileInfoDialog
import app.filemanager.ui.components.FileRenameDialog
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    snackbarHostState: SnackbarHostState,
    updatePath: (String) -> Unit
) {
    val mainState = koinInject<MainState>()
    val path by mainState.path.collectAsState()

    val fileState = koinInject<FileState>()
    val fileInfo by fileState.fileInfo.collectAsState()
    val isRenameFile by fileState.isRenameFile.collectAsState()
    val renameText by fileState.renameText.collectAsState()

    val scope = rememberCoroutineScope()
    FileFilter()
    BoxWithConstraints {
        val columnCount = when (calculateWindowSizeClass(maxWidth, maxHeight)) {
            WindowSizeClass.Compact -> 1
            WindowSizeClass.Medium -> 2
            WindowSizeClass.Expanded -> 3
        }
        LazyVerticalGrid(columns = GridCells.Fixed(columnCount)) {
            items(
                path.getAllFilesInDirectory()
                    .filter { !it.isHidden }
                    .sortedBy { it.isDirectory }
                    .sortedBy { it.name }
            ) {
                FileCard(
                    file = it,
                    onClick = {
                        if (it.isDirectory) {
                            updatePath(it.path)
                        } else {
                            FileUtils.openFile(it.path)
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
                                SnackbarResult.ActionPerformed -> fileState.deleteFile(path)
                            }
                        }
                    }
                )
            }
        }
    }

    if (isRenameFile && fileInfo != null) {
        FileRenameDialog(fileInfo!!) {
            fileState.updateRenameFile(false)
            fileState.updateFileInfo(null)
            if (it.isEmpty()) return@FileRenameDialog
            fileState.updateRenameText(it)
        }
    } else if (fileInfo != null) {
        FileInfoDialog(fileInfo!!) {
            fileState.updateFileInfo(null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileFilter() {
    val fileFilterState = koinInject<FileFilterState>()

    Row {
        Spacer(Modifier.width(4.dp))
        IconButton({}) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).weight(1f)) {
            fileFilterState.filterFileTypes.forEachIndexed { index, fileFilter ->
                val isSelected = fileFilterState.filterFileExtensions.contains(fileFilter.iconType)
                FilterChip(selected = isSelected,
                    label = { Text(fileFilter.name) },
                    leadingIcon = { getFileFilterIcon(fileFilter.iconType) },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        if (isSelected) {
                            fileFilterState.filterFileExtensions.remove(fileFilter.iconType)
                        } else {
                            fileFilterState.filterFileExtensions.add(fileFilter.iconType)
                        }
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
        }
    }
}
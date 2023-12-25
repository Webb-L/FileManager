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
import app.filemanager.ui.components.FileIcon
import app.filemanager.ui.state.file.FileState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.WindowSizeClass
import app.filemanager.utils.calculateWindowSizeClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    path: String,
    fileState: FileState,
    snackbarHostState: SnackbarHostState,
    updatePath: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    FileFilter(fileState)
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
                    fileState = fileState,
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

    val fileInfo = path.getAllFilesInDirectory().first()
    AlertDialog(
        icon = { FileIcon(fileInfo) },
        title = { Text(fileInfo.name) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("位置", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text(fileInfo.path, Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("类型", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text(if (fileInfo.isDirectory) "文件夹" else "文件", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("权限", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("Column 2", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("所属者", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("Column 2", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("所属组", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("Column 2", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("文件大小", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("100KB/100GB", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("创建时间", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("更新时间", Modifier.weight(0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("更新时间", Modifier.weight(0.3f))
                    Spacer(Modifier.width(8.dp))
                    Text("更新时间", Modifier.weight(0.7f))
                }
            }
        },
        onDismissRequest = {

        },
        confirmButton = {
            TextButton(
                onClick = {

                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileFilter(fileState: FileState) {
    Row {
        Spacer(Modifier.width(4.dp))
        IconButton({}) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).weight(1f)) {
            fileState.filterFileTypes.forEachIndexed { index, fileFilter ->
                val isSelected = fileState.filterFileExtensions.contains(fileFilter.iconType)
                FilterChip(selected = isSelected,
                    label = { Text(fileFilter.name) },
                    leadingIcon = { getFileFilterIcon(fileFilter.iconType) },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        if (isSelected) {
                            fileState.filterFileExtensions.remove(fileFilter.iconType)
                        } else {
                            fileState.filterFileExtensions.add(fileFilter.iconType)
                        }
                    })
                Spacer(Modifier.width(8.dp))
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 12.dp)) {
            val isHideFile by fileState.isHideFile.collectAsState()
            FilterChip(selected = isHideFile,
                label = { Text("隐藏文件") },
                shape = RoundedCornerShape(25.dp),
                onClick = { fileState.updateHideFile(!isHideFile) })
        }
    }
}
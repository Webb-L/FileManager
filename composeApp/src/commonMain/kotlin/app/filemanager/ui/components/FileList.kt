package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileFilterSort
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.file.PathInfo
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.filter
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.buttons.FileFilterButtonGroup
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FileListComponent(
    openPath: String,
    onFilesSelected: (List<FileSimpleInfo>) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val fileFilterState = koinInject<FileFilterState>()

    val rootFileInfos = mutableStateListOf<PathInfo>()
    var rootPath by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        rootFileInfos.clear()
        rootFileInfos.addAll(PathUtils.getRootPaths().getOrDefault(listOf()))
        rootPath = rootFileInfos.first().path
    }

    var path by remember { mutableStateOf(openPath) }
    var files by remember { mutableStateOf<List<FileSimpleInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var exception by remember { mutableStateOf<Throwable?>(null) }
    val checkedFiles = mutableStateListOf<FileSimpleInfo>()
    val paths = mutableStateListOf<String>()
    paths.addAll(path.parsePath())

    var isHideFile by remember { mutableStateOf(false) }
    var fileFilterSortType by remember { mutableStateOf(FileFilterSort.NameAsc) }

    // 过滤的文件类型
    val filterFileExtensions = mutableStateListOf<FileFilterType>()

    fun loadFilesFromPath() {
        paths.clear()
        paths.addAll(path.parsePath())

        exception = null
        isLoading = true
        path.getFileAndFolder()
            .onSuccess {
                files =
                    it.filter(
                        isHidden = isHideFile,
                        sortType = fileFilterSortType,
                        filterFileTypes = fileFilterState.filterFileTypes,
                        filterFileExtensions = filterFileExtensions,
                    )
                isLoading = false
            }
            .onFailure {
                exception = it
                isLoading = false
            }
    }

    println(1111)

    LaunchedEffect(path) {
        loadFilesFromPath()
    }

    Column {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)
        LazyRow(state = listState) {
            item {
                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    FilterChip(
                        selected = false,
                        label = { Text(rootPath) },
                        border = null,
                        shape = RoundedCornerShape(25.dp),
                        trailingIcon = if (rootFileInfos.size > 1) {
                            {
                                Icon(
                                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    null,
                                    Modifier.clip(RoundedCornerShape(25.dp)).clickable { expanded = !expanded }
                                )
                            }
                        } else {
                            null
                        },
                        onClick = { path = rootPath }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        for (fileInfo in rootFileInfos) {
                            DropdownMenuItem(
                                text = { Text(fileInfo.path) },
                                onClick = {
                                    path = fileInfo.path
                                    expanded = false
                                })
                        }
                    }
                }
            }

            itemsIndexed(paths) { index, text ->
                val nowPath =
                    PathUtils.getPathSeparator() + paths.subList(0, index).joinToString(PathUtils.getPathSeparator())
                PathSwitch(
                    text,
                    nowPath,
                    onClick = { path = nowPath + PathUtils.getPathSeparator() + text },
                    onSelected = { path = it }
                )
            }
        }

        Row {
            FileFilterButtonGroup(
                fileAndFolder = files,
                filterFileTypes = fileFilterState.filterFileTypes,
                filterFileExtensions = filterFileExtensions,
                isHide = isHideFile,
                onCheckedFileFilterTypeChange = { isSelected, fileFilterType ->
                    if (isSelected) {
                        filterFileExtensions.remove(fileFilterType)
                    } else {
                        filterFileExtensions.add(fileFilterType)
                    }
                    loadFilesFromPath()
                },
                modifier = Modifier.weight(1f)
            )

            Row(Modifier.padding(start = 16.dp, end = 12.dp)) {
                FilterChip(
                    selected = isHideFile,
                    label = { Text("隐藏文件") },
                    shape = RoundedCornerShape(25.dp),
                    onClick = {
                        isHideFile = !isHideFile
                        loadFilesFromPath()
                    })

                SortButton(
                    sortType = fileFilterSortType,
                    onUpdateSort = {
                        fileFilterSortType = it
                        loadFilesFromPath()
                    }
                )
            }
        }

        LaunchedEffect(paths) {
            listState.scrollToItem(paths.size)
        }

        GridList(isLoading = isLoading, exception = if (files.isEmpty()) EmptyDataException() else exception) {
            items(files, key = { it.path }) { file ->
                FileCard(
                    file = file,
                    checkedState = checkedFiles.contains(file),
                    onStateChange = { status ->
                        if (status) {
                            checkedFiles.add(file)
                        } else {
                            checkedFiles.remove(file)
                        }
                        onFilesSelected(checkedFiles.toList())
                    },
                    onClick = {
                        if (checkedFiles.contains(file)) {
                            checkedFiles.remove(file)
                            onFilesSelected(checkedFiles.toList())
                            return@FileCard
                        }
                        scope.launch {
                            if (file.isDirectory) {
                                path = file.path
                            }
                        }
                    },
                    onRemove = { deletePath ->
                    }
                )
            }
        }
    }
}

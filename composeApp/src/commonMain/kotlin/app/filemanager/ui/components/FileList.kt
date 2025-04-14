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
import app.filemanager.data.file.*
import app.filemanager.db.FileFilter
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.filter
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.inject
import kotlin.getValue

@Composable
fun FileListComponent(
    openPath: String,
    onFilesSelected: (List<FileSimpleInfo>) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val rootFileInfos = mutableStateListOf<PathInfo>()
    var rootPath by remember { mutableStateOf("") }

    val database = koinInject<FileManagerDatabase>()


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
    var fileFilterSortType by remember { mutableStateOf<FileFilterSort>(FileFilterSort.NameAsc) }
    val filterFileTypes = mutableStateListOf<FileFilter>()
    filterFileTypes.addAll(database.fileFilterQueries.queryAllByLimit(0, 100).executeAsList())

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
                    it.filter(isHidden = isHideFile, sortType = fileFilterSortType, filterFileTypes = filterFileTypes)
                isLoading = false
            }
            .onFailure {
                exception = it
                isLoading = false
            }
    }

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

        val extensions =
            files
                .filter { !it.isDirectory }
                .filter {
                    if (!isHideFile) {
                        !it.isHidden
                    } else {
                        true
                    }
                }
                .groupBy { it.mineType }
                .mapValues { (_, value) -> value.size }

        val folderCount = files
            .filter { it.isDirectory }
            .count {
                if (!isHideFile) {
                    !it.isHidden
                } else {
                    true
                }
            }

        Row {
            val filterExtensions = filterFileTypes
                .filter { filterFileType -> filterFileType.extensions.any { it in extensions.keys } }

            LazyRow(Modifier.weight(1f)) {
                item {
                    if (folderCount < 1) return@item

                    val isSelected = filterFileExtensions.contains(FileFilterType.Folder)

                    FilterChip(
                        selected = isSelected,
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
                    val isSelected = filterFileExtensions.contains(fileFilter.type)
                    val fileCount = fileFilter.extensions.intersect(extensions.keys).sumOf { key ->
                        extensions.filterKeys { it == key }.values.sum()
                    }

                    FilterChip(
                        selected = isSelected,
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

                    val isSelected = filterFileExtensions.contains(FileFilterType.File)

                    FilterChip(
                        selected = isSelected,
                        label = { Text("文件($fileCount)") },
                        leadingIcon = { getFileFilterType(FileFilterType.File) },
                        shape = RoundedCornerShape(25.dp),
                        onClick = {
                            if (isSelected) {
                                filterFileExtensions.remove(FileFilterType.File)
                            } else {
                                filterFileExtensions.add(FileFilterType.File)
                            }
                            fileFilterState.updateFilerKey()
                        })
                    Spacer(Modifier.width(8.dp))
                }
            }

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

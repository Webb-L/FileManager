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

/**
 * 文件选择器组件，用于浏览和选择文件/文件夹
 *
 * @param openPath 初始打开的路径
 * @param defaultCheckedFiles 默认选中的文件列表
 * @param onFilesSelected 文件选择回调函数，返回当前选中的文件列表
 * @param fileFilterType 可选的文件过滤类型，用于只显示特定类型的文件
 * @param isSingleSelection 是否为单选模式，true表示只能选择一个文件/文件夹
 */
@Composable
fun FileSelector(
    openPath: String,
    defaultCheckedFiles: List<FileSimpleInfo> = listOf(),
    onFilesSelected: (List<FileSimpleInfo>) -> Unit = {},
    fileFilterType: FileFilterType? = null,
    isSingleSelection: Boolean = false,
) {
    // 协程作用域
    val scope = rememberCoroutineScope()

    val fileFilterState = koinInject<FileFilterState>()

    val rootFileInfos = mutableStateListOf<PathInfo>()
    var rootPath by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        rootFileInfos.clear()
        rootFileInfos.addAll(PathUtils.getRootPaths().getOrDefault(listOf()))
        rootPath = rootFileInfos.first().path
    }

    // 当前路径
    var path by remember { mutableStateOf(openPath) }
    // 当前路径下的文件列表
    var files by remember { mutableStateOf<List<FileSimpleInfo>>(emptyList()) }
    // 加载状态
    var isLoading by remember { mutableStateOf(false) }
    // 异常信息
    var exception by remember { mutableStateOf<Throwable?>(null) }
    // 已选中的文件列表
    val checkedFiles = mutableStateListOf<FileSimpleInfo>().apply {
        addAll(defaultCheckedFiles)
    }
    // 路径分解后的分段列表
    val paths = mutableStateListOf<String>()
    paths.addAll(path.parsePath())

    // 是否隐藏文件
    var isHideFile by remember { mutableStateOf(false) }
    // 文件排序类型
    var fileFilterSortType by remember { mutableStateOf(FileFilterSort.NameAsc) }

    // 过滤的文件类型
    var filterFileExtensions by remember { mutableStateOf<List<FileFilterType>>(listOf()) }

    /**
     * 从当前路径加载文件列表
     * 1. 清空并更新路径分段
     * 2. 重置异常状态
     * 3. 设置加载状态
     * 4. 异步获取文件列表
     * 5. 根据结果更新状态
     */
    fun loadFilesFromPath() {
        // 更新路径分段
        paths.clear()
        paths.addAll(path.parsePath())

        // 重置状态
        exception = null
        isLoading = true

        // 异步加载文件
        path.getFileAndFolder()
            .onSuccess { fileList ->
                // 应用文件类型过滤
                files = fileList.filter(
                    filterFileExtensions = fileFilterType?.let { listOf(it) } ?: emptyList()
                )
                isLoading = false
            }
            .onFailure { error ->
                exception = error
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

        Row {
            FileFilterButtonGroup(
                fileAndFolder = files,
                filterFileTypes = fileFilterState.filterFileTypes,
                filterFileExtensions = filterFileExtensions,
                isHide = isHideFile,
                onCheckedFileFilterTypeChange = { isSelected, fileFilterType ->
                    filterFileExtensions = if (isSelected) {
                        filterFileExtensions - fileFilterType
                    } else {
                        filterFileExtensions + fileFilterType
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

        val filterFiles = files.filter(
            isHidden = isHideFile,
            sortType = fileFilterSortType,
            filterFileTypes = fileFilterState.filterFileTypes,
            filterFileExtensions = filterFileExtensions,
        )

        GridList(isLoading = isLoading, exception = if (filterFiles.isEmpty()) EmptyDataException() else exception) {
            items(filterFiles, key = { it.path }) { file ->
                FileCard(
                    file = file,
                    checkedState = checkedFiles.contains(file),
                    onStateChange = { status ->
                        if (status) {
                            if (isSingleSelection) {
                                checkedFiles.clear()
                                checkedFiles.add(file)
                            } else {
                                checkedFiles.add(file)
                            }
                        } else {
                            checkedFiles.remove(file)
                        }
                        onFilesSelected(checkedFiles)
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

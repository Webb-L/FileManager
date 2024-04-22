package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
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
import app.filemanager.data.file.FileInfo
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.buttons.DiskSwitchButton
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileState
import app.filemanager.utils.NaturalOrderComparator
import app.filemanager.utils.PathUtils
import app.filemanager.utils.PathUtils.getRootPaths
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun AppBarPath() {
    val scope = rememberCoroutineScope()

    val fileState = koinInject<FileState>()
    val path by fileState.path.collectAsState()
    val rootPath by fileState.rootPath.collectAsState()
    val deskType by fileState.deskType.collectAsState()

    val paths = path.parsePath()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)
    LazyRow(state = listState) {
        item {
            DiskSwitchButton(
                deskType,
                fileState::updateDesk
            )
        }

        item {
            RootPathSwitch()
        }

        itemsIndexed(paths) { index, text ->
            val nowPath = rootPath + paths.subList(0, index).joinToString(PathUtils.getPathSeparator())
            PathSwitch(
                text,
                nowPath,
                onClick = {
                    scope.launch {
                        val newPath = rootPath + paths.subList(0, index + 1)
                            .joinToString(PathUtils.getPathSeparator())
                        fileState.updatePath(newPath)
                    }
                },
                onSelected = {
                    scope.launch {
                        fileState.updatePath(it)
                    }
                }
            )
        }
    }

    LaunchedEffect(paths) {
        listState.scrollToItem(paths.size)
    }
}

/**
 * 根路径切换组件，用于显示根路径的切换按钮
 */
@Composable
fun RootPathSwitch() {
    val scope = rememberCoroutineScope()

    val fileState = koinInject<FileState>()
    val rootPath by fileState.rootPath.collectAsState()
    val fileInfos = mutableStateListOf<FileSimpleInfo>()

    LaunchedEffect(fileState.deskType.value) {
        fileInfos.clear()
        fileInfos.addAll(fileState.getRootPaths())
    }

    renderPathSwitch(
        name = rootPath,
        fileInfos = fileInfos,
        selected = false,
        onClick = {
            scope.launch {
                fileState.updatePath(rootPath)
            }
        },
        onSelected = {
            scope.launch {
                fileState.updateRootPath(it)
                fileState.updatePath(it)
            }
        }
    )
}

/**
 * 路径切换组件，用于显示路径的切换按钮
 *
 * @param name 组件显示的名称
 * @param path 要切换的路径
 * @param selected 是否选中状态，默认为false
 * @param onClick 点击事件回调函数
 * @param onSelected 选中回调函数，参数为选中的路径
 */
@Composable
fun PathSwitch(
    name: String,
    path: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    onSelected: (String) -> Unit
) {
    val fileState = koinInject<FileState>()
    var fileInfos by remember { mutableStateOf<List<FileSimpleInfo>>(listOf()) }

    val fileFilterState = koinInject<FileFilterState>()

    LaunchedEffect(Unit) {
        fileInfos = fileState
            .getFileAndFolder(path)
            .filter { it.isDirectory }
            .filter { it.isHidden != fileFilterState.isHideFile.value }
            .sortedWith(NaturalOrderComparator())
    }

    renderPathSwitch(name, fileInfos, selected, onClick, onSelected)
}

/**
 * 渲染路径切换组件
 *
 * @param name 组件显示的名称
 * @param fileInfos 路径信息列表
 * @param selected 是否选中状态
 * @param onClick 点击事件回调函数
 * @param onSelected 选中回调函数，参数为选中的路径
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun renderPathSwitch(
    name: String,
    fileInfos: List<FileSimpleInfo>,
    selected: Boolean,
    onClick: () -> Unit,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        FilterChip(
            selected = selected,
            label = { Text(name) },
            border = null,
            shape = RoundedCornerShape(25.dp),
            trailingIcon = if (fileInfos.size > 1) {
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
            onClick = onClick
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (fileInfo in fileInfos) {
                DropdownMenuItem(
                    text = { Text(fileInfo.name) },
                    onClick = {
                        onSelected(fileInfo.path)
                        expanded = false
                    })
            }
        }
    }
}

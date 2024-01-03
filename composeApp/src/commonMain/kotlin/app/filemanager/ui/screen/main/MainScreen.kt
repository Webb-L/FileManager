package app.filemanager.ui.screen.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.AppDrawer
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.PathUtils
import app.filemanager.utils.PathUtils.getRootPaths
import app.filemanager.utils.WindowSizeClass
import cafe.adriel.voyager.navigator.Navigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(screenType: WindowSizeClass) {
    val mainState = koinInject<MainState>()
    val expandDrawer by mainState.isExpandDrawer.collectAsState()

    // 小屏
    if (screenType == WindowSizeClass.Compact) {
        ModalNavigationDrawer(
            drawerState = DrawerState(
                if (expandDrawer) DrawerValue.Open else DrawerValue.Closed,
                confirmStateChange = {
                    if (it == DrawerValue.Closed) {
                        mainState.updateExpandDrawer(false)
                    }
                    true
                },
            ),
            drawerContent = { AppDrawer() },
        ) {
            Navigator(HomeScreen)
        }
    } else {
        Row {
            if (listOf(WindowSizeClass.Medium, WindowSizeClass.Expanded).contains(screenType) && expandDrawer) {
                AppDrawer()
            }
            Navigator(HomeScreen)
        }
    }
}

@Composable
fun AppBarPath() {
    val mainState = koinInject<MainState>()
    val path by mainState.path.collectAsState()
    val rootPath by mainState.rootPath.collectAsState()

    val paths = path.parsePath()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)
    LazyRow(state = listState) {
        item {
            PathSwitch(
                mainState.rootPath.value,
                getRootPaths().map {
                    FileInfo(
                        name = it,
                        description = "",
                        isDirectory = true,
                        isHidden = false,
                        path = it,
                        mineType = "",
                        size = 0,
                        permissions = 0,
                        user = "",
                        userGroup = "",
                        createdDate = 0,
                        updatedDate = 0
                    )
                },
                onClick = {
                    mainState.updatePath(mainState.rootPath.value)
                },
                onSelected = {
                    mainState.updateRootPath(it)
                    mainState.updatePath(it)
                }
            )
        }
        itemsIndexed(paths) { index, text ->
            val nowPath = rootPath + paths.subList(0, index).joinToString(PathUtils.getPathSeparator())
            PathSwitch(
                text,
                nowPath.getFileAndFolder().filter { it.isDirectory }.sortedBy { it.name },
                onClick = {
                    val newPath = rootPath + paths.subList(0, index + 1)
                        .joinToString(PathUtils.getPathSeparator())
                    mainState.updatePath(newPath)
                },
                onSelected = { mainState.updatePath(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PathSwitch(name: String, fileInfos: List<FileInfo>, onClick: () -> Unit, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        FilterChip(
            selected = expanded,
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
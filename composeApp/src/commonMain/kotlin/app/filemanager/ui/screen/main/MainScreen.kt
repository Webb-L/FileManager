package app.filemanager.ui.screen.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.AppDrawer
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.PathUtils.getPathSeparator
import app.filemanager.utils.PathUtils.getRootPaths
import app.filemanager.utils.WindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainState: MainState, screenType: WindowSizeClass) {
    val path by mainState.path.collectAsState()
    val rootPath by mainState.rootPath.collectAsState()
    val isSearchText by mainState.isSearchText.collectAsState()
    val searchText by mainState.searchText.collectAsState()

    val fileState = FileState()

    Row {
        val expandDrawer by mainState.isExpandDrawer.collectAsState()
        if (listOf(WindowSizeClass.Medium, WindowSizeClass.Expanded).contains(screenType) && expandDrawer) {
            AppDrawer(mainState)
        }

        Column {
            val paths = path.parsePath()
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)
            TopAppBar(
                title = {
                    LazyRow(state = listState) {
                        item {
                            RootPathSwitch(mainState)
                        }
                        itemsIndexed(paths) { index, text ->
                            FilterChip(selected = false,
                                label = { Text(text) },
                                border = null,
                                shape = RoundedCornerShape(25.dp),
                                onClick = {
                                    val newPath = rootPath + paths.subList(0, index + 1)
                                        .joinToString(getPathSeparator())
                                    mainState.updatePath(newPath)
                                })
                        }
                    }
                },
                navigationIcon = {
                    IconButton({
                        mainState.updateExpandDrawer(!expandDrawer)
                    }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton({ mainState.updateSearch(!isSearchText) }) {
                        Icon(Icons.Default.Search, null)
                    }
                    IconButton({}) {
                        Icon(Icons.Default.Sort, null)
                    }
                }
            )
            if (isSearchText) {
                Row(modifier = Modifier.fillMaxWidth().padding(end = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextField(searchText, label = { Text("搜索") }, onValueChange = mainState::updateSearchText)
                }
            }
            FileScreen(path, fileState) {
                mainState.updatePath(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootPathSwitch(mainState: MainState) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        FilterChip(selected = expanded,
            label = { Text(mainState.rootPath.value) },
            border = null,
            shape = RoundedCornerShape(25.dp),
            trailingIcon = if (getRootPaths().size > 1) {
                {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        null,
                        Modifier.clickable { expanded = !expanded }
                    )
                }
            } else {
                null
            },
            onClick = { mainState.updatePath(mainState.rootPath.value) })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (rootPath in getRootPaths()) {
                DropdownMenuItem(
                    text = { Text(rootPath) },
                    onClick = {
                        mainState.updateRootPath(rootPath)
                        mainState.updatePath(rootPath)
                        expanded = false
                    })
            }
        }
    }
}
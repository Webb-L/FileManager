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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.FileInfo
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.AppDrawer
import app.filemanager.ui.components.SortButton
import app.filemanager.ui.components.TextFieldDialog
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.PathUtils
import app.filemanager.utils.PathUtils.getRootPaths
import app.filemanager.utils.WindowSizeClass
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
            MainScreenContainer()
        }
    } else {
        Row {
            if (listOf(WindowSizeClass.Medium, WindowSizeClass.Expanded).contains(screenType) && expandDrawer) {
                AppDrawer()
            }
            MainScreenContainer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContainer() {
    val mainState = koinInject<MainState>()
    val path by mainState.path.collectAsState()
    val rootPath by mainState.rootPath.collectAsState()
    val expandDrawer by mainState.isExpandDrawer.collectAsState()
    val editPath by mainState.isEditPath.collectAsState()

    val fileFilterState = koinInject<FileFilterState>()
    val isSearchText by fileFilterState.isSearchText.collectAsState()
    val searchText by fileFilterState.searchText.collectAsState()

    val fileState = koinInject<FileState>()
    val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
    val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

    val paths = path.parsePath()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = paths.size - 1)

    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                                nowPath.getFileAndFolder().filter { it.isDirectory },
                                onClick = {
                                    val newPath = rootPath + paths.subList(0, index + 1)
                                        .joinToString(PathUtils.getPathSeparator())
                                    println(newPath)
                                    mainState.updatePath(newPath)
                                },
                                onSelected = {
                                    mainState.updatePath(it)
                                }
                            )
//                            FilterChip(selected = false,
//                                label = { Text(text) },
//                                border = null,
//                                shape = RoundedCornerShape(25.dp),
//                                onClick = {
//                                    val newPath = rootPath + paths.subList(0, index + 1)
//                                        .joinToString(PathUtils.getPathSeparator())
//                                    println(newPath)
//                                    mainState.updatePath(newPath)
//                                })
                        }
                    }
                },
                navigationIcon = {
                    IconButton({ mainState.updateExpandDrawer(!expandDrawer) }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                    IconButton({ mainState.updateExpandDrawer(!expandDrawer) }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton({ mainState.updateEditPath(true) }) {
                        Icon(Icons.Default.Edit, null)
                    }
                    IconButton({ fileFilterState.updateSearch(!isSearchText) }) {
                        Icon(Icons.Default.Search, null)
                    }
                    SortButton()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isPasteCopyFile || isPasteMoveFile) {
                BottomAppBar(
                    actions = {
                        if (isPasteCopyFile || isPasteMoveFile) {
                            IconButton({
                                if (isPasteCopyFile) fileState.pasteCopyFile(path)
                                if (isPasteMoveFile) fileState.pasteMoveFile(path)
                            }) {
                                Icon(Icons.Filled.ContentPaste, null)
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { fileState.pasteCopyFile(path) },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    }
                )
            }
        },
//        floatingActionButton = {
//            if (!isPasteCopyFile && !isPasteMoveFile) {
//                ExtendedFloatingActionButton({ }) {
//                    Icon(Icons.Filled.Add, null)
//                    Spacer(Modifier.width(8.dp))
//                    Text("新增")
//                }
//            }
//        }
    ) {
        Column(Modifier.padding(it)) {
            if (isSearchText) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextField(
                        searchText,
                        label = { Text("搜索") },
                        onValueChange = fileFilterState::updateSearchText
                    )
                }
            }
            FileScreen(snackbarHostState) {
                mainState.updatePath(it)
            }
        }
    }

    if (editPath) {
        TextFieldDialog("修改目录", label = "目录", initText = path) {
            mainState.updateEditPath(false)
            if (it.isEmpty()) return@TextFieldDialog
            if (it.parsePath().isNotEmpty()) {
                mainState.updatePath(it)
            }
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
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
import app.filemanager.ui.components.SortButton
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
                        item { RootPathSwitch() }
                        itemsIndexed(paths) { index, text ->
                            FilterChip(selected = false,
                                label = { Text(text) },
                                border = null,
                                shape = RoundedCornerShape(25.dp),
                                onClick = {
                                    val newPath = rootPath + paths.subList(0, index + 1)
                                        .joinToString(PathUtils.getPathSeparator())
                                    mainState.updatePath(newPath)
                                })
                        }
                    }
                },
                navigationIcon = {
                    IconButton({ mainState.updateExpandDrawer(!expandDrawer) }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                },
                actions = {
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
        floatingActionButton = {
            if (!isPasteCopyFile && !isPasteMoveFile) {
                ExtendedFloatingActionButton({ }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
            }
        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootPathSwitch() {
    val mainState = koinInject<MainState>()
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
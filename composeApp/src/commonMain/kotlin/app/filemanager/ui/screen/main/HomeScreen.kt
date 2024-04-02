package app.filemanager.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.FileOperationDialog
import app.filemanager.ui.components.FileWarningOperationDialog
import app.filemanager.ui.components.TextFieldDialog
import app.filemanager.ui.screen.file.FavoriteScreen
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.VerificationUtils
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val mainState = koinInject<MainState>()
        val path by mainState.path.collectAsState()
        val editPath by mainState.isEditPath.collectAsState()
        val isCreateFolder by mainState.isCreateFolder.collectAsState()
        val isFavorite by mainState.isFavorite.collectAsState()

        val fileFilterState = koinInject<FileFilterState>()
        val isSearchText by fileFilterState.isSearchText.collectAsState()
        val searchText by fileFilterState.searchText.collectAsState()

        val fileState = koinInject<FileState>()
        val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
        val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

        val fileOperationState = koinInject<FileOperationState>()
        val isOperationDialog by fileOperationState.isOperationDialog.collectAsState()
        val isWarningOperationDialog by fileOperationState.isWarningOperationDialog.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = { HomeTopBar() },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { HomeBottomBar() },
            floatingActionButton = {
                if (!isPasteCopyFile && !isPasteMoveFile) {
                    ExtendedFloatingActionButton({ mainState.updateCreateFolder(true) }) {
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
                FileScreen(snackbarHostState)
            }
        }

        LaunchedEffect(isFavorite) {
            if (isFavorite) {
                navigator.push(FavoriteScreen())
            }
        }

        if (isCreateFolder) {
            TextFieldDialog(
                "新增文件夹",
                label = "名称",
                verifyFun = { text -> VerificationUtils.folder(text, path.getFileAndFolder()) }
            ) {
                mainState.updateCreateFolder(false)
                if (it.isEmpty()) return@TextFieldDialog
                FileUtils.createFolder(path, it)
                fileFilterState.updateFilerKey()
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

        if (isOperationDialog) {
            FileOperationDialog(
                onCancel = {
                    fileOperationState.isCancel = true
                    fileOperationState.updateOperationDialog(false)
                },
                onDismiss = {
                    fileOperationState.updateOperationDialog(false)
                }
            )
        }

        if (isWarningOperationDialog) {
            FileWarningOperationDialog()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeTopBar() {
        val mainState = koinInject<MainState>()
        val expandDrawer by mainState.isExpandDrawer.collectAsState()

        val fileFilterState = koinInject<FileFilterState>()
        val isSearchText by fileFilterState.isSearchText.collectAsState()

        TopAppBar(
            title = { AppBarPath() },
            navigationIcon = {
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
            }
        )
    }

    @Composable
    fun HomeBottomBar() {
        val scope = rememberCoroutineScope()

        val mainState = koinInject<MainState>()
        val path by mainState.path.collectAsState()

        val fileOperationState = koinInject<FileOperationState>()

        val fileState = koinInject<FileState>()
        val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
        val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

        if (isPasteCopyFile || isPasteMoveFile) {
            BottomAppBar(
                actions = {
                    if (isPasteCopyFile || isPasteMoveFile) {
                        IconButton({
                            fileOperationState.updateOperationDialog(true)
                            scope.launch {
                                withContext(Dispatchers.Default) {
                                    if (isPasteCopyFile) {
                                        fileState.pasteCopyFile(path, fileOperationState)
                                    }
                                    if (isPasteMoveFile) {
                                        fileState.pasteMoveFile(path, fileOperationState)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentPaste, null)
                        }
                    }

                    IconButton({
                        if (isPasteCopyFile) fileState.cancelCopyFile()
                        if (isPasteMoveFile) fileState.cancelMoveFile()
                    }) {
                        Icon(Icons.Filled.Close, null)
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { mainState.updateCreateFolder(true) },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Filled.Add, null)
                    }
                }
            )
        }
    }
}
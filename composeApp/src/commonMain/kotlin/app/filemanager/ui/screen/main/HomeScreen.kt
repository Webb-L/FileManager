package app.filemanager.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.*
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
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

        val scope = rememberCoroutineScope()

        val mainState = koinInject<MainState>()
        val screen by mainState.screen.collectAsState()
        val editPath by mainState.isEditPath.collectAsState()

        val fileFilterState = koinInject<FileFilterState>()
        val isSearchText by fileFilterState.isSearchText.collectAsState()
        val searchText by fileFilterState.searchText.collectAsState()

        val fileState = koinInject<FileState>()
        val path by fileState.path.collectAsState()
        val isCreateFolder by fileState.isCreateFolder.collectAsState()
        val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
        val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()

        val fileOperationState = koinInject<FileOperationState>()
        val isWarningOperationDialog by fileOperationState.isWarningOperationDialog.collectAsState()

        val deviceState = koinInject<DeviceState>()

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = { HomeTopBar() },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { HomeBottomBar() },
            floatingActionButton = {
                if (fileState.checkedPath.isEmpty()) {
                    ExtendedFloatingActionButton({ fileState.updateCreateFolder(true) }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("新增")
                    }
                }
            }
        ) {
            Column(Modifier.padding(it)) {
                deviceState.connectionRequest.entries
                    .firstOrNull { it.value.first == DeviceConnectType.WAITING }
                    ?.let { entry ->
                        deviceState.socketDevices.firstOrNull { it.id == entry.key }?.let { device ->
                            MaterialBannerDeviceConnect(device)
                        }
                    }
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

        LaunchedEffect(screen) {
            if (screen != null) {
                navigator.push(screen!!)
            }
        }

        if (isCreateFolder) {
            TextFieldDialog(
                "新增文件夹",
                label = "名称",
                verifyFun = { text -> VerificationUtils.folder(text, fileState.fileAndFolder) }
            ) {
                fileState.updateCreateFolder(false)
                if (it.isEmpty()) return@TextFieldDialog
                scope.launch {
                    val result = fileState.createFolder(path, it)
                    if (result.isFailure) {
                        snackbarHostState.showSnackbar(
                            message = result.exceptionOrNull()?.message ?: "创建失败",
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }

        if (editPath) {
            TextFieldDialog("修改目录", label = "目录", initText = path) {
                mainState.updateEditPath(false)
                if (it.isEmpty()) return@TextFieldDialog
                if (it.parsePath().isNotEmpty()) {
                    scope.launch {
                        fileState.updatePath(it)
                    }
                }
            }
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


        val fileOperationState = koinInject<FileOperationState>()

        val fileState = koinInject<FileState>()
        val path by fileState.path.collectAsState()
        val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
        val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()


        val fileFilterState = koinInject<FileFilterState>()
        val updateKey by fileFilterState.updateKey.collectAsState()

        if (fileState.checkedPath.isNotEmpty()) {
            BottomAppBar(
                actions = {
                    val files = fileFilterState.filter(fileState.fileAndFolder, updateKey)
                    val isCheckedAll =
                        fileState.checkedPath.size == files.count { fileState.checkedPath.contains(it.path) } &&
                                fileState.checkedPath.size == files.size
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .toggleable(
                                value = isCheckedAll,
                                onValueChange = {
                                    fileState.checkedPath.clear()
                                    if (!isCheckedAll) {
                                        fileState.checkedPath.addAll(files.map { it.path })
                                    }
                                },
                                role = Role.Checkbox,
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(isCheckedAll, onCheckedChange = null)
                    }

                    if (isPasteCopyFile || isPasteMoveFile) {
                        IconButton({
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
                        fileState.checkedPath.clear()
                    }) {
                        Icon(Icons.Filled.Close, null)
                    }

                    Spacer(Modifier.weight(1f))

                    FileBottomAppMenu()

                    Spacer(Modifier.width(8.dp))
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { fileState.updateCreateFolder(true) },
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
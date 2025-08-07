@file:OptIn(kotlin.time.ExperimentalTime::class)

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
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.Local
import app.filemanager.data.main.Share
import app.filemanager.extensions.parsePath
import app.filemanager.ui.components.*
import app.filemanager.ui.screen.file.FileScreen
import app.filemanager.ui.state.file.FileFilterState
import app.filemanager.ui.state.file.FileOperationState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import app.filemanager.ui.state.main.Task
import app.filemanager.ui.state.main.TaskType
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
        mainState.navigator = navigator
        val editPath by mainState.isEditPath.collectAsState()

        val fileFilterState = koinInject<FileFilterState>()
        val isSearchText by fileFilterState.isSearchText.collectAsState()
        val searchText by fileFilterState.searchText.collectAsState()

        val fileState = koinInject<FileState>()
        val path by fileState.path.collectAsState()
        val isCreateFolder by fileState.isCreateFolder.collectAsState()
        val deskType by fileState.deskType.collectAsState()

        val fileOperationState = koinInject<FileOperationState>()
        val isWarningOperationDialog by fileOperationState.isWarningOperationDialog.collectAsState()

        val deviceState = koinInject<DeviceState>()
        
        var showSearchDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = { HomeTopBar { showSearchDialog = true } },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { HomeBottomBar(snackbarHostState) },
            floatingActionButton = {
                if (fileState.checkedFileSimpleInfo.isEmpty() && deskType is Device) {
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
                deviceState.shareRequest.entries
                    .firstOrNull { it.value.first == DeviceConnectType.WAITING }
                    ?.let { entry ->
                        deviceState.socketDevices.firstOrNull { it.id == entry.key }?.let { device ->
                            MaterialBannerDeviceShare(device)
                        }
                    }
                FileScreen(snackbarHostState)
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
                scope.launch(Dispatchers.Default) {
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
                    scope.launch(Dispatchers.Default) {
                        fileState.updatePath(it)
                    }
                }
            }
        }

        if (isWarningOperationDialog) {
            FileWarningOperationDialog()
        }

        if (showSearchDialog) {
            SearchDialog(
                searchText = searchText,
                onDismiss = { 
                    showSearchDialog = false
                    fileFilterState.updateSearch(false)
                },
                onConfirm = { query ->
                    showSearchDialog = false
                    fileFilterState.updateSearchText(query)
                    fileFilterState.updateSearch(true)
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeTopBar(onSearchClick: () -> Unit = {}) {
        val mainState = koinInject<MainState>()
        val expandDrawer by mainState.isExpandDrawer.collectAsState()

        TopAppBar(
            title = { AppBarPath() },
            navigationIcon = {
                IconButton({ mainState.updateExpandDrawer(!expandDrawer) }) {
                    BadgedBox(badge = {
                        if (!expandDrawer) {
                            Badge { Text("1") }
                        }
                    }) {
                        Icon(if (expandDrawer) Icons.Default.Close else Icons.Default.Menu, null)
                    }
                }
            },
            actions = {
                IconButton({ mainState.updateEditPath(true) }) {
                    Icon(Icons.Default.Edit, null)
                }
                IconButton(onSearchClick) {
                    Icon(Icons.Default.Search, null)
                }
            }
        )
    }

    @Composable
    fun HomeBottomBar(snackbarHostState: SnackbarHostState) {
        val scope = rememberCoroutineScope()

        val fileOperationState = koinInject<FileOperationState>()

        val fileState = koinInject<FileState>()
        val path by fileState.path.collectAsState()
        val isPasteCopyFile by fileState.isPasteCopyFile.collectAsState()
        val isPasteMoveFile by fileState.isPasteMoveFile.collectAsState()
        val deskType by fileState.deskType.collectAsState()
        val isShareType = deskType is Share

        val fileFilterState = koinInject<FileFilterState>()
        val updateKey by fileFilterState.updateKey.collectAsState()

        if (fileState.checkedFileSimpleInfo.isNotEmpty()) {
            // 用户选择了分享文件，但是没有粘贴文件。
            if (deskType is Local && fileState.checkedFileSimpleInfo.find { it.protocol == FileProtocol.Share } != null) {
                BottomAppBar(
                    actions = {
                        IconButton({
                            if (isPasteCopyFile) fileState.cancelCopyFile()
                            if (isPasteMoveFile) fileState.cancelMoveFile()
                            fileState.checkedFileSimpleInfo.clear()
                        }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    fileState.pasteCopyFile(
                                        Task(
                                            taskType = TaskType.Move,
                                            status = StatusEnum.LOADING,
                                            values = mutableMapOf("path" to path)
                                        ), path, fileOperationState
                                    )
                                }
                            },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(Icons.Filled.ContentPaste, null)
                        }
                    }
                )
                return
            }

            val files = fileFilterState.filter(fileState.fileAndFolder, updateKey)
            val isCheckedAll =
                fileState.checkedFileSimpleInfo.size == files.count {
                    fileState.checkedFileSimpleInfo.contains(it)
                } && fileState.checkedFileSimpleInfo.size == files.size
            // 用户选择了分享文件
            if (isShareType) {
                BottomAppBar(
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .toggleable(
                                    value = isCheckedAll,
                                    onValueChange = {
                                        fileState.checkedFileSimpleInfo.clear()
                                        if (!isCheckedAll) {
                                            fileState.checkedFileSimpleInfo.addAll(files)
                                        }
                                    },
                                    role = Role.Checkbox,
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Checkbox(isCheckedAll, onCheckedChange = null)
                        }

                        IconButton({
                            if (isPasteCopyFile) fileState.cancelCopyFile()
                            if (isPasteMoveFile) fileState.cancelMoveFile()
                            fileState.checkedFileSimpleInfo.clear()
                        }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    },
                )
                return
            }


            BottomAppBar(
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .toggleable(
                                value = isCheckedAll,
                                onValueChange = {
                                    if (isPasteCopyFile) fileState.cancelCopyFile()
                                    if (isPasteMoveFile) fileState.cancelMoveFile()
                                    fileState.checkedFileSimpleInfo.clear()
                                    if (!isCheckedAll) {
                                        fileState.checkedFileSimpleInfo.addAll(files)
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
                            scope.launch(Dispatchers.Default) {
                                withContext(Dispatchers.Default) {
                                    if (isPasteCopyFile) {
                                        fileState.pasteCopyFile(
                                            Task(
                                                taskType = TaskType.Move,
                                                status = StatusEnum.LOADING,
                                                values = mutableMapOf("path" to path)
                                            ), path, fileOperationState
                                        )
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
                        fileState.checkedFileSimpleInfo.clear()
                    }) {
                        Icon(Icons.Filled.Close, null)
                    }

                    Spacer(Modifier.weight(1f))

                    FileBottomAppMenu(
                        onRemove = { paths ->
                            scope.launch(Dispatchers.Default) {
                                when (snackbarHostState.showSnackbar(
                                    message = "确认要删除选择文件或文件夹吗？",
                                    actionLabel = "删除",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )) {
                                    SnackbarResult.Dismissed -> {}
                                    SnackbarResult.ActionPerformed -> {
                                        scope.launch(Dispatchers.Default) {
                                            for (it in paths) {
                                                fileState.deleteFile(
                                                    Task(
                                                        taskType = TaskType.Delete,
                                                        status = StatusEnum.LOADING,
                                                        values = mutableMapOf("path" to it)
                                                    ),
                                                    it
                                                )
                                            }
                                            fileState.updateFileAndFolder()
                                            fileFilterState.updateFilerKey()
                                        }
                                    }
                                }
                            }
                        }
                    )

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

@Composable
fun SearchDialog(
    searchText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var currentSearchText by remember(searchText) { mutableStateOf(searchText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("搜索文件")
        },
        text = {
            OutlinedTextField(
                value = currentSearchText,
                onValueChange = { currentSearchText = it },
                label = { Text("输入搜索关键词") },
                placeholder = { Text("文件名或扩展名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentSearchText) }
            ) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
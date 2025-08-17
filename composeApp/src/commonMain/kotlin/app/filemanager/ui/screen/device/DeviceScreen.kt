package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType
import app.filemanager.data.main.Local
import app.filemanager.data.main.Share
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.DeviceIcon
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.device.DeviceCertificateState
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import app.filemanager.db.Device as DbDevice

/**
 * 设备管理屏幕
 * 提供设备列表的显示、搜索、编辑和删除功能
 * 
 * 主要功能：
 * - 设备列表展示：根据设备类型筛选显示设备
 * - 搜索功能：支持按设备名称搜索
 * - 设备编辑：修改设备名称
 * - 设备删除：删除设备及相关连接
 * - 类型筛选：支持按设备类型（PC、iOS、Android、浏览器）筛选
 */
class DeviceScreen : Screen {
    /**
     * 这是一个用于显示和管理设备列表的可组合函数。它包括设备的展示、搜索功能、
     * 设备编辑功能以及过滤设备类型等多种交互功能。
     *
     * 功能包括：
     * - 显示设备列表：通过从数据库查询设备信息展示设备列表。
     * - 搜索功能：能够根据用户输入的搜索查询更新设备列表。
     * - 过滤功能：按设备类型筛选设备列表（例如 PC、IOS、安卓、浏览器等）。
     * - 编辑设备：可以更新设备的名称，包括同步名称更新到相关的设备共享和桌面状态。
     * - 导航：提供返回按钮以返回到上一屏幕。
     *
     * 本方法使用了 Material3 设计的组件，包括 TopAppBar、Scaffold 等。
     *
     * 内部使用：
     * - `LaunchedEffect`：初始化时加载设备列表。
     * - 状态变量：使用 `remember` 和 `mutableStateOf` 来管理本地状态，如显示对话框、查询条件等。
     * - 依赖注入：通过 `koinInject` 注入必要的状态和数据库实例。
     *
     * 注意：
     * - 该方法包含了对设备进行编辑时的相关逻辑，并确保设备相关数据之间的同步。
     * - 在筛选*/
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mainState = koinInject<MainState>()
        val deviceState = koinInject<DeviceState>()
        val fileState = koinInject<FileState>()
        val database = koinInject<FileManagerDatabase>()
        val deviceCertificateState = koinInject<DeviceCertificateState>()

        val deskType by fileState.deskType.collectAsState()

        // 搜索相关状态
        var showSearchDialog by remember { mutableStateOf(false) } // 是否显示搜索对话框
        var searchQuery by remember { mutableStateOf("") } // 搜索查询字符串

        // 设备列表和编辑相关状态
        var devices by remember { mutableStateOf(emptyList<DbDevice>()) } // 当前显示的设备列表
        var showEditDialog by remember { mutableStateOf(false) } // 是否显示编辑对话框
        var showDeleteDialog by remember { mutableStateOf(false) } // 是否显示删除确认对话框
        var selectedDevice by remember { mutableStateOf<DbDevice?>(null) } // 当前选中的设备
        var editedName by remember { mutableStateOf("") } // 编辑中的设备名称
        
        // 设备类型筛选选项
        val deviceTypes = listOf(
            null to "全部",
            DeviceType.JVM to "PC",
            DeviceType.IOS to "IOS",
            DeviceType.Android to "安卓",
            DeviceType.JS to "浏览器",
        )
        var deviceType by remember { mutableStateOf<DeviceType?>(null) } // 当前选择的设备类型筛选

        val scope = rememberCoroutineScope()

        /**
         * 刷新设备列表
         * 根据当前选择的设备类型和搜索查询条件从数据库查询设备列表
         */
        fun refreshDevices() {
            val typesToQuery = if (deviceType == null) {
                listOf(DeviceType.JVM, DeviceType.IOS, DeviceType.Android, DeviceType.JS)
            } else {
                listOf(deviceType!!)
            }

            devices = database.deviceQueries.queryByNameAndDeviceTypePaginated(
                searchQuery,
                typesToQuery,
                100L,
                0L
            ).executeAsList()
        }

        /**
         * 删除设备的处理逻辑
         * 从数据库、内存列表和远程连接中完全移除设备
         * 如果当前桌面显示的是该设备，则切换到本地桌面
         */
        fun deleteDevice() {
            selectedDevice?.let { device ->
                val deviceId = device.id

                // 从数据库删除设备
                database.deviceQueries.deleteById(deviceId)
                database.deviceConnectQueries.deleteById(deviceId)

                // 从socketDevices列表移除
                val deviceToRemove = deviceState.socketDevices.find { it.id == deviceId }
                deviceToRemove?.let { deviceState.socketDevices.remove(it) }
                scope.launch {
                    deviceState.remoteDeviceConnections[deviceId]?.close()
                    deviceState.remoteDeviceConnections.remove(deviceId)
                }

                // 从devices列表移除
                deviceState.devices.removeAll { it.id == deviceId }

                // TODO 删除数据
                // 从shares列表移除
                deviceState.shares.removeAll { it.id == deviceId }

                deviceCertificateState.removeDeviceToken(device.id)

                // 如果当前桌面显示的是该设备，切换到本地
                if (deskType is Device && (deskType as Device).id == device.id) {
                    fileState.updateDesk(FileProtocol.Local, Local())
                }
                if (deskType is Share && (deskType as Share).id == device.id) {
                    fileState.updateDesk(FileProtocol.Local, Local())
                }

                refreshDevices()
                showDeleteDialog = false
                selectedDevice = null
            }
        }

        LaunchedEffect(Unit) {
            refreshDevices()
        }

        // 删除确认对话框
        if (showDeleteDialog && selectedDevice != null) {
            DeleteDeviceDialog(
                selectedDevice = selectedDevice!!,
                onConfirm = { deleteDevice() },
                onDismiss = { showDeleteDialog = false }
            )
        }

        // 编辑设备名称对话框
        if (showEditDialog && selectedDevice != null) {
            DeviceEditDialog(
                selectedDevice = selectedDevice!!,
                editedName = editedName,
                onEditedNameChange = { editedName = it },
                onDismiss = { showEditDialog = false },
                onConfirm = { device ->
                    // 更新数据库中设备的名称和备注启用状态
                    database.deviceQueries.updateNameAndEnableRemarksById(
                        name = editedName,
                        id = device.id
                    )

                    // 更新socketDevices列表中对应设备的名称（如果存在）
                    val indexSocket = deviceState.socketDevices.indexOfFirst { it.id == device.id }
                    if (indexSocket != -1) {
                        // 使用copy方法创建新实例以保持不可变性，同时更新名称
                        deviceState.socketDevices[indexSocket] =
                            deviceState.socketDevices[indexSocket].copy(name = editedName)
                    }

                    // 更新devices列表中对应设备的名称（如果存在）
                    val indexDevice = deviceState.devices.indexOfFirst { it.id == device.id }
                    if (indexDevice != -1) {
                        deviceState.devices[indexDevice] = deviceState.devices[indexDevice].copy(name = editedName)
                    }

                    // 检查当前桌面类型是否为Device类型，如果是且ID匹配，则更新桌面显示
                    if (deskType is Device) {
                        val currentDevice = deskType as Device
                        if (currentDevice.id == device.id) {
                            // 通过FileState更新桌面显示，传递新的设备实例（带有更新后的名称）
                            fileState.updateDesk(
                                FileProtocol.Device, currentDevice.copy(name = editedName)
                            )
                        }
                    }

                    // 更新shares列表中对应设备的名称（如果存在）
                    val indexShare = deviceState.shares.indexOfFirst { it.id == device.id }
                    if (indexShare != -1) {
                        deviceState.shares[indexShare] = deviceState.shares[indexShare].copy(name = editedName)
                    }

                    // 检查当前桌面类型是否为Share类型，如果是且ID匹配，则更新桌面显示
                    if (deskType is Share) {
                        val currentShare = deskType as Share
                        if (currentShare.id == device.id) {
                            // 通过FileState更新桌面显示，传递新的共享实例（带有更新后的名称）
                            fileState.updateDesk(
                                FileProtocol.Share, currentShare.copy(name = editedName)
                            )
                        }
                    }

                    refreshDevices()
                    showEditDialog = false
                }
            )
        }

        // 在Content函数中添加对话框显示逻辑
        if (showSearchDialog) {
            SearchDialog(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onDismiss = { showSearchDialog = false },
                onSearch = {
                    refreshDevices()
                    showSearchDialog = false
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备") },
                    navigationIcon = {
                        IconButton(
                            navigator::pop
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconToggleButton(
                            checked = searchQuery.isNotBlank(),
                            onCheckedChange = {
                                showSearchDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "搜索设备")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    deviceTypes.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = deviceType == type,
                            onClick = {
                                deviceType = type
                                refreshDevices()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = deviceTypes.size),
                        ) { Text(label) }
                    }
                }

                GridList(
                    exception = if (devices.isEmpty()) EmptyDataException() else null
                ) {
                    items(devices) { device ->
                        DeviceListItem(
                            device = device,
                            onEditClick = {
                                selectedDevice = device
                                editedName = device.name
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                selectedDevice = device
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * 搜索对话框组件
     * 提供设备搜索功能，允许用户输入设备名称进行搜索
     * 
     * @param searchQuery 当前搜索查询字符串
     * @param onSearchQueryChange 搜索查询变更回调
     * @param onDismiss 关闭对话框回调
     * @param onSearch 执行搜索回调
     */
    @Composable
    private fun SearchDialog(
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSearch: () -> Unit
    ) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            onSearchQueryChange(searchQuery) // 这会触发光标移动到末尾
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("搜索设备") },
            text = {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("输入设备名称") },
                    singleLine = true,
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch() }
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清空"
                                )
                            }
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onSearch) {
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

    /**
     * 显示一个设备列表项，展示设备信息及编辑按钮。
     *
     * @param device 要显示的设备实例，包含设备的名称、类型、主机等信息。
     * @param onEditClick 当用户点击编辑按钮时调用的回调函数。
     */
    @Composable
    private fun DeviceListItem(
        device: DbDevice,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = {
                if (device.host != null) {
                    Text(device.host)
                }
            },
            leadingContent = {
                device.type.DeviceIcon()
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑设备名称")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "删除设备")
                    }
                }
            },
            modifier = Modifier.clickable(onClick = onEditClick)
        )
    }

    /**
     * 显示一个设备编辑对话框，允许用户编辑设备的名称。
     *
     * @param selectedDevice 当前选中的设备实例。
     * @param editedName 当前被编辑的设备名称。
     * @param onEditedNameChange 当设备名称发生修改时的回调。
     * @param onDismiss 当对话框被关闭时的回调。
     * @param onConfirm 当用户确认修改时的回调，传递修改后的设备实例。
     */
    @Composable
    private fun DeviceEditDialog(
        selectedDevice: DbDevice,
        editedName: String,
        onEditedNameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onConfirm: (DbDevice) -> Unit
    ) {
        var isNameError by remember { mutableStateOf(editedName.isBlank()) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("编辑设备名称") },
            text = {
                TextField(
                    value = editedName,
                    onValueChange = {
                        onEditedNameChange(it)
                        isNameError = it.isBlank()
                    },
                    label = { Text("设备名称") },
                    singleLine = true,
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text("设备名称不能为空")
                        }
                    },
                    trailingIcon = {
                        if (editedName.isNotEmpty()) {
                            IconButton(onClick = { onEditedNameChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清空"
                                )
                            }
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedName.isNotEmpty() && selectedDevice.name != editedName,
                    onClick = { onConfirm(selectedDevice) }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }

    /**
     * 删除设备确认对话框
     * 显示删除确认对话框，确保用户确认删除操作
     * 
     * @param selectedDevice 要删除的设备
     * @param onConfirm 确认删除回调
     * @param onDismiss 取消删除回调
     */
    @Composable
    private fun DeleteDeviceDialog(
        selectedDevice: DbDevice,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认删除") },
            text = { Text("确定要删除设备 \"${selectedDevice.name}\" 吗？") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

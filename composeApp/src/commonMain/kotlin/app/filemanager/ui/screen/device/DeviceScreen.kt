package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.filemanager.data.main.Share
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import app.filemanager.db.Device as DbDevice

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

        val deskType by fileState.deskType.collectAsState()

        // 在DeviceScreen类中添加状态变量
        var showSearchDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }


        var devices by remember { mutableStateOf(emptyList<DbDevice>()) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<DbDevice?>(null) }
        var editedName by remember { mutableStateOf("") }
        val deviceTypes = listOf(
            null to "全部",
            DeviceType.JVM to "PC",
            DeviceType.IOS to "IOS",
            DeviceType.Android to "安卓",
            DeviceType.JS to "浏览器",
        )
        var deviceType by remember { mutableStateOf<DeviceType?>(null) }

        // 刷新设备列表
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

        LaunchedEffect(Unit) {
            refreshDevices()
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
                            }
                        )
                    }
                }
            }
        }
    }

    // 添加搜索对话框组件
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
        onEditClick: () -> Unit
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            supportingContent = {
                if (device.host != null) {
                    Text(device.host)
                }
            },
            leadingContent = {
                when (device.type) {
                    DeviceType.Android -> Icon(Icons.Default.PhoneAndroid, null)
                    DeviceType.IOS -> Icon(Icons.Default.PhoneIphone, null)
                    DeviceType.JVM -> Icon(Icons.Default.Devices, null)
                    DeviceType.JS -> Icon(Icons.Default.Javascript, null)
                }
            },
            trailingContent = {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑设备名称")
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
}

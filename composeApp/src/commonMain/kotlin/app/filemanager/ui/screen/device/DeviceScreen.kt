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
import app.filemanager.data.main.DeviceType
import app.filemanager.db.Device
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class DeviceScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mainState = koinInject<MainState>()
        val database = koinInject<FileManagerDatabase>()

        // 在DeviceScreen类中添加状态变量
        var showSearchDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }


        var devices by remember { mutableStateOf(emptyList<Device>()) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<Device?>(null) }
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
                    database.deviceQueries.updateNameAndEnableRemarksById(
                        name = editedName,
                        id = device.id
                    )
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
                    title = { Text("设备列表") },
                    navigationIcon = {
                        IconButton({
                            mainState.updateScreen(null)
                            navigator.pop()
                        }) {
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
        device: Device,
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
        selectedDevice: Device,
        editedName: String,
        onEditedNameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onConfirm: (Device) -> Unit
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
                    enabled = editedName.isNotEmpty(),
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

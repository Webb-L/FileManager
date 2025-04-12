package app.filemanager.ui.screen.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.filemanager.data.main.DeviceType
import app.filemanager.db.Device
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class DeviceScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val database = koinInject<FileManagerDatabase>()

        var devices by remember { mutableStateOf(emptyList<Device>()) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDevice by remember { mutableStateOf<Device?>(null) }
        var editedName by remember { mutableStateOf("") }

        // 刷新设备列表
        fun refreshDevices() {
            devices = database.deviceQueries.queryAll().executeAsList()
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备列表") },
                    navigationIcon = {
                        IconButton({
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    }
                )
            }
        ) { padding ->
            GridList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                Button(
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
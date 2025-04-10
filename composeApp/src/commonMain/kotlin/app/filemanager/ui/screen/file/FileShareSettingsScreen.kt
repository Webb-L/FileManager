package app.filemanager.ui.screen.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceType
import app.filemanager.db.DeviceReceiveShare
import app.filemanager.db.FileManagerDatabase
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

class FileShareSettings() : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        // 获取导航器实例
        val navigator = LocalNavigator.currentOrThrow

        // 注入数据库实例
        val database = koinInject<FileManagerDatabase>()

        // 设备列表状态
        var devices by remember { mutableStateOf(emptyList<DeviceReceiveShare>()) }
        // 是否显示编辑对话框
        var showEditDialog by remember { mutableStateOf(false) }
        // 当前选择编辑的设备
        var selectedDevice by remember { mutableStateOf<DeviceReceiveShare?>(null) }

        // 加载所有设备数据
        LaunchedEffect(Unit) {
            devices = database.deviceReceiveShareQueries.selectAll().executeAsList()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备分享管理") },
                    navigationIcon = {
                        IconButton({
                            navigator.pop() // 返回上一级
                        }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton({
                            navigator.push(FileShareSettings()) // 打开历史记录界面
                        }) {
                            Icon(Icons.Default.History, null)
                        }
                    }
                )
            }
        ) { padding ->
            // 列表展示所有设备
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(devices) { device ->
                    DeviceListItem(device) { // 点击某个设备，显示编辑对话框
                        selectedDevice = device
                        showEditDialog = true
                    }
                }
            }

            // 显示编辑对话框
            if (showEditDialog && selectedDevice != null) {
                EditDeviceDialog(
                    device = selectedDevice!!,
                    onDismiss = { showEditDialog = false }, // 关闭对话框
                    onConfirm = { updatedDevice ->
                        devices = database.deviceReceiveShareQueries.selectAll().executeAsList() // 更新设备列表
                        showEditDialog = false
                    },
                    database = database
                )
            }
        }
    }

    // 设备列表中的单条设备项
    @Composable
    private fun DeviceListItem(
        device: DeviceReceiveShare,
        onClick: () -> Unit = {}
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick), // 点击事件
            overlineContent = { // 上方小标签
                when (device.connectionType) {
                    DeviceConnectType.AUTO_CONNECT -> Badge(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) { Text("自动连接") } // 自动连接标签

                    DeviceConnectType.PERMANENTLY_BANNED -> Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) { Text("自动拒绝") } // 自动拒绝标签

                    else -> {}
                }
            },
            headlineContent = { Text(device.name) }, // 设备名称
            supportingContent = {
                if (device.path.isNotEmpty()) {
                    Text(device.path) // 显示路径信息
                }
            },
            leadingContent = { // 设备图标
                when (device.type) {
                    DeviceType.Android -> Icon(Icons.Default.PhoneAndroid, null)
                    DeviceType.IOS -> Icon(Icons.Default.PhoneIphone, null)
                    DeviceType.JVM -> Icon(Icons.Default.Devices, null)
                    DeviceType.JS -> Icon(Icons.Default.Javascript, null)
                }
            }
        )
    }

    // 编辑设备信息的对话框
    @Composable
    fun EditDeviceDialog(
        device: DeviceReceiveShare,
        onDismiss: () -> Unit,
        onConfirm: (DeviceReceiveShare) -> Unit,
        database: FileManagerDatabase
    ) {
        // 记录编辑后的设备信息
        var editedDevice by remember { mutableStateOf(device) }

        AlertDialog(
            onDismissRequest = onDismiss, // 对话框关闭事件
            title = { Text("编辑设备设置") },
            text = {
                Column {
                    Text("连接类型:", style = MaterialTheme.typography.labelLarge)
                    // 选择连接类型的按钮组
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = editedDevice.connectionType == DeviceConnectType.AUTO_CONNECT,
                            onClick = {
                                editedDevice = editedDevice.copy(connectionType = DeviceConnectType.AUTO_CONNECT)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("自动同意") }

                        SegmentedButton(
                            selected = editedDevice.connectionType == DeviceConnectType.PERMANENTLY_BANNED,
                            onClick = {
                                editedDevice = editedDevice.copy(connectionType = DeviceConnectType.PERMANENTLY_BANNED)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("自动拒绝") }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 编辑路径的文本框
                    TextField(
                        value = editedDevice.path,
                        onValueChange = { editedDevice = editedDevice.copy(path = it) },
                        label = { Text("路径") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 更新设备的连接类型和路径信息
                        database.deviceReceiveShareQueries.updateConnectionTypeAndPathById(
                            connectionType = editedDevice.connectionType,
                            path = editedDevice.path,
                            id = editedDevice.id
                        )
                        onConfirm(editedDevice) // 确认操作
                    }
                ) {
                    Text("确认") // 确认按钮
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss // 取消操作
                ) {
                    Text("取消") // 取消按钮
                }
            }
        )
    }
}
package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileFilterType
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.CONNECT_TIMEOUT
import app.filemanager.ui.state.device.DeviceRoleState
import app.filemanager.ui.state.file.FileShareStatus
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.PathUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.koin.compose.koinInject


@Composable
fun MaterialBannerDeviceShare(socketDevice: SocketDevice) {
    val database = koinInject<FileManagerDatabase>()
    val deviceState = koinInject<DeviceState>()

    var expanded by remember { mutableStateOf(false) }

    val remainingConnectionTime =
        (CONNECT_TIMEOUT + ((deviceState.shareRequest[socketDevice.id]!!.second - System.currentTimeMillis()) / 1000L)).toInt()
    var timeRemaining by remember { mutableStateOf(remainingConnectionTime) } // 10 minutes in seconds


    LaunchedEffect(Unit) {
        while (timeRemaining > 0 && deviceState.shareRequest[socketDevice.id] != null) {
            delay(1000)
            timeRemaining =
                (CONNECT_TIMEOUT + ((deviceState.shareRequest[socketDevice.id]!!.second - System.currentTimeMillis()) / 1000L)).toInt()
        }
        // 等待时间结束
        if (timeRemaining <= 0) {
            deviceState.shareRequest[socketDevice.id] =
                Pair(REJECTED, System.currentTimeMillis())
            deviceState.shareConnectionStates[socketDevice.id] = FileShareStatus.REJECTED
        }
    }

    val timeText = remember(timeRemaining) {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        if (minutes == 0) "${seconds}秒" else "${minutes}分${seconds}秒"
    }

    var isSelectPathDialog by remember { mutableStateOf(false) }

    fun updateShareRequest(deviceType: DeviceConnectType, path: String? = null) {
        when (deviceType) {
            AUTO_CONNECT, APPROVED -> {
                deviceState.connectShare(socketDevice)
                deviceState.shareConnectionStates[socketDevice.id] = FileShareStatus.COMPLETED
                if (deviceType == AUTO_CONNECT) {
                    database.deviceReceiveShareQueries.insert(
                        socketDevice.id,
                        AUTO_CONNECT,
                        path ?: PathUtils.getHomePath()
                    )
                }
            }

            else -> {
                if (deviceType == PERMANENTLY_BANNED) {
                    database.deviceReceiveShareQueries.insert(
                        socketDevice.id,
                        PERMANENTLY_BANNED,
                        ""
                    )
                }
                deviceState.shareConnectionStates[socketDevice.id] = FileShareStatus.REJECTED
            }
        }
        deviceState.shareRequest.remove(socketDevice.id)
        expanded = false
    }

    MaterialBanner(
        message = "${socketDevice.name} 请求向您发送文件或文件夹\n${timeText} 后将会自动拒绝。",
        actionLabel = "同意",
        onActionClick = { updateShareRequest(APPROVED) },
        menu = {
            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                IconButton({ expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "菜单",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("自动同意") },
                        onClick = {
                            isSelectPathDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("自动拒绝") },
                        onClick = { updateShareRequest(PERMANENTLY_BANNED) }
                    )
                    DropdownMenuItem(
                        text = { Text("拒绝") },
                        onClick = { updateShareRequest(REJECTED) }
                    )
                }
            }
        }
    )



    if (isSelectPathDialog) {
        var fileSimpleInfo by remember { mutableStateOf<FileSimpleInfo?>(null) }

        AlertDialog(
            title = { Text("请选择要保存的目录") },
            text = {
                FileSelector(
                    PathUtils.getHomePath(),
                    isSingleSelection = true,
                    fileFilterType = FileFilterType.Folder,
                    onFilesSelected = {
                        fileSimpleInfo = null
                        if (it.isNotEmpty()) {
                            fileSimpleInfo = it.first()
                        }
                    }
                )
            },
            onDismissRequest = {},
            confirmButton = {
                TextButton({
                    updateShareRequest(AUTO_CONNECT, fileSimpleInfo?.path)
                }, enabled = fileSimpleInfo != null) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton({ isSelectPathDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 显示一个 Material 设计风格的横幅，用于处理设备的连接请求。
 *
 * @param socketDevice 表示请求连接的设备实例，包含设备的详细信息。
 */
@Composable
fun MaterialBannerDeviceConnect(socketDevice: SocketDevice) {
    val database = koinInject<FileManagerDatabase>()
    val deviceState = koinInject<DeviceState>()
    val roleState = koinInject<DeviceRoleState>()
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var pendingConnectionType by remember { mutableStateOf(APPROVED) }

    val remainingConnectionTime =
        (CONNECT_TIMEOUT + ((deviceState.connectionRequest[socketDevice.id]!!.second - System.currentTimeMillis()) / 1000L)).toInt()
    var timeRemaining by remember { mutableStateOf(remainingConnectionTime) } // 10 minutes in seconds


    LaunchedEffect(Unit) {
        while (timeRemaining > 0 && deviceState.connectionRequest[socketDevice.id] != null) {
            delay(1000)
            timeRemaining =
                (CONNECT_TIMEOUT + ((deviceState.connectionRequest[socketDevice.id]!!.second - System.currentTimeMillis()) / 1000L)).toInt()
        }
        // Automatically reject after the countdown ends
        if (timeRemaining <= 0) {
            deviceState.connectionRequest[socketDevice.id] =
                Pair(REJECTED, System.currentTimeMillis())
        }
    }

    val timeText = remember(timeRemaining) {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        if (minutes == 0) "${seconds}秒" else "${minutes}分${seconds}秒"
    }

    fun handleConnectionRequest(connectionType: DeviceConnectType) {
        scope.launch {
            // 检查设备是否有设置roleId
            val deviceConnect = database.deviceConnectQueries.queryByIdAndCategory(
                socketDevice.id,
                DeviceCategory.SERVER
            ).executeAsOneOrNull()

            if (deviceConnect == null || deviceConnect.roleId == -1L) {
                // 设备首次连接或未设置角色，需要选择角色
                pendingConnectionType = connectionType
                showRoleDialog = true
            } else {
                // roleId已设置，直接执行连接
                deviceState.connectionRequest[socketDevice.id] =
                    Pair(connectionType, deviceState.connectionRequest[socketDevice.id]!!.second)
                if (connectionType == AUTO_CONNECT) {
                    expanded = false
                }
            }
        }
    }

    fun handleAgree() = handleConnectionRequest(APPROVED)
    fun handleAutoConnect() = handleConnectionRequest(AUTO_CONNECT)

    MaterialBanner(
        message = "${socketDevice.name} 请求和您创建连接。\n${timeText} 后将会自动拒绝。",
        actionLabel = "同意",
        onActionClick = { handleAgree() },
        menu = {
            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                IconButton({ expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "菜单",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("自动同意") },
                        onClick = { handleAutoConnect() }
                    )
                    DropdownMenuItem(
                        text = { Text("自动拒绝") },
                        onClick = {
                            deviceState.connectionRequest[socketDevice.id] = Pair(
                                PERMANENTLY_BANNED,
                                deviceState.connectionRequest[socketDevice.id]!!.second
                            )
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("拒绝") },
                        onClick = {
                            deviceState.connectionRequest[socketDevice.id] = Pair(
                                REJECTED,
                                deviceState.connectionRequest[socketDevice.id]!!.second
                            )
                            expanded = false
                        }
                    )
                }
            }
        }
    )

    // 角色选择弹窗
    if (showRoleDialog) {
        var selectedRole by remember { mutableStateOf("") }

        AlertDialog(
            title = { Text("选择设备角色") },
            text = {
                Column {
                    Text("请为设备 \"${socketDevice.name}\" 选择一个角色：")
                    Spacer(modifier = Modifier.height(16.dp))

                    roleState.roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role.name },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role.name,
                                onClick = { selectedRole = role.name }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role.name)
                        }
                    }
                }
            },
            onDismissRequest = {
                showRoleDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // 找到选中的角色ID
                            val selectedRoleId = roleState.roles.find { it.name == selectedRole }?.id ?: -1

                            // 更新设备连接记录的角色ID
                            database.deviceConnectQueries.updateNameConnectTypeRoleIdByIdAndCategory(
                                pendingConnectionType,
                                selectedRoleId,
                                socketDevice.id,
                                DeviceCategory.SERVER
                            )

                            // 执行相应的连接操作
                            deviceState.connectionRequest[socketDevice.id] =
                                Pair(pendingConnectionType, deviceState.connectionRequest[socketDevice.id]!!.second)

                            showRoleDialog = false
                        }
                    },
                    enabled = selectedRole.isNotEmpty()
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton({
                    deviceState.connectionRequest[socketDevice.id] = Pair(
                        REJECTED,
                        deviceState.connectionRequest[socketDevice.id]!!.second
                    )
                    showRoleDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MaterialBanner(
    icon: @Composable (() -> Unit)? = null,
    menu: @Composable (() -> Unit)? = null,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()

                Spacer(modifier = Modifier.width(8.dp))
            }

            // 中间文本
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 操作按钮
            TextButton(onClick = onActionClick) {
                Text(text = actionLabel)
            }

            if (menu != null) {
                menu()
            }

            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Banner"
                    )
                }
            }
        }
    }
}
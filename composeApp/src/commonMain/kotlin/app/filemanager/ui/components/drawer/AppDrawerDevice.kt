package app.filemanager.ui.components.drawer

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.extensions.DeviceIcon
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.ConnectType.*
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.components.IpsButton
import app.filemanager.ui.screen.device.DeviceSettingsScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// 通用的设备状态更新函数
private fun updateDeviceStateAndConnect(
    deviceState: DeviceState,
    scope: CoroutineScope,
    device: SocketDevice,
    connectType: ConnectType,
    shouldConnect: Boolean = false
) {
    // 通用的设备状态更新逻辑
    fun updateDeviceConnectType(deviceId: String, newConnectType: ConnectType) {
        val updatedDevices = deviceState.socketDevices.map {
            if (it.id == deviceId) it.withCopy(connectType = newConnectType) else it
        }
        deviceState.socketDevices.clear()
        deviceState.socketDevices.addAll(updatedDevices)
    }

    updateDeviceConnectType(device.id, connectType)

    if (shouldConnect) {
        scope.launch {
            try {
                deviceState.connect(device)
            } catch (e: Exception) {
                updateDeviceConnectType(device.id, Fail)
            }
        }
    }
}

@Composable
fun AppDrawerDevice() {
    val mainState = koinInject<MainState>()
    val fileState = koinInject<FileState>()

    val drawerState = koinInject<DrawerState>()
    val isExpandDevice by drawerState.isExpandDevice.collectAsState()

    val deviceState = koinInject<DeviceState>()
    val loadingDevices by deviceState.loadingDevices.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (loadingDevices) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val scope = rememberCoroutineScope()

    var socketDevice by remember {
        mutableStateOf<SocketDevice?>(null)
    }

    AppDrawerItem(
        "设备",
        actions = {
            Row {
                Icon(
                    Icons.Default.Add,
                    null,
                    Modifier.clip(RoundedCornerShape(25.dp)).clickable {
                        deviceState.updateDeviceAdd(true)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Sync,
                    null,
                    Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                        .alpha(if (loadingDevices) 0.5f else 1f)
                        .clickable {
                            if (loadingDevices) return@clickable
                            scope.launch {
                                deviceState.scanner(getAllIPAddresses(type = SocketClientIPEnum.IPV4_UP))
                            }
                        }
                )
                Spacer(Modifier.width(8.dp))
                IpsButton()
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Settings,
                    null,
                    Modifier.clip(RoundedCornerShape(25.dp))
                        .clickable {
                            if (mainState.windowSize == WindowSizeClass.Compact) {
                                mainState.updateExpandDrawer(false)
                            }
                            mainState.navigator?.push(DeviceSettingsScreen())
                        }
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpandDevice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    Modifier.clip(RoundedCornerShape(25.dp))
                        .clickable { drawerState.updateExpandDevice(!isExpandDevice) }
                )
            }
        }
    ) {
        if (!isExpandDevice) return@AppDrawerItem
        for ((index, device) in deviceState.socketDevices.sortedByDescending { it.httpClient != null }.withIndex()) {
            NavigationDrawerItem(
                icon = {
                    if (device.connectType == Loading) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        return@NavigationDrawerItem
                    }

                    device.type.DeviceIcon()
                },
                badge = {
                    when (device.connectType) {
                        Connect -> Icon(
                            Icons.Default.Close,
                            "取消连接",
                            Modifier
                                .clip(RoundedCornerShape(25.dp))
                                .clickable {
                                    if (device.httpClient?.disconnect() == true) {
                                        // 安全地更新设备状态，避免索引越界
                                        updateDeviceStateAndConnect(deviceState, scope, device, UnConnect)
                                        deviceState.devices.remove(deviceState.devices.firstOrNull { it.id == device.id })
                                    }
                                }
                        )

                        Fail -> Badge { Text("连接失败") }
                        UnConnect -> Badge { Text("未连接") }
                        Loading -> Badge(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) { Text("连接中") }

                        New -> Badge { Text("新-未连接") }
                        Rejected -> Badge { Text("拒绝连接") }
                    }
                },
                label = { Text(device.name) },
                selected = false,
                onClick = {
                    when (device.connectType) {
                        UnConnect, Fail, Rejected -> {
                            // 连接设备并更新状态
                            updateDeviceStateAndConnect(deviceState, scope, device, Loading, shouldConnect = true)
                        }

                        New -> {
                            socketDevice = device
                        }

                        Connect -> {
                            deviceState.devices.firstOrNull { it.id == device.id }?.let {
                                fileState.updateDesk(FileProtocol.Device, it)
                            }
                        }

                        Loading -> {
                            // 取消连接中的状态
                            device.httpClient?.disconnect()
                            updateDeviceStateAndConnect(deviceState, scope, device, UnConnect)
                        }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    if (socketDevice != null) {
        DeviceConnectNewDialog(
            socketDevice!!,
            onCancel = { socketDevice = null },
        )
    }
}

@Composable
fun DeviceConnectNewDialog(
    socketDevice: SocketDevice,
    onCancel: () -> Unit
) {
    val database = koinInject<app.filemanager.db.FileManagerDatabase>()
    val deviceState = koinInject<DeviceState>()
    val scope = rememberCoroutineScope()

    val onConnect: (Boolean) -> Unit = { isAuto ->
        database.deviceConnectQueries.insert(
            id = socketDevice.id,
            connectionType = if (isAuto) app.filemanager.data.main.DeviceConnectType.AUTO_CONNECT else app.filemanager.data.main.DeviceConnectType.WAITING,
            category = app.filemanager.data.main.DeviceCategory.CLIENT,
            -1
        )
        // 连接设备并更新状态
        updateDeviceStateAndConnect(deviceState, scope, socketDevice, Loading, shouldConnect = true)
        onCancel()
    }

    AlertDialog(
        onDismissRequest = { onCancel() },
        title = {
            Text(text = "连接新设备")
        },
        text = {
            Text(text = "发现新设备「${socketDevice.name}」，是否要进行连接？")
        },
        confirmButton = {
            TextButton(onClick = { onConnect(false) }) {
                Text("连接")
            }
            TextButton(onClick = { onConnect(true) }) {
                Text("自动连接")
            }
        },
        dismissButton = {
            TextButton(onClick = { onCancel() }) {
                Text("取消")
            }
        },
    )
}
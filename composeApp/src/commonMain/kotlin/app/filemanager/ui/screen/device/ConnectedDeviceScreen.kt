package app.filemanager.ui.screen.device

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.filemanager.exception.EmptyDataException
import app.filemanager.extensions.DeviceIcon
import app.filemanager.service.data.SocketDevice
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.main.DeviceState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 正在连接的设备屏幕
 * 显示当前连接到服务器的远程设备列表
 * 
 * 主要功能：
 * - 显示远程连接设备列表：展示通过remoteDeviceConnections连接的设备
 * - 连接状态显示：显示设备的连接状态和信息
 * - 断开连接：允许手动断开特定设备的连接
 * - 实时更新：实时显示连接状态变化
 */
class ConnectedDeviceScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val deviceState = koinInject<DeviceState>()
        val scope = rememberCoroutineScope()
        
        // 连接设备相关状态
        var showDisconnectDialog by remember { mutableStateOf(false) } // 是否显示断开连接确认对话框
        var selectedDevice by remember { mutableStateOf<SocketDevice?>(null) } // 当前选中的设备
        var showDisconnectAllDialog by remember { mutableStateOf(false) } // 是否显示断开所有连接确认对话框
        
        // 获取连接的设备列表
        val connectedDevices by remember {
            derivedStateOf {
                deviceState.socketDevices.filter { device ->
                    deviceState.remoteDeviceConnections.containsKey(device.id)
                }
            }
        }
        
        /**
         * 断开设备连接的处理逻辑
         * 关闭RPC连接并从连接映射中移除设备
         */
        fun disconnectDevice() {
            selectedDevice?.let { device ->
                scope.launch {
                    // 关闭RPC连接
                    deviceState.remoteDeviceConnections[device.id]?.close()
                    // 从连接映射中移除
                    deviceState.remoteDeviceConnections.remove(device.id)
                    
                    // 重置对话框状态
                    showDisconnectDialog = false
                    selectedDevice = null
                }
            }
        }
        
        // 断开连接确认对话框
        if (showDisconnectDialog && selectedDevice != null) {
            DisconnectDeviceDialog(
                selectedDevice = selectedDevice!!,
                onConfirm = { disconnectDevice() },
                onDismiss = { showDisconnectDialog = false }
            )
        }
        
        // 断开所有连接确认对话框
        if (showDisconnectAllDialog) {
            DisconnectAllDevicesDialog(
                onConfirm = {
                    scope.launch {
                        // 关闭所有连接
                        deviceState.remoteDeviceConnections.values.forEach { route ->
                            try {
                                route.close()
                            } catch (_: Exception) {
                                // 忽略关闭异常
                            }
                        }
                        // 清空连接映射
                        deviceState.remoteDeviceConnections.clear()
                    }
                    showDisconnectAllDialog = false
                },
                onDismiss = { showDisconnectAllDialog = false }
            )
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("正在连接的设备") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (connectedDevices.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    showDisconnectAllDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "关闭所有连接")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            // 设备列表
            GridList(
                modifier = Modifier.padding(padding),
                exception = if (connectedDevices.isEmpty()) EmptyDataException() else null
            ) {
                items(connectedDevices) { device ->
                    ConnectedDeviceListItem(
                        device = device,
                        onDisconnectClick = {
                            selectedDevice = device
                            showDisconnectDialog = true
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 连接设备列表项
     * 显示设备图标、名称和断开连接按钮
     * 
     * @param device 连接的设备信息
     * @param onDisconnectClick 断开连接点击回调
     */
    @Composable
    private fun ConnectedDeviceListItem(
        device: SocketDevice,
        onDisconnectClick: () -> Unit
    ) {
        ListItem(
            headlineContent = { Text(device.name) },
            leadingContent = {
                device.type.DeviceIcon()
            },
            trailingContent = {
                IconButton(
                    onClick = onDisconnectClick
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "断开连接"
                    )
                }
            }
        )
    }
    
    /**
     * 断开连接确认对话框
     * 显示断开连接确认对话框，确保用户确认断开操作
     * 
     * @param selectedDevice 要断开连接的设备
     * @param onConfirm 确认断开连接回调
     * @param onDismiss 取消断开连接回调
     */
    @Composable
    private fun DisconnectDeviceDialog(
        selectedDevice: SocketDevice,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认断开连接") },
            text = { Text("确定要断开与设备 \"${selectedDevice.name}\" 的连接吗？") },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                ) {
                    Text("断开连接")
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
     * 断开所有连接确认对话框
     * 显示断开所有连接确认对话框，确保用户确认断开操作
     * 
     * @param onConfirm 确认断开所有连接回调
     * @param onDismiss 取消断开连接回调
     */
    @Composable
    private fun DisconnectAllDevicesDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("确认断开所有连接") },
            text = { Text("确定要断开与所有设备的连接吗？") },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                ) {
                    Text("断开所有连接")
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
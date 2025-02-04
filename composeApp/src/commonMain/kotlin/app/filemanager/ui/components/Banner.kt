package app.filemanager.ui.components

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
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.CONNECT_TIMEOUT
import app.filemanager.ui.state.main.DeviceState
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@Composable
fun MaterialBannerDeviceConnect(socketDevice: SocketDevice) {
    val deviceState = koinInject<DeviceState>()

    var expanded by remember { mutableStateOf(false) }

    val remainingConnectionTime = (CONNECT_TIMEOUT + ((deviceState.connectionRequest[socketDevice.id]!!.second - Clock.System.now()
        .toEpochMilliseconds()) / 1000L)).toInt()
    var timeRemaining by remember { mutableStateOf(remainingConnectionTime) } // 10 minutes in seconds


    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining = (CONNECT_TIMEOUT + ((deviceState.connectionRequest[socketDevice.id]!!.second - Clock.System.now()
                .toEpochMilliseconds()) / 1000L)).toInt()
        }
        // Automatically reject after the countdown ends
        if (timeRemaining <= 0) {
            deviceState.connectionRequest[socketDevice.id] =
                Pair(DeviceConnectType.REJECTED, Clock.System.now().toEpochMilliseconds())
        }
    }

    val timeText = remember(timeRemaining) {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        if (minutes == 0) "${seconds}秒" else "${minutes}分${seconds}秒"
    }

    MaterialBanner(
        message = "${socketDevice.name} 请求和您创建连接。\n${timeText} 后将会自动拒绝。",
        actionLabel = "同意",
        onActionClick = {
            deviceState.connectionRequest[socketDevice.id] =
                Pair(DeviceConnectType.APPROVED, deviceState.connectionRequest[socketDevice.id]!!.second)
        },
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
                            deviceState.connectionRequest[socketDevice.id] = Pair(
                                DeviceConnectType.AUTO_CONNECT,
                                deviceState.connectionRequest[socketDevice.id]!!.second
                            )
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("自动拒绝") },
                        onClick = {
                            deviceState.connectionRequest[socketDevice.id] = Pair(
                                DeviceConnectType.PERMANENTLY_BANNED,
                                deviceState.connectionRequest[socketDevice.id]!!.second
                            )
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("拒绝") },
                        onClick = {
                            deviceState.connectionRequest[socketDevice.id] = Pair(
                                DeviceConnectType.REJECTED,
                                deviceState.connectionRequest[socketDevice.id]!!.second
                            )
                            expanded = false
                        }
                    )
                }
            }
        }
    )
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
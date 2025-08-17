package app.filemanager.ui.components.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.extensions.DeviceIcon
import app.filemanager.ui.screen.device.ConnectedDeviceScreen
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppDrawerConnectedDevice() {
    val mainState = koinInject<MainState>()
    val deviceState = koinInject<DeviceState>()
    val scope = rememberCoroutineScope()

    // 获取连接的设备列表
    val connectedDevices by remember {
        derivedStateOf {
            deviceState.socketDevices.filter { device ->
                deviceState.remoteDeviceConnections.containsKey(device.id)
            }
        }
    }

    AppDrawerItem("连接设备", actions = {}) {
        if (connectedDevices.size > 1) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Cable, null) },
                label = {
                    Column {
                        Text(connectedDevices.first().name)
                        Text(
                            "查看更多(${connectedDevices.size})",
                            style = typography.bodySmall
                        )
                    }
                },
                selected = false,
                onClick = {
                    if (mainState.windowSize == WindowSizeClass.Compact) {
                        mainState.updateExpandDrawer(false)
                    }
                    mainState.navigator?.push(ConnectedDeviceScreen())
                },
                badge = {
                    Icon(Icons.Default.ExpandLess, null, Modifier.rotate(90f))
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            return@AppDrawerItem
        }

        val device = connectedDevices.first()
        NavigationDrawerItem(
            icon = { device.type.DeviceIcon() },
            label = {
                Column {
                    Text(device.name)
                    Row {
                        Text(
                            "已连接",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.bodySmall
                        )
                    }
                }
            },
            selected = false,
            onClick = {
                if (mainState.windowSize == WindowSizeClass.Compact) {
                    mainState.updateExpandDrawer(false)
                }
                mainState.navigator?.push(ConnectedDeviceScreen())
            },
            badge = {
                Icon(
                    Icons.Default.Close,
                    "断开连接",
                    Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .clickable {
                            scope.launch {
                                deviceState.remoteDeviceConnections[device.id]?.close()
                                deviceState.remoteDeviceConnections.remove(device.id)
                            }
                        }
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
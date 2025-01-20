package app.filemanager.ui.components.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.Device
import app.filemanager.data.main.DiskBase
import app.filemanager.data.main.Local
import app.filemanager.data.main.Network
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.NetworkState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun DiskSwitchButton(
    deskType: DiskBase,
    onSelectDesk: suspend (FileProtocol, DiskBase) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var expandedDevice by remember { mutableStateOf(false) }
    var expandedNetwork by remember { mutableStateOf(false) }

    val deviceState = koinInject<DeviceState>()
    val networkState = koinInject<NetworkState>()

    val devices = deviceState.devices

    if (
        devices.isEmpty() &&
        networkState.networks.isEmpty()
    ) return

    FilterChip(
        selected = true,
        label = { Text(deskType.name) },
        border = null,
        shape = RoundedCornerShape(25.dp),
        trailingIcon = {
            Icon(
                if (expanded)
                    Icons.Default.ArrowDropUp
                else
                    Icons.Default.ArrowDropDown,
                null
            )
        },
        onClick = {
            expanded = !expanded
        }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        DropdownMenuItem(
            text = { Text("本地") },
            onClick = {
                scope.launch {
                    onSelectDesk(FileProtocol.Local, Local())
                }
                expanded = false
            },
        )

        if (devices.isNotEmpty()) {
            DeskDeviceMenuButton(
                expandedDevice,
                devices,
                onSelect = {
                    scope.launch {
                        onSelectDesk(FileProtocol.Device, it)
                    }
                    expanded = false
                },
                onDismissRequest = { expandedDevice = it }
            )
        }
        if (networkState.networks.isNotEmpty()) {
            DeskNetworkMenuButton(
                expandedNetwork,
                networkState.networks,
                onSelect = {
                    scope.launch {
                        onSelectDesk(FileProtocol.Network, it)
                    }
                    expanded = false
                },
                onDismissRequest = { expandedNetwork = it }
            )
        }
    }
}

@Composable
private fun <T : DiskBase> DiskSwitchMenuButton(
    title: String,
    expanded: Boolean,
    items: List<T>,
    onSelect: (T) -> Unit,
    onDismissRequest: (Boolean) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(title) },
        onClick = { onDismissRequest(true) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(items.size.toString())
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowRight,
                    contentDescription = null
                )
            }
        }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismissRequest(false) }
    ) {
        for (item in items) {
            DropdownMenuItem(
                text = { Text(item.name) },
                onClick = {
                    onSelect(item)
                    onDismissRequest(false)
                }
            )
        }
    }
}

@Composable
fun DeskDeviceMenuButton(
    expanded: Boolean,
    devices: List<Device>,
    onSelect: (Device) -> Unit,
    onDismissRequest: (Boolean) -> Unit,
) {
    DiskSwitchMenuButton(
        title = "设备",
        expanded = expanded,
        items = devices,
        onSelect = onSelect,
        onDismissRequest = onDismissRequest
    )
}

@Composable
fun DeskNetworkMenuButton(
    expanded: Boolean,
    networks: List<Network>,
    onSelect: (Network) -> Unit,
    onDismissRequest: (Boolean) -> Unit,
) {
    DiskSwitchMenuButton(
        title = "网络",
        expanded = expanded,
        items = networks,
        onSelect = onSelect,
        onDismissRequest = onDismissRequest
    )
}

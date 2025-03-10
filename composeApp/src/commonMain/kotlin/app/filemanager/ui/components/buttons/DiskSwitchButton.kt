package app.filemanager.ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.*
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.NetworkState
import org.koin.compose.koinInject


@Composable
fun DiskSwitchButton(
    deskType: DiskBase,
    onSelectDesk: (FileProtocol, DiskBase) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var expandedDevice by remember { mutableStateOf(false) }
    var expandedShare by remember { mutableStateOf(false) }
    var expandedNetwork by remember { mutableStateOf(false) }

    val deviceState = koinInject<DeviceState>()
    val networkState = koinInject<NetworkState>()

    val devices = deviceState.devices
    val shares = deviceState.shares

    if (
        devices.isEmpty() &&
        shares.isEmpty() &&
        networkState.networks.isEmpty()
    ) return

    FilterChip(
        selected = true,
        label = {
            if (deskType is Share) {
                Text("来自${deskType.name}的分享")
                return@FilterChip
            }
            Text(deskType.name)
        },
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
                onSelectDesk(FileProtocol.Local, Local())
                expanded = false
            },
        )

        if (devices.isNotEmpty()) {
            DeskDeviceMenuButton(
                expandedDevice,
                devices,
                onSelect = {
                    onSelectDesk(FileProtocol.Device, it)
                    expanded = false
                },
                onDismissRequest = { expandedDevice = it }
            )
        }
        if (shares.isNotEmpty()) {
            DeskShareMenuButton(
                expandedShare,
                shares,
                onSelect = {
                    onSelectDesk(FileProtocol.Share, it)
                    expanded = false
                },
                onDismissRequest = { expandedShare = it }
            )
        }
        if (networkState.networks.isNotEmpty()) {
            DeskNetworkMenuButton(
                expandedNetwork,
                networkState.networks,
                onSelect = {
                    onSelectDesk(FileProtocol.Network, it)
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
    Box(Modifier.wrapContentSize(Alignment.TopStart)) {
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
fun DeskShareMenuButton(
    expanded: Boolean,
    shares: List<Share>,
    onSelect: (Share) -> Unit,
    onDismissRequest: (Boolean) -> Unit,
) {
    DiskSwitchMenuButton(
        title = "分享",
        expanded = expanded,
        items = shares,
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

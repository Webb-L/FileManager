package app.filemanager.ui.screen.device

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
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

        LaunchedEffect(Unit) {
            devices = database.deviceQueries.queryAll().executeAsList()
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
                    DeviceListItem(device = device)
                }
            }
        }
    }

    @Composable
    private fun DeviceListItem(device: Device) {
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
            }
        )
    }
}

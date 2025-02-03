package app.filemanager.ui.screen.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.data.main.DeviceType.*
import app.filemanager.db.Device
import app.filemanager.db.FileManagerDatabase
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.main.MainState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class DeviceSettingsScreen() : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mainState = koinInject<MainState>()
        val database = koinInject<FileManagerDatabase>()
        val scope = rememberCoroutineScope()

        val deviceList = remember { mutableStateListOf<Device>() }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        var category by remember { mutableStateOf(DeviceCategory.SERVER) }
        var deviceName by remember { mutableStateOf("") }
        val isRefreshing = remember { mutableStateOf(false) }
        val state = rememberPullToRefreshState()

        val onRefresh: () -> Unit = {
            deviceList.apply {
                clear()
                addAll(
                    database.deviceQueries.queryByNameLikeAndCategory(
                        "%$deviceName%",
                        category
                    ).executeAsList()
                )
            }
        }

        val connectTypeNameMap = mapOf(
            AUTO_CONNECT to "自动同意",
            PERMANENTLY_BANNED to "自动拒绝",
            APPROVED to "等待",
        )

        val connectTypeNameClientMap = mapOf(
            AUTO_CONNECT to "自动连接",
            APPROVED to "手动连接",
        )

        LaunchedEffect(category, deviceName) {
            onRefresh()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设备管理") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                mainState.updateScreen(null)
                                navigator.pop()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton({
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(if (drawerState.isClosed) Icons.Default.Search else Icons.Default.Close, null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            DismissibleNavigationDrawer(
                modifier = Modifier.padding(paddingValues).fillMaxWidth(),
                drawerState = drawerState,
                drawerContent = {
                    DismissibleDrawerSheet {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp)
                        ) {
                            TextField(
                                value = deviceName,
                                onValueChange = { deviceName = it },
                                label = { Text("设备名") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    if (deviceName.isNotEmpty()) {
                                        IconButton(onClick = { deviceName = "" }) {
                                            Icon(Icons.Default.Close, "清除")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DeviceCategory.SERVER to "其他设备访问我的设备",
                            DeviceCategory.CLIENT to "我的设备访问其他设备"
                        ).forEach { (cat, label) ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(label) },
                            )
                        }
                    }

                    PullToRefreshBox(
                        state = state,
                        isRefreshing = isRefreshing.value,
                        onRefresh = onRefresh
                    ) {
                        GridList(
                            exception = if (deviceList.isEmpty()) EmptyDataException() else null
                        ) {
                            items(deviceList) { device ->
                                DeviceListItem(
                                    device = device,
                                    connectTypeNameMap = connectTypeNameMap,
                                    connectTypeNameClientMap = connectTypeNameClientMap,
                                    updateConnectionType = { connectionType, deviceId ->
                                        database.deviceQueries.updateConnectionTypeByIdAndCategory(
                                            connectionType,
                                            deviceId,
                                            category
                                        )
                                        onRefresh()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun DeviceListItem(
        device: Device,
        connectTypeNameMap: Map<DeviceConnectType, String>,
        connectTypeNameClientMap: Map<DeviceConnectType, String>,
        updateConnectionType: (DeviceConnectType, String) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text(device.name) },
            trailingContent = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, null)
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val connectionMap =
                            if (device.category == DeviceCategory.SERVER) connectTypeNameMap else connectTypeNameClientMap
                        connectionMap.filter { it.key != device.connectionType }.forEach { (type, label) ->
                            DropdownMenuItem(
                                onClick = {
                                    updateConnectionType(type, device.id)
                                    expanded = false
                                },
                                text = { Text(label) }
                            )
                        }
                    }
                }
            },
            overlineContent = {
                Text(
                    "上次连接时间: ${
                        Instant.fromEpochSeconds(device.lastConnection)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .run { "$date $time" }
                    }"
                )
            },
            leadingContent = {
                val icon = when (device.type) {
                    Android -> Icons.Default.PhoneAndroid
                    IOS -> Icons.Default.PhoneIphone
                    JVM -> Icons.Default.Devices
                    JS -> Icons.Default.Javascript
                }
                Icon(icon, null)
            },
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (device.connectionType in listOf(AUTO_CONNECT, PERMANENTLY_BANNED)) {
                        Icon(
                            if (device.connectionType == AUTO_CONNECT) Icons.Default.Check else Icons.Default.Close,
                            null
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    val text = if (device.category == DeviceCategory.SERVER) {
                        when (device.connectionType) {
                            AUTO_CONNECT -> "自动同意"
                            PERMANENTLY_BANNED -> "自动拒绝"
                            else -> "等待"
                        }
                    } else {
                        when (device.connectionType) {
                            AUTO_CONNECT -> "自动连接"
                            else -> "手动连接"
                        }
                    }
                    Text(text)
                }
            }
        )
    }
}
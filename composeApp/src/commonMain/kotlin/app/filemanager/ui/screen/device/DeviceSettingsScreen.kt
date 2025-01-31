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

        val deviceList = mutableStateListOf<Device>()

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val scope = rememberCoroutineScope()

        var category by remember {
            mutableStateOf(DeviceCategory.SERVER)
        }

        var deviceName by remember {
            mutableStateOf("")
        }

        val state = rememberPullToRefreshState()
        var isRefreshing by remember {
            mutableStateOf(false)
        }
        val onRefresh: () -> Unit = {
            deviceList.apply {
                clear()
                addAll(
                    database.deviceQueries.queryByNameLikeAndCategory(
                        "%${deviceName}%",
                        category
                    ).executeAsList()
                )
            }
        }

        val connectTypeNameMap = mapOf(
            AUTO_CONNECT to "自动同意",
            PERMANENTLY_BANNED to "自动拒绝",
            APPROVED to "同意",
            REJECTED to "拒绝",
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
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            if (drawerState.isClosed) {
                                Icon(Icons.Default.Search, null)
                            } else {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            DismissibleNavigationDrawer(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
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
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (deviceName.isEmpty()) return@TextField
                                    IconButton(onClick = { deviceName = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "清除")
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("设备分类")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = category == DeviceCategory.SERVER,
                                    onClick = {
                                        category = DeviceCategory.SERVER
                                    },
                                    label = { Text("其他设备访问我的设备") }
                                )
                                FilterChip(
                                    selected = category == DeviceCategory.CLIENT,
                                    onClick = {
                                        category = DeviceCategory.CLIENT
                                    },
                                    label = { Text("我的设备访问其他设备") }
                                )
                            }
                        }
                    }

                }
            ) {
                PullToRefreshBox(
                    state = state,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                ) {
                    GridList(
                        exception = if (deviceList.isEmpty()) EmptyDataException() else null
                    ) {
                        items(deviceList) { device ->
                            var expanded by remember { mutableStateOf(false) }
                            ListItem(
                                headlineContent = { Text(text = device.name) },
                                trailingContent = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, null)
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            for (entry in connectTypeNameMap.filter { it.key != device.connectionType }) {
                                                DropdownMenuItem(
                                                    onClick = {
                                                        database.deviceQueries.updateConnectionTypeByIdAndCategory(
                                                            entry.key,
                                                            device.id,
                                                            category
                                                        )
                                                        deviceList.apply {
                                                            clear()
                                                            addAll(
                                                                database.deviceQueries.queryByNameLikeAndCategory(
                                                                    "%${deviceName}%",
                                                                    category
                                                                ).executeAsList()
                                                            )
                                                        }
                                                    },
                                                    text = { Text(entry.value) }
                                                )
                                            }
                                        }
                                    }
                                },
                                overlineContent = {
                                    Text(
                                        text = "上次连接时间: ${
                                            Instant.fromEpochSeconds(device.lastConnection)
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                                .run { "$date $time" }
                                        }"
                                    )
                                },
                                leadingContent = {
                                    when (device.type) {
                                        Android -> Icon(Icons.Default.PhoneAndroid, null)
                                        IOS -> Icon(Icons.Default.PhoneIphone, null)
                                        JVM -> Icon(Icons.Default.Devices, null)
                                        JS -> Icon(Icons.Default.Javascript, null)
                                    }
                                },
                                supportingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (listOf(AUTO_CONNECT, PERMANENTLY_BANNED).contains(device.connectionType)) {
                                            Icon(
                                                if (device.connectionType == AUTO_CONNECT)
                                                    Icons.Default.Check
                                                else
                                                    Icons.Default.Close,
                                                null
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            when (device.connectionType) {
                                                AUTO_CONNECT -> "自动同意"
                                                PERMANENTLY_BANNED -> "自动拒绝"
                                                APPROVED -> "同意"
                                                REJECTED -> "拒绝"
                                                WAITING -> "等待"
                                            }
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
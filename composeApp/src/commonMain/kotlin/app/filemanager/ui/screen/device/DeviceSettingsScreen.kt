package app.filemanager.ui.screen.device

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.filemanager.data.device.DeviceJoinDeviceRole
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceConnectType.*
import app.filemanager.data.main.DeviceType.*
import app.filemanager.exception.EmptyDataException
import app.filemanager.ui.components.EditableExposedDropdownMenu
import app.filemanager.ui.components.GridList
import app.filemanager.ui.state.device.DeviceRoleState
import app.filemanager.ui.state.device.DeviceSettingsState
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
    private val connectTypeNameMap = mapOf(
        AUTO_CONNECT to "自动同意",
        PERMANENTLY_BANNED to "自动拒绝",
        APPROVED to "等待",
    )

    private val connectTypeNameClientMap = mapOf(
        AUTO_CONNECT to "自动连接",
        APPROVED to "手动连接",
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val mainState = koinInject<MainState>()
        val deviceSettingsState = koinInject<DeviceSettingsState>()
        val scope = rememberCoroutineScope()

        var isShowSearch by remember { mutableStateOf(false) }
        val category by deviceSettingsState.category.collectAsState()
        val deviceName by deviceSettingsState.deviceName.collectAsState()
        var updateDevice by remember { mutableStateOf<Pair<Int, DeviceJoinDeviceRole>?>(null) }
        val isRefreshing = remember { mutableStateOf(false) }
        val state = rememberPullToRefreshState()

        LaunchedEffect(category, deviceName) {
            deviceSettingsState.onRefresh()
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
                                isShowSearch = !isShowSearch
                            }
                        }) {
                            Icon(if (!isShowSearch) Icons.Default.Search else Icons.Default.Close, null)
                        }
                    }
                )
            },
            floatingActionButton = {
                if (category == DeviceCategory.SERVER) {
                    ExtendedFloatingActionButton({ navigator.push(DeviceRoleScreen()) }) {
                        Icon(Icons.Filled.Person, null)
                        Spacer(Modifier.width(8.dp))
                        Text("角色")
                    }
                }
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxWidth().padding(paddingValues)) {
                AnimatedVisibility(isShowSearch) {
                    TextField(
                        value = deviceName,
                        onValueChange = { deviceSettingsState.updateDeviceName(it) },
                        label = { Text("设备名") },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (deviceName.isNotEmpty()) {
                                IconButton(onClick = { deviceSettingsState.updateDeviceName("") }) {
                                    Icon(Icons.Default.Close, "清除")
                                }
                            }
                        }
                    )
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    listOf(
                        DeviceCategory.SERVER to "其他设备访问我的设备",
                        DeviceCategory.CLIENT to "我的设备访问其他设备"
                    ).forEachIndexed { index, (cat, label) ->
                        SegmentedButton(
                            selected = category == cat,
                            onClick = { deviceSettingsState.updateCategory(cat) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) { Text(label) }
                    }
                }

                PullToRefreshBox(
                    state = state,
                    isRefreshing = isRefreshing.value,
                    onRefresh = deviceSettingsState.onRefresh
                ) {
                    GridList(
                        exception = if (deviceSettingsState.devices.isEmpty()) EmptyDataException() else null
                    ) {
                        itemsIndexed(deviceSettingsState.devices) { index, device ->
                            if (device.category == DeviceCategory.SERVER) {
                                DeviceListServerItem(
                                    device = device,
                                    updateConnectionType = { connectionType, deviceId ->
                                        deviceSettingsState.updateDeviceCategory(
                                            connectionType,
                                            deviceId,
                                            category
                                        )
                                    }
                                ) { updateDevice = Pair(index, device) }
                            } else {
                                DeviceListClientItem(
                                    device = device,
                                    updateConnectionType = { connectionType, deviceId ->
                                        deviceSettingsState.updateDeviceCategory(
                                            connectionType,
                                            deviceId,
                                            category
                                        )
                                    }
                                ) {
                                    updateDevice = Pair(index, device)
                                }
                            }
                        }
                    }
                }
            }

            if (updateDevice != null) {
                EditDeviceDialog(
                    device = updateDevice!!.second,
                    onDismissRequest = { updateDevice = null },
                    onSaveChange = { deviceSettingsState.updateDevice(updateDevice!!.first, it) }
                )
            }
        }
    }

    @Composable
    fun DeviceListServerItem(
        device: DeviceJoinDeviceRole,
        updateConnectionType: (DeviceConnectType, String) -> Unit,
        onClick: () -> Unit = {}
    ) {
        var expanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onClick),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (device.type) {
                            Android -> Icons.Default.PhoneAndroid
                            IOS -> Icons.Default.PhoneIphone
                            JVM -> Icons.Default.Devices
                            JS -> Icons.Default.Javascript
                        }
                        Icon(icon, null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = when (device.connectionType) {
                                    AUTO_CONNECT -> "自动同意"
                                    PERMANENTLY_BANNED -> "自动拒绝"
                                    else -> "等待"
                                },
                                color = when (device.connectionType) {
                                    AUTO_CONNECT -> colorScheme.primary
                                    PERMANENTLY_BANNED -> colorScheme.error
                                    else -> Color.Unspecified
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            connectTypeNameMap.filter { it.key != device.connectionType }.forEach { (type, label) ->
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
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "上次连接时间: ${
                                Instant.fromEpochSeconds(device.lastConnection)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .run { "$date $time" }
                            }",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "设备角色: ${device.roleName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    fun DeviceListClientItem(
        device: DeviceJoinDeviceRole,
        updateConnectionType: (DeviceConnectType, String) -> Unit,
        onClick: () -> Unit = {}
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
                    val text = when (device.connectionType) {
                        AUTO_CONNECT -> "自动连接"
                        else -> "手动连接"
                    }
                    Text(text)
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }

    @Composable
    fun EditDeviceDialog(
        device: DeviceJoinDeviceRole,
        onDismissRequest: () -> Unit,
        onSaveChange: (updatedDevice: DeviceJoinDeviceRole) -> Unit
    ) {
        val connectionMap =
            if (device.category == DeviceCategory.SERVER) connectTypeNameMap else connectTypeNameClientMap

        val roleState = koinInject<DeviceRoleState>()

        // 维护当前可编辑的状态
        var deviceName by remember { mutableStateOf(device.name) }
        var deviceConnectType by remember {
            mutableStateOf(
                connectionMap[device.connectionType] ?: connectionMap.values.last()
            )
        }
        var deviceRole by remember { mutableStateOf(device.roleName) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    enabled = deviceName.isNotEmpty() && deviceConnectType.isNotEmpty(),
                    onClick = {
                        val correspondingKey =
                            connectionMap.entries.find { entry -> entry.value == deviceConnectType }?.key

                        onSaveChange(
                            device.copy(
                                name = deviceName,
                                connectionType = correspondingKey!!,
                                roleId = if (device.category == DeviceCategory.SERVER)
                                    roleState.roles.firstOrNull { it.name == deviceRole }?.id ?: -1L
                                else
                                    -1L,
                            )
                        )
                        onDismissRequest()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("取消")
                }
            },
            title = { Text(text = "编辑设备") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. 编辑设备名称
                    TextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("设备名称") },
                        isError = deviceName.isEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. 选择连接状态
                    EditableExposedDropdownMenu(
                        options = connectionMap.values.toList(),
                        value = deviceConnectType,
                        isError = deviceConnectType.isEmpty(),
                        label = { Text("设备连接") },
                        onValueChange = { deviceConnectType = it }
                    )

                    // 3. 选择设备角色
                    if (device.category == DeviceCategory.SERVER) {
                        EditableExposedDropdownMenu(
                            options = roleState.roles.map { it.name },
                            value = deviceRole,
                            label = { Text("设备角色") },
                            onValueChange = { deviceRole = it }
                        )
                    }
                }
            }
        )
    }
}
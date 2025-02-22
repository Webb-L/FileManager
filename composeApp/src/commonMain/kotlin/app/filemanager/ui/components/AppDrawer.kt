package app.filemanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.filemanager.data.StatusEnum
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.toIcon
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceCategory
import app.filemanager.data.main.DeviceConnectType
import app.filemanager.data.main.DeviceType.*
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.db.FileManagerDatabase
import app.filemanager.service.data.ConnectType.*
import app.filemanager.service.data.SocketDevice
import app.filemanager.service.rpc.RpcClientManager.Companion.PORT
import app.filemanager.service.rpc.SocketClientIPEnum
import app.filemanager.service.rpc.getAllIPAddresses
import app.filemanager.ui.screen.device.DeviceSettingsScreen
import app.filemanager.ui.screen.file.FavoriteScreen
import app.filemanager.ui.screen.main.NotificationScreen
import app.filemanager.ui.screen.task.TaskResultScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.*
import app.filemanager.ui.state.main.DrawerState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer() {
    val mainState = koinInject<MainState>()
    val drawerState = koinInject<DrawerState>()
    val taskState = koinInject<TaskState>()
    val deviceState = koinInject<DeviceState>()
    val isDeviceAdd by deviceState.isDeviceAdd.collectAsState()

    val isExpandNetwork by drawerState.isExpandNetwork.collectAsState()

    val scope = rememberCoroutineScope()

    ModalDrawerSheet {
        TopAppBar(
            title = { Text("Name") },
            navigationIcon = {
                IconButton({}) {
                    Icon(Icons.Default.AccountCircle, null)
                }
            },
            actions = {
                IconButton({
                    mainState.updateScreen(NotificationScreen())
                }) {
                    BadgedBox(badge = { Badge { Text("1") } }) {
                        Icon(Icons.Default.Notifications, null)
                    }
                }
                IconButton({}) {
                    Icon(Icons.Default.Settings, null)
                }
            }
        )
        HorizontalDivider()
        LazyColumn {
//            item {
//                AppDrawerHeader("工具箱", actions = {
//                    Icon(
//                        Icons.Default.ChevronRight,
//                        null,
//                        Modifier.clip(RoundedCornerShape(25.dp))
//                            .clickable { }
//                    )
//                })
//                Spacer(Modifier.height(12.dp))
//            }
            if (taskState.tasks.isNotEmpty()) {
                item { AppDrawerTask() }
                item { HorizontalDivider() }
            }
            item { AppDrawerBookmark() }
            item { HorizontalDivider() }
            item { AppDrawerDevice() }
// TODO 3.0版本
//            item { HorizontalDivider() }
//            item {
//                AppDrawerItem(
//                    "网络",
//                    actions = {
//                        Row {
//                            Icon(Icons.Default.Add, null, Modifier)
//                            Spacer(Modifier.width(8.dp))
//                            Icon(
//                                if (isExpandNetwork) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                                null,
//                                Modifier.clip(RoundedCornerShape(25.dp))
//                                    .clickable { drawerState.updateExpandNetwork(!isExpandNetwork) }
//                            )
//                        }
//                    }
//                ) {
//                }
//            }
//            item { HorizontalDivider() }
        }
    }

    if (isDeviceAdd) {
        val allIPAddresses = getAllIPAddresses(
            type = SocketClientIPEnum.ALL
        )
        TextFieldDialog(
            "添加设备", label = "IP地址 或 IP地址:端口",
            verifyFun = { text ->
                if (text.isEmpty()) {
                    Pair(true, "请输入IP地址 或 IP地址:端口")
                } else {
                    if (allIPAddresses.any { text.indexOf(it) == 0 }) {
                        Pair(true, "禁止使用本地地址")
                    } else {
                        val regex = Regex(
                            "^(([0-9]{1,3}\\.){3}[0-9]{1,3}|\\[([a-fA-F0-9:]+)])(:([0-9]{1,5}))?$"
                        )
                        if (regex.matches(text)) {
                            Pair(false, "")
                        } else {
                            Pair(true, "请输入正确的IP地址 或 IP地址:端口")
                        }
                    }
                }
            }
        ) {
            if (it.isEmpty()) {
                deviceState.updateDeviceAdd(false)
                return@TextFieldDialog
            }
            val inputText = it

            val ip: String
            var port: String? = null

            if (inputText.startsWith("[")) {
                val rightBracketIndex = inputText.indexOf(']')
                if (rightBracketIndex != -1) {
                    // 截取完整的 IPv6 地址部分，如 [2001:db8::1]
                    ip = inputText.substring(0, rightBracketIndex + 1)
                    // 检查是否存在端口部分
                    if (rightBracketIndex + 1 < inputText.length && inputText[rightBracketIndex + 1] == ':') {
                        port = inputText.substring(rightBracketIndex + 2)
                    }
                } else {
                    // 若没有找到 ']'，则默认为整串都是 IP；也可根据需要进行异常处理
                    ip = inputText
                }
            } else {
                // 普通情况（如 IPv4 或不带中括号的 IPv6），用冒号分割
                val parts = inputText.split(":")
                ip = parts[0]
                if (parts.size > 1) {
                    port = parts[1]
                }
            }

            deviceState.updateDeviceAdd(false)

            scope.launch {
                try {
                    deviceState.pingDevice(ip, (port ?: PORT).toString().toInt())
                } catch (e: Exception) {
                }
            }
        }
    }
}

@Composable
private fun AppDrawerTask() {
    val taskState = koinInject<TaskState>()
    val mainState = koinInject<MainState>()
    var checkedTask by remember {
        mutableStateOf<Task?>(null)
    }

    AppDrawerItem("任务", actions = {
    }) {
        if (taskState.tasks.size > 1) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.TaskAlt, null) },
                label = {
                    Column {
                        Text("任务(${taskState.tasks.size})")
                        Text(
                            "执行中(${taskState.tasks.filter { it.status == StatusEnum.LOADING }.size})-暂停中(${taskState.tasks.filter { it.status == StatusEnum.PAUSE }.size})-失败(${taskState.tasks.filter { it.status == StatusEnum.FAILURE }.size})",
                            style = typography.bodySmall
                        )
                    }
                },
                selected = false,
                onClick = {
                },
                badge = {
                    Icon(Icons.Default.ExpandLess, null, Modifier.rotate(90f))
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            return@AppDrawerItem
        }

        val task = taskState.tasks.first()
        NavigationDrawerItem(
            icon = {
                when (task.taskType) {
                    TaskType.Copy -> Icon(Icons.Outlined.FileCopy, null)
                    TaskType.Move -> Icon(Icons.Default.ContentCut, null)
                    TaskType.Delete -> Icon(Icons.Default.Delete, null)
                }
            },
            label = {
                Column {
                    when (task.taskType) {
                        TaskType.Copy -> Text("复制中")
                        TaskType.Move -> Text("移动中")
                        TaskType.Delete -> Text("删除中")
                    }
                    Row {
                        task.protocol.toIcon()
                        Text(
                            task.values["path"] ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography.bodySmall
                        )
                    }
                }
            },
            selected = false,
            onClick = {
                checkedTask = task
            },
            badge = {
                when (task.status) {
                    StatusEnum.SUCCESS -> {}
                    StatusEnum.FAILURE -> Row {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ExpandLess, null, Modifier.rotate(90f))
                    }

                    StatusEnum.PAUSE -> {
                        Icon(Icons.Default.Stop, null)
                    }

                    StatusEnum.LOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 3.dp
                    )
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
    if (checkedTask != null) {
        TaskInfoDialog(
            checkedTask!!,
            onConfirm = {},
            onDismiss = {
                checkedTask = null
            },
            onToResult = {
                mainState.updateScreen(TaskResultScreen(checkedTask!!))
                checkedTask = null
            }
        )
    }
}

@Composable
private fun AppDrawerBookmark() {
    val scope = rememberCoroutineScope()

    val mainState = koinInject<MainState>()
    val isFavorite by mainState.isFavorite.collectAsState()

    val fileState = koinInject<FileState>()
    val path by fileState.path.collectAsState()
    val deskType by fileState.deskType.collectAsState()

    val drawerState = koinInject<DrawerState>()
    val isExpandBookmark by drawerState.isExpandBookmark.collectAsState()

    AppDrawerItem(
        if (deskType is Device) "${deskType.name} - 书签" else "书签",
        actions = {
            Row {
                Icon(Icons.Default.Add, null, Modifier)
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpandBookmark) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    Modifier.clip(RoundedCornerShape(25.dp))
                        .clickable { drawerState.updateExpandBookmark(!isExpandBookmark) }
                )
            }
        }
    ) {
        if (!isExpandBookmark) return@AppDrawerItem
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("收藏") },
            selected = isFavorite,
            onClick = {
                mainState.updateFavorite(true)
                mainState.updateScreen(FavoriteScreen())
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        for (bookmark in drawerState.bookmarks) {
            NavigationDrawerItem(
                icon = {
                    val icon = when (bookmark.iconType) {
                        DrawerBookmarkType.Home -> Icons.Default.Home
                        DrawerBookmarkType.Image -> Icons.Default.Image
                        DrawerBookmarkType.Audio -> Icons.Default.Headphones
                        DrawerBookmarkType.Video -> Icons.Default.Videocam
                        DrawerBookmarkType.Document -> Icons.Default.Description
                        DrawerBookmarkType.Download -> Icons.Default.Download
                        DrawerBookmarkType.Custom -> Icons.Default.Bookmark
                    }
                    Icon(icon, null)
                },
                label = { Text(bookmark.name) },
                selected = !isFavorite && path == bookmark.path,
                onClick = {
                    scope.launch {
                        fileState.updatePath(bookmark.path)
                    }
                    mainState.updateScreen(null)
                    mainState.updateFavorite(false)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
private fun AppDrawerItem(
    title: String,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AppDrawerHeader(title, actions)
    content()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AppDrawerDevice() {
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
                            mainState.updateScreen(DeviceSettingsScreen())
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
        for ((index, device) in deviceState.socketDevices.sortedByDescending { it.client != null }.withIndex()) {
            NavigationDrawerItem(
                icon = {
                    if (device.connectType == Loading) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        return@NavigationDrawerItem
                    }

                    when (device.type) {
                        Android -> Icon(Icons.Default.PhoneAndroid, null)
                        IOS -> Icon(Icons.Default.PhoneIphone, null)
                        JVM -> Icon(Icons.Default.Devices, null)
                        JS -> Icon(Icons.Default.Javascript, null)
                    }
                },
                badge = {
                    when (device.connectType) {
                        Connect -> Icon(
                            Icons.Default.Close,
                            "取消连接",
                            Modifier
                                .clip(RoundedCornerShape(25.dp))
                                .clickable {
                                    if (device.client?.disconnect() == true) {
                                        deviceState.socketDevices[index] = device.withCopy(
                                            connectType = UnConnect
                                        )
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
                            deviceState.socketDevices[index] = device.withCopy(
                                connectType = Loading
                            )
                            deviceState.connect(device)
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
                            // TODO 断开连接
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
private fun AppDrawerNetwork() {
}


@Composable
private fun AppDrawerHeader(title: String, actions: @Composable () -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
        Text(title)
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
fun DeviceConnectNewDialog(
    socketDevice: SocketDevice,
    onCancel: () -> Unit
) {
    val database = koinInject<FileManagerDatabase>()
    val deviceState = koinInject<DeviceState>()

    val onConnect: (Boolean) -> Unit = { isAuto ->
        database.deviceQueries.insert(
            id = socketDevice.id,
            name = socketDevice.name,
            type = socketDevice.type,
            connectionType = if (isAuto) DeviceConnectType.AUTO_CONNECT else DeviceConnectType.WAITING,
            category = DeviceCategory.CLIENT,
            -1
        )
        val index = deviceState.socketDevices.indexOfFirst { it.id == socketDevice.id }
        if (index >= 0) {
            deviceState.socketDevices[index] = socketDevice.withCopy(
                connectType = Loading
            )
            deviceState.connect(socketDevice)
            onCancel()
        } else {
            onCancel()
        }
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
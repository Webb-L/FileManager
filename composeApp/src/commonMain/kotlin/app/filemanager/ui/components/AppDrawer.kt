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
import app.filemanager.data.file.toIcon
import app.filemanager.data.main.Device
import app.filemanager.data.main.DeviceType.*
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.service.data.ConnectType.*
import app.filemanager.ui.screen.file.FavoriteScreen
import app.filemanager.ui.screen.task.TaskResultScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.*
import app.filemanager.ui.state.main.DrawerState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer() {
    val drawerState = koinInject<DrawerState>()
    val taskState = koinInject<TaskState>()
    val deviceState = koinInject<DeviceState>()
    val isDeviceAdd by deviceState.isDeviceAdd.collectAsState()

    val isExpandNetwork by drawerState.isExpandNetwork.collectAsState()

    ModalDrawerSheet {
        TopAppBar(
            title = { Text("Name") },
            navigationIcon = {
                IconButton({}) {
                    Icon(Icons.Default.AccountCircle, null)
                }
            },
            actions = {
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
                item { HorizontalDivider() }
                item { AppDrawerTask() }
            }
            item { HorizontalDivider() }
            item { AppDrawerBookmark() }
            item { HorizontalDivider() }
            item { AppDrawerDevice() }
            item { HorizontalDivider() }
            item {
                AppDrawerItem(
                    "网络",
                    actions = {
                        Row {
                            Icon(Icons.Default.Add, null, Modifier)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isExpandNetwork) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                Modifier.clip(RoundedCornerShape(25.dp))
                                    .clickable { drawerState.updateExpandNetwork(!isExpandNetwork) }
                            )
                        }
                    }
                ) {
                }
            }
            item { HorizontalDivider() }
        }
    }

    if (isDeviceAdd) {
        TextFieldDialog("设备服务", label = "IP地址") {
            deviceState.updateDeviceAdd(false)
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
                        }
                )
                Spacer(Modifier.width(8.dp))
                IpsButton()
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
        for ((index, device) in deviceState.socketDevices.withIndex()) {
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
                                    if (device.socketManger?.disconnect() == true) {
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
                        New, UnConnect, Fail, Rejected -> {
                            deviceState.socketDevices[index] = device.withCopy(
                                connectType = Loading
                            )
                            scope.launch {
                                try {
                                    mainState.socketClientManger.connect(device)
                                } catch (e: Exception) {
                                    deviceState.socketDevices[index] = device.withCopy(
                                        connectType = Fail
                                    )
                                }
                            }
                        }

                        Connect -> {
                            // TODO 断开连接
                        }

                        Loading -> {}
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
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
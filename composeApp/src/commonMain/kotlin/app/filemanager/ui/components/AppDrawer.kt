package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.Device
import app.filemanager.data.main.DrawerBookmarkType
import app.filemanager.service.WebSocketServiceManager
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer() {
    val drawerState = koinInject<DrawerState>()
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
        Divider()
        LazyColumn {
            item {
                AppDrawerHeader("工具箱", actions = {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        Modifier.clip(RoundedCornerShape(25.dp))
                            .clickable { }
                    )
                })
                Spacer(Modifier.height(12.dp))
            }
            item { Divider() }
            item { AppDrawerBookmark() }
            item { Divider() }
            item { AppDrawerDevice() }
            item { Divider() }
            item {
                AppDrawerItem(
                    "网络",
                    actions = {
                        Row {
                            Icon(Icons.Default.Add, null, Modifier.clip(RoundedCornerShape(25.dp)).clickable { })
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
            item { Divider() }
        }
    }

    if (isDeviceAdd) {
        TextFieldDialog("设备服务", label = "IP地址") {
            deviceState.updateDeviceAdd(false)
        }
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
                Icon(Icons.Default.Add, null, Modifier.clip(RoundedCornerShape(25.dp)).clickable { })
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
            onClick = { mainState.updateFavorite(true) },
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
    val drawerState = koinInject<DrawerState>()
    val isExpandDevice by drawerState.isExpandDevice.collectAsState()

    val deviceState = koinInject<DeviceState>()

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
                        .clickable {

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
        for (device in deviceState.devices) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Devices, null) },
                label = { Text(device.name) },
                selected = false,
                onClick = {},
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    val webSocketServiceManager = koinInject<WebSocketServiceManager>()
    LaunchedEffect(Unit) {
        webSocketServiceManager.connect("127.0.0.1")
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
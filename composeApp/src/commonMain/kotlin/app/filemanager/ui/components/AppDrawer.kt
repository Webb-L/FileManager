package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.DrawerBookmarkIcon
import app.filemanager.service.WebSocketService
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import org.koin.compose.koinInject

@Composable
fun AppDrawer() {
    val drawerState = koinInject<DrawerState>()
    val isExpandDevice by drawerState.isExpandDevice.collectAsState()
    val isDeviceAdd by drawerState.isDeviceAdd.collectAsState()

    val isExpandNetwork by drawerState.isExpandNetwork.collectAsState()

    ModalDrawerSheet {
        LazyColumn {
            item { AppDrawerBookmark() }
            item { Divider() }
            item {
                AppDrawerItem(
                    "设备",
                    false,
                    actions = {
                        Row {
                            Icon(
                                Icons.Default.Add,
                                null,
                                Modifier.clip(RoundedCornerShape(25.dp)).clickable {
                                    drawerState.updateDeviceAdd(true)
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
                }
            }
            item { Divider() }
            item {
                AppDrawerItem(
                    "网络",
                    false,
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
        }
    }

    if (isDeviceAdd) {
        TextFieldDialog("新增设备", label = "IP地址") {
            drawerState.updateDeviceAdd(false)
        }
    }

    LaunchedEffect(Unit) {
        println(WebSocketService().scanService())
    }
}

@Composable
private fun AppDrawerBookmark() {
    val mainState = koinInject<MainState>()
    val path by mainState.path.collectAsState()

    val drawerState = koinInject<DrawerState>()
    val isExpandBookmark by drawerState.isExpandBookmark.collectAsState()

    AppDrawerItem(
        "书签",
        isExpand = isExpandBookmark,
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

        for (bookmark in drawerState.bookmarks) {
            NavigationDrawerItem(
                icon = {
                    val icon = when (bookmark.iconType) {
                        DrawerBookmarkIcon.Favorite -> Icons.Default.Favorite
                        DrawerBookmarkIcon.Home -> Icons.Default.Home
                        DrawerBookmarkIcon.Image -> Icons.Default.Image
                        DrawerBookmarkIcon.Audio -> Icons.Default.Headphones
                        DrawerBookmarkIcon.Video -> Icons.Default.Videocam
                        DrawerBookmarkIcon.Document -> Icons.Default.Description
                        DrawerBookmarkIcon.Download -> Icons.Default.Download
                        DrawerBookmarkIcon.Custom -> Icons.Default.Bookmark
                    }
                    Icon(icon, null)
                },
                label = { Text(bookmark.name) },
                selected = path == bookmark.path,
                onClick = { mainState.updatePath(bookmark.path) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
private fun AppDrawerItem(
    title: String,
    isExpand: Boolean,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AppDrawerHeader(title, actions)
    content()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AppDrawerDevice() {
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
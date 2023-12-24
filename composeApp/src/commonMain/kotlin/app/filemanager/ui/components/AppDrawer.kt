package app.filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.filemanager.data.main.DrawerBookmarkIcon
import app.filemanager.service.WebSocketService
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState

@Composable
fun AppDrawer(mainState: MainState) {
    val drawerState = DrawerState()
    val path by mainState.path.collectAsState()
    val isExpandBookmark by drawerState.isExpandBookmark.collectAsState()
    val isExpandDevice by drawerState.isExpandDevice.collectAsState()
    val isExpandNetwork by drawerState.isExpandNetwork.collectAsState()

    ModalDrawerSheet {
        LazyColumn {
            item {
                AppDrawerBookmark(
                    isExpandBookmark,
                    drawerState,
                    path,
                    mainState::updatePath
                )
            }
            item { Divider() }
            item {
                AppDrawerItem(
                    "设备",
                    false,
                    actions = {
                        Row {
                            Icon(Icons.Default.Add, null, Modifier.clickable { })
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isExpandDevice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                Modifier.clickable { drawerState.updateExpandDevice(!isExpandDevice) }
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
                            Icon(Icons.Default.Add, null, Modifier.clickable { })
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isExpandNetwork) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                Modifier.clickable { drawerState.updateExpandNetwork(!isExpandNetwork) }
                            )
                        }
                    }
                ) {
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        println(WebSocketService().scanService())
    }
}

@Composable
private fun AppDrawerBookmark(
    isExpandBookmark: Boolean,
    drawerState: DrawerState,
    path: String,
    updatePath: (String) -> Unit
) {
    AppDrawerItem(
        "书签",
        isExpand = isExpandBookmark,
        actions = {
            Row {
                Icon(Icons.Default.Add, null, Modifier.clickable { })
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isExpandBookmark) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    Modifier.clickable { drawerState.updateExpandBookmark(!isExpandBookmark) }
                )
            }
        }
    ) {
        if (!isExpandBookmark) {
            return@AppDrawerItem
        }
        for (bookmark in drawerState.bookmarks) {
            NavigationDrawerItem(
                icon = {
                    when (bookmark.iconType) {
                        DrawerBookmarkIcon.Favorite -> Icon(
                            Icons.Default.Favorite,
                            contentDescription = null
                        )

                        DrawerBookmarkIcon.Home -> Icon(Icons.Default.Home, contentDescription = null)
                        DrawerBookmarkIcon.Image -> Icon(Icons.Default.Image, contentDescription = null)
                        DrawerBookmarkIcon.Audio -> Icon(
                            Icons.Default.Headphones,
                            contentDescription = null
                        )

                        DrawerBookmarkIcon.Video -> Icon(
                            Icons.Default.Videocam,
                            contentDescription = null
                        )

                        DrawerBookmarkIcon.Document -> Icon(
                            Icons.Default.Description,
                            contentDescription = null
                        )

                        DrawerBookmarkIcon.Download -> Icon(
                            Icons.Default.Download,
                            contentDescription = null
                        )

                        DrawerBookmarkIcon.Custom -> Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null
                        )
                    }
                },
                label = { Text(bookmark.name) },
                selected = path == bookmark.path,
                onClick = { updatePath(bookmark.path) },
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
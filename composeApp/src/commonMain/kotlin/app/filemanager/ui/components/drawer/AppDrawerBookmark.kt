package app.filemanager.ui.components.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.filemanager.data.file.FileProtocol
import app.filemanager.data.main.*
import app.filemanager.ui.screen.file.FavoriteScreen
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DrawerState
import app.filemanager.ui.state.main.MainState
import app.filemanager.utils.WindowSizeClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppDrawerBookmark() {
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
                if (mainState.windowSize == WindowSizeClass.Compact) {
                    mainState.updateExpandDrawer(false)
                }
                mainState.updateFavorite(true)
                mainState.navigator?.push(FavoriteScreen())
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
                    scope.launch(Dispatchers.Default) {
                        fileState.updateDesk(FileProtocol.Local, Local())
                        fileState.updatePath(bookmark.path)
                    }
                    if (mainState.windowSize == WindowSizeClass.Compact) {
                        mainState.updateExpandDrawer(false)
                    }
                    if (mainState.navigator?.lastItem is FavoriteScreen) {
                        mainState.navigator?.pop()
                    }
                    mainState.updateFavorite(false)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
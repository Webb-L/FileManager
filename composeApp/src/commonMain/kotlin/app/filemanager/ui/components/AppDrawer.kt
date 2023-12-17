package app.filemanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
// icons to mimic drawer destinations
    val items = listOf(Icons.Default.Favorite, Icons.Default.Face, Icons.Default.Email)
    val selectedItem = remember { mutableStateOf(items[0]) }

    ModalDrawerSheet {
        LazyColumn {
            item {
                AppDrawerItem("书签") {
                    AppDrawerBookmark()
                }
            }
            item { Divider() }
            item {
                AppDrawerItem("挂载") {
                }
            }
            item { Divider() }
            item {
                AppDrawerItem("网络") {
                }
            }
        }
    }
}

@Composable
private fun AppDrawerItem(title: String, content: @Composable () -> Unit) {
    AppDrawerHeader(title)
    content()
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AppDrawerMount() {
    AppDrawerHeader("挂载")

}

@Composable
private fun AppDrawerNetwork() {
    AppDrawerHeader("网络")
}

@Composable
private fun AppDrawerBookmark() {
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
        label = { Text("收藏") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Home, contentDescription = null) },
        label = { Text("主目录") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Image, contentDescription = null) },
        label = { Text("图片") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Headphones, contentDescription = null) },
        label = { Text("音乐") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
        label = { Text("视频") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Description, contentDescription = null) },
        label = { Text("文档") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        label = { Text("下载") },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
private fun AppDrawerHeader(title: String) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)) {
        Text(title)
        Spacer(Modifier.weight(1f))
        Row {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ExpandMore, null)
        }
    }
}
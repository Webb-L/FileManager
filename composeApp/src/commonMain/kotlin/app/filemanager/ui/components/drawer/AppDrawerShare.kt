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
import app.filemanager.data.main.Local
import app.filemanager.extensions.DeviceIcon
import app.filemanager.ui.state.file.FileState
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.ui.state.main.DrawerState
import org.koin.compose.koinInject

@Composable
fun AppDrawerShare() {
    val deviceState = koinInject<DeviceState>()
    if (deviceState.shares.isEmpty()) return

    val fileState = koinInject<FileState>()

    val drawerState = koinInject<DrawerState>()
    val isExpandShare by drawerState.isExpandShare.collectAsState()

    AppDrawerItem(
        "分享",
        actions = {
            Icon(
                if (isExpandShare) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                Modifier.clip(RoundedCornerShape(25.dp))
                    .clickable { drawerState.updateExpandShare(!isExpandShare) }
            )
        }
    ) {
        if (!isExpandShare) return@AppDrawerItem
        for ((index, share) in deviceState.shares.withIndex()) {
            NavigationDrawerItem(
                icon = {
                    share.type.DeviceIcon()
                },
                label = { Text(share.name) },
                selected = false,
                badge = {
                    Icon(
                        Icons.Default.Close,
                        "取消连接",
                        Modifier
                            .clip(RoundedCornerShape(25.dp))
                            .clickable {
                                if (share.rpcClientManager.disconnect()) {
                                    deviceState.shares.remove(share)
                                    fileState.updateDesk(FileProtocol.Local, Local())
                                }
                            }
                    )
                },
                onClick = {
                    deviceState.shares.firstOrNull { it.id == share.id }?.let {
                        fileState.updateDesk(FileProtocol.Share, it)
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}